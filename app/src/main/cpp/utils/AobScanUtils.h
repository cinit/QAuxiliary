//
// Created by sulfate on 2023-07-14.
//

#ifndef QAUXV_AOBSCANUTILS_H
#define QAUXV_AOBSCANUTILS_H

#include <cstdint>
#include <vector>
#include <array>
#include <string>
#include <string_view>
#include <span>
#include <optional>
#include <functional>

namespace utils {

class AobScanTarget {
public:
    /**
     * A validator for the result of AOB scan. Note that the offsetForResult is applied before the validator.
     * @param base the base address of the ELF image
     * @param isLoaded true if the image is loaded in memory by a linker, false if it's a mmap-ed file
     * @param rva the relative virtual address of the result, this is real result of the AOB scan
     * @param optOffsetInFile the offset in the file, if the image is loaded in memory, this value is 0
     */
    using Validator = std::function<bool(const void* base, bool isLoaded, uint64_t rva, uint64_t optOffsetInFile)>;

    std::string name; // for debugging purpose only
    std::vector<uint8_t> sequence;
    std::vector<uint8_t> mask; // optional, if not provided, the mask is all 0xFF
    int step = 0;
    bool execMemOnly = false;
    std::vector<int64_t> offsetsForResult;
    std::optional<Validator> resultValidator;

    std::vector<uint64_t> results;

    AobScanTarget() = default;

    AobScanTarget(std::string name, std::vector<uint8_t> sequence, std::vector<uint8_t> mask, int step, bool execMemOnly,
                  int64_t offsetsForResult, std::optional<Validator> resultValidator)
            : name(std::move(name)), sequence(std::move(sequence)), mask(std::move(mask)), step(step), execMemOnly(execMemOnly),
              offsetsForResult(std::move(offsetsForResult)), resultValidator(std::move(resultValidator)) {}

    inline AobScanTarget& WithName(std::string newName) {
        this->name = std::move(newName);
        return *this;
    }

    inline AobScanTarget& WithSequence(std::vector<uint8_t> newSequence) {
        this->sequence = std::move(newSequence);
        return *this;
    }

    inline AobScanTarget& WithMask(std::vector<uint8_t> newMask) {
        this->mask = std::move(newMask);
        return *this;
    }

    inline AobScanTarget& WithStep(int newStep) {
        this->step = newStep;
        return *this;
    }

    inline AobScanTarget& WithExecMemOnly(bool newExecMemOnly) {
        this->execMemOnly = newExecMemOnly;
        return *this;
    }

    inline AobScanTarget& WithOffsetsForResult(std::vector<int64_t> newOffsetsForResult) {
        this->offsetsForResult = std::move(newOffsetsForResult);
        return *this;
    }

    inline AobScanTarget& WithResultValidator(std::optional<Validator> newResultValidator) {
        this->resultValidator = std::move(newResultValidator);
        return *this;
    }

    inline AobScanTarget& WithResultValidator(Validator newResultValidator) {
        this->resultValidator = std::move(newResultValidator);
        return *this;
    }

    inline uint64_t GetResultOffset() const noexcept {
        if (results.size() != 1) {
            return 0;
        }
        return results[0];
    }

    inline bool HasResult() const noexcept {
        return results.size() == 1;
    }
};

class CommonAobScanValidator {
public:
    /**
     * Require the result to be arm64 instruction "stp x29, x30, [sp, #imm]" or "stp x29, x30, [sp, #imm]!"
     * This is used to find the function prologue.
     */
    static const AobScanTarget::Validator kArm64StpX29X30SpImm;
};

/**
 * Search for all AOB scan targets in the given image.
 * @param targets an array of AOB scan targets
 * @param imageBase the base address of the ELF image
 * @param isLoadedImage true if the image is loaded in memory by a linker, false if it's a mmap-ed file
 * @param errors a vector of error messages
 * @return true if and only if every AOB scan target has one result
 */
bool SearchForAllAobScanTargets(std::vector<AobScanTarget*> targets, const void* imageBase, bool isLoadedImage, std::vector<std::string>& errors);

}

#endif //QAUXV_AOBSCANUTILS_H
