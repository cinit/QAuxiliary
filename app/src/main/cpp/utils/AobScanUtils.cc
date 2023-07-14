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
        int64_t offsetForResult = target->offsetForResult;
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
        uint64_t result = uint64_t(int64_t(rawResultSet[0]) + offsetForResult);
        if (validator.has_value()) {
            // TODO: 2023-07-14 if the image is a mmaped file, the offset in file is not provided, currently we just set it to 0
            if (!validator.value()(imageBase, isLoadedImage, result, 0)) {
                errors.emplace_back(fmt::format("AobScanUtils: validator failed for target '{}' with sequence {} for result 0x{:x}",
                                                name, bytes2hex(sequence), result));
                hasFailed = true;
                continue;
            }
        }
        target->results.emplace_back(result);
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
