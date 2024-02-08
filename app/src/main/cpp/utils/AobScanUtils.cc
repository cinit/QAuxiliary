//
// Created by sulfate on 2023-07-14.
//

#include "AobScanUtils.h"

#include <fmt/format.h>

#include "ElfScan.h"
#include "TextUtils.h"
#include "ConfigManager.h"
#include "qauxv_core/HostInfo.h"

#include "string_operators.h"

namespace utils {

using Validator = AobScanTarget::Validator;

bool SearchForAllAobScanTargets(std::vector<AobScanTarget*> targets,
                                const void* imageBase, bool isLoadedImage,
                                std::vector<std::string>& errors) {
    bool hasFailed = false;
    const uint64_t currentVersion = qauxv::HostInfo::GetLongVersionCode();
    for (auto* target: targets) {
        std::string_view name = target->name;
        std::span<const uint8_t> sequence = target->sequence;
        std::span<const uint8_t> mask = target->mask;
        int step = target->step;
        bool execMemOnly = target->execMemOnly;
        const auto cacheValueKey = name + ".value";
        const auto cacheVersionKey = name + ".version";
        auto offsetsForResult = target->offsetsForResult;
        auto validator = target->resultValidator;
        if (mask.size() != sequence.size() && !mask.empty()) {
            errors.emplace_back(fmt::format("AobScanUtils: sequence and mask size mismatch for target '{}', sequence: '{}', mask: '{}'",
                                            name, bytes2hex(sequence), bytes2hex(mask)));
            hasFailed = true;
            continue;
        }
        std::optional<uint64_t> lastResult;
        {
            auto& cache = qauxv::ConfigManager::GetCache();
            auto lastValue = cache.GetUInt64(cacheValueKey);
            auto lastVersion = cache.GetUInt64(cacheVersionKey);
            if (lastValue.has_value() && lastVersion.has_value() && lastVersion.value() == currentVersion) {
                lastResult = lastValue.value();
            }
        }
        auto rawResultSet = FindByteSequenceImpl(imageBase, isLoadedImage, sequence, mask, execMemOnly, step, lastResult);
        if (rawResultSet.empty()) {
            errors.emplace_back(fmt::format("AobScanUtils: failed to find target '{}' with sequence '{}' mask '{}'",
                                            name, bytes2hex(sequence), bytes2hex(mask)));
            hasFailed = true;
            continue;
        }
        if (rawResultSet.size() > 1) {
            std::string msg = fmt::format("AobScanUtils: found {} targets '{}' with sequence '{}' mask '{}'",
                                          rawResultSet.size(), name, bytes2hex(sequence), bytes2hex(mask));
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
            errors.emplace_back(fmt::format(
                    "AobScanUtils: validator failed for all targets '{}' with sequence '{}' mask '{}' for result 0x{:x}+offset",
                    name, bytes2hex(sequence), bytes2hex(mask), rawResultSet[0]));
            hasFailed = true;
            continue;
        } else if (results.size() > 1) {
            std::string msg = fmt::format(
                    "AobScanUtils: validator passed too many for {} targets '{}' with sequence '{}' mask '{}' for results 0x{:x}+offset, result: ",
                    results.size(), name, bytes2hex(sequence), bytes2hex(mask), rawResultSet[0]);
            for (auto result: results) {
                msg += fmt::format("0x{:x}, ", result);
            }
            errors.emplace_back(std::move(msg));
            hasFailed = true;
            continue;
        } else {
            target->results.emplace_back(results[0]);
            // save to cache
            auto& cache = qauxv::ConfigManager::GetCache();
            // head up: we use rawResultSet[0] as cache value, not results[0]
            // because the offsetForResult is not applied to the cache value
            // nor does the FindByteSequenceImpl know about it
            cache.PutUInt64(cacheValueKey, rawResultSet[0]);
            cache.PutUInt64(cacheVersionKey, currentVersion);
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
