//
// Created by sulfate on 2023-07-14.
//

#include "AobScanUtils.h"

#include <fmt/format.h>

#include "ElfScan.h"

namespace utils {

using Validator = AobScanTarget::Validator;

static std::string bytes2hex(std::span<const uint8_t> bytes) {
    std::string result;
    result.reserve(bytes.size() * 2);
    for (uint8_t byte: bytes) {
        result += fmt::format("{:02x}", byte);
    }
    return result;
}

bool SearchForAllAobScanTargets(std::vector<AobScanTarget*> targets,
                                const void* imageBase, bool isLoadedImage,
                                std::vector<std::string>& errors) {
    bool hasFailed = false;
    for (auto* target: targets) {
        std::string_view name = target->name;
        std::span<const uint8_t> sequence = target->sequence;
        int step = target->step;
        bool execMemOnly = target->execMemOnly;
        auto offsetsForResult = target->offsetsForResult;
        auto validator = target->resultValidator;
        auto rawResultSet = FindByteSequenceImpl(imageBase, isLoadedImage, sequence, execMemOnly, step);
        if (rawResultSet.empty()) {
            errors.emplace_back(fmt::format("AobScanUtils: failed to find target '{}' with sequence {}", name, bytes2hex(sequence)));
            hasFailed = true;
            continue;
        }
        if (rawResultSet.size() > 1) {
            std::string msg = fmt::format("AobScanUtils: found {} targets '{}' with sequence {}", rawResultSet.size(), name, bytes2hex(sequence));
            for (auto result: rawResultSet) {
                msg += fmt::format("offset: 0x{:x}, ", result);
            }
            errors.emplace_back(std::move(msg));
            hasFailed = true;
            continue;
        }
        std::vector<uint64_t> resultCandidates;
        for (auto offsetForResult: offsetsForResult) {
            resultCandidates.emplace_back(uint64_t(int64_t(rawResultSet[0]) + offsetForResult));
        }
        std::vector<uint64_t> results;
        // run validator for each result candidate
        if (validator.has_value()) {
            for (auto resultCandidate: resultCandidates) {
                // TODO: 2023-07-14 if the image is a mmaped file, the offset in file is not provided, currently we just set it to 0
                if (!validator.value()(imageBase, isLoadedImage, resultCandidate, 0)) {
                    continue;
                }
                results.emplace_back(resultCandidate);
            }
        } else {
            results = std::move(resultCandidates);
        }
        if (results.empty()) {
            errors.emplace_back(fmt::format("AobScanUtils: validator failed for all targets '{}' with sequence {} for result 0x{:x}+offset",
                                            name, bytes2hex(sequence), rawResultSet[0]));
            hasFailed = true;
            continue;
        } else if (results.size() > 1) {
            std::string msg = fmt::format("AobScanUtils: validator passed too many for {} targets '{}' with sequence {} for results 0x{:x}+offset, result: ",
                                          results.size(), name, bytes2hex(sequence), rawResultSet[0]);
            for (auto result: results) {
                msg += fmt::format("0x{:x}, ", result);
            }
            errors.emplace_back(std::move(msg));
            hasFailed = true;
            continue;
        } else {
            target->results.emplace_back(results[0]);
        }
    }
    return !hasFailed;
}

const Validator CommonAobScanValidator::kArm64StpX29X30SpImm = [](const void* base, bool isLoaded,
                                                                  uint64_t rva, uint64_t optOffsetInFile) -> bool {
    if (!isLoaded) {
        // TODO: 2023-07-14 if the image is a mmaped file, the offset in file is not provided, currently we just set it to 0
        // currently we only support loaded image
        return false;
    }
    if (base == nullptr) {
        return false;
    }
    const uint8_t* va = reinterpret_cast<const uint8_t*>(base) + rva;
    const uint32_t* p = reinterpret_cast<const uint32_t*>(va);
    uint32_t inst = *p;
    // expect  fd 7b ba a9     stp        x29,x30,[sp, #???]!
    if ((inst & ((0b11111111u << 24) | (0b11000000u << 16u) | (0b01111111u << 8u) | 0xFF))
            == ((0b10101001u << 24u) | (0b10000000u << 16u) | (0x7b << 8u) | 0xfd)) {
        return true;
    } else {
        return false;
    }
};

}
