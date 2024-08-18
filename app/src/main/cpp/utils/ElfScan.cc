//
// Created by sulfate on 2023-06-24.
//

#include "ElfScan.h"

#include <optional>

#include <elf.h>

#include "utils/Log.h"
#include "utils/MemoryUtils.h"
#include "TextUtils.h"

namespace utils {


static std::vector<uint8_t> bitwise_and(std::span<const uint8_t> a, std::span<const uint8_t> b) {
    std::vector<uint8_t> result;
    if (a.size() != b.size()) [[unlikely]] {
        throw std::invalid_argument("a.size() != b.size()");
    }
    auto size = a.size();
    result.reserve(size);
    for (size_t i = 0; i < size; ++i) {
        result.push_back(a[i] & b[i]);
    }
    return result;
}

std::vector<uint64_t> FindByteSequenceImpl(const void* baseAddress, bool isLoaded, std::span<const uint8_t> sequence,
                                           std::span<const uint8_t> mask, bool execMemOnly, int step, std::optional<uint64_t> hint) {
    if (!mask.empty() && mask.size() != sequence.size()) {
        LOGE("mask size is not sequence size, abort, mask: {}, sequence: {}", mask.size(), sequence.size());
        return {};
    }
    std::vector<uint8_t> sequence_masked;
    if (!mask.empty()) {
        // make sure that (sequence & mask) == sequence
        sequence_masked = bitwise_and(sequence, mask);
        sequence = sequence_masked;
    }
    // step should be 0/1/2/4/8
    if (step != 0 && step != 1 && step != 2 && step != 4 && step != 8) {
        LOGE("invalid step, got {}", step);
        return {};
    }
    if (step > 1) {
        if (sequence.size() % step != 0) {
            LOGE("invalid sequence size or step, got {} {}", sequence.size(), step);
            return {};
        }
    }
    // check whether the base address is aligned
    if (reinterpret_cast<uintptr_t>(baseAddress) % GetPageSize() != 0) {
        LOGE("base address is not aligned, got {}", baseAddress);
        return {};
    }
    if (!IsPageReadable(baseAddress)) {
        LOGE("base address is not readable, got {}", baseAddress);
        return {};
    }
    // check magic 0x7fELF
    if (memcmp(baseAddress, ELFMAG, SELFMAG) != 0) {
        LOGE("magic is not 0x7fELF, got {:x}", *reinterpret_cast<const uint32_t*>(baseAddress));
        return {};
    }
    const uint8_t* base = static_cast<const uint8_t*>(baseAddress);
    // get current bit of the ELF image
    uint8_t elfClass = *reinterpret_cast<const uint8_t*>(base + EI_CLASS);
    if (elfClass != ELFCLASS32 && elfClass != ELFCLASS64) {
        LOGE("invalid ELF class, got {}", elfClass);
        return {};
    }
    bool is64Bit = elfClass == ELFCLASS64;
    uint64_t programHeaderOffset;
    if (is64Bit) {
        programHeaderOffset = *reinterpret_cast<const uint64_t*>(base + offsetof(Elf64_Ehdr, e_phoff));
    } else {
        programHeaderOffset = *reinterpret_cast<const uint32_t*>(base + offsetof(Elf32_Ehdr, e_phoff));
    }
    if (programHeaderOffset == 0) {
        LOGE("program header offset is 0");
        return {};
    }
    uint16_t programHeaderEntrySize;
    uint16_t programHeaderEntryCount;
    if (is64Bit) {
        programHeaderEntrySize = *reinterpret_cast<const uint16_t*>(base + offsetof(Elf64_Ehdr, e_phentsize));
        programHeaderEntryCount = *reinterpret_cast<const uint16_t*>(base + offsetof(Elf64_Ehdr, e_phnum));
    } else {
        programHeaderEntrySize = *reinterpret_cast<const uint16_t*>(base + offsetof(Elf32_Ehdr, e_phentsize));
        programHeaderEntryCount = *reinterpret_cast<const uint16_t*>(base + offsetof(Elf32_Ehdr, e_phnum));
    }
    if (is64Bit) {
        if (programHeaderEntrySize != sizeof(Elf64_Phdr)) {
            LOGE("programHeaderEntrySize is not sizeof(Elf64_Phdr), abort, got {:x}", programHeaderEntrySize);
            return {};
        }
    } else {
        if (programHeaderEntrySize != sizeof(Elf32_Phdr)) {
            LOGE("programHeaderEntrySize is not sizeof(Elf32_Phdr), abort, got {:x}", programHeaderEntrySize);
            return {};
        }
    }
    struct Segment {
        uint64_t offsetInFile;
        uint64_t offsetInMemory;
        uint64_t offsetInSource;
        uint64_t sizeInFile;
        uint64_t sizeInMemory;
        uint64_t sizeInSource;
        int flags;
    };
    // source is the file or memory, start from the base address, so that in the following code,
    // we can use the same code to calculate the RVA
    std::vector<Segment> segments;
    if (!IsMemoryReadable(base + programHeaderOffset)) {
        LOGE("program header is not readable, abort, address={}", static_cast<const void*>(base + programHeaderOffset));
        return {};
    }
    for (uint16_t i = 0; i < programHeaderEntryCount; ++i) {
        const uint8_t* programHeaderEntry = base + programHeaderOffset + i * programHeaderEntrySize;
        uint32_t type;
        if (is64Bit) {
            type = *reinterpret_cast<const uint32_t*>(programHeaderEntry + offsetof(Elf64_Phdr, p_type));
        } else {
            type = *reinterpret_cast<const uint32_t*>(programHeaderEntry + offsetof(Elf32_Phdr, p_type));
        }
        if (type != PT_LOAD) {
            continue;
        }
        uint64_t offsetInFile;
        uint64_t offsetInMemory;
        uint64_t offsetInSource;
        uint64_t sizeInFile;
        uint64_t sizeInMemory;
        uint64_t sizeInSource;
        int flags;
        if (is64Bit) {
            offsetInFile = *reinterpret_cast<const uint64_t*>(programHeaderEntry + offsetof(Elf64_Phdr, p_offset));
            offsetInMemory = *reinterpret_cast<const uint64_t*>(programHeaderEntry + offsetof(Elf64_Phdr, p_vaddr));
            sizeInFile = *reinterpret_cast<const uint64_t*>(programHeaderEntry + offsetof(Elf64_Phdr, p_filesz));
            sizeInMemory = *reinterpret_cast<const uint64_t*>(programHeaderEntry + offsetof(Elf64_Phdr, p_memsz));
            flags = *reinterpret_cast<const uint32_t*>(programHeaderEntry + offsetof(Elf64_Phdr, p_flags));
        } else {
            offsetInFile = *reinterpret_cast<const uint32_t*>(programHeaderEntry + offsetof(Elf32_Phdr, p_offset));
            offsetInMemory = *reinterpret_cast<const uint32_t*>(programHeaderEntry + offsetof(Elf32_Phdr, p_vaddr));
            sizeInFile = *reinterpret_cast<const uint32_t*>(programHeaderEntry + offsetof(Elf32_Phdr, p_filesz));
            sizeInMemory = *reinterpret_cast<const uint32_t*>(programHeaderEntry + offsetof(Elf32_Phdr, p_memsz));
            flags = *reinterpret_cast<const uint32_t*>(programHeaderEntry + offsetof(Elf32_Phdr, p_flags));
        }
        // ignore .bss
        if (sizeInFile == 0 || sizeInMemory == 0) {
            continue;
        }
        if (execMemOnly && ((flags & PF_X) == 0)) {
            continue;
        }
        if (isLoaded) {
            offsetInSource = offsetInMemory;
            sizeInSource = sizeInMemory;
        } else {
            offsetInSource = offsetInFile;
            sizeInSource = sizeInFile;
        }
        segments.push_back({offsetInFile, offsetInMemory, offsetInSource, sizeInFile, sizeInMemory, sizeInSource, flags});
    }
    if (segments.empty()) {
        LOGE("no segments found");
        return {};
    }
    std::vector<uint64_t> results;
    const auto ptrInSourceToResultRVA = [&](const void* ptr) -> std::optional<uint64_t> {
        if (isLoaded) {
            return reinterpret_cast<const uint8_t*>(ptr) - base;
        } else {
            // calculate the RVA when the file is loaded
            uint64_t offsetInSource = reinterpret_cast<const uint8_t*>(ptr) - base;
            // find the segment
            for (const auto& seg: segments) {
                if (offsetInSource >= seg.offsetInSource && offsetInSource < seg.offsetInSource + seg.sizeInSource) {
                    uint64_t offsetInSegment = offsetInSource - seg.offsetInSource;
                    uint64_t segmentRVA = seg.offsetInMemory;
                    uint64_t rva = segmentRVA + offsetInSegment;
                    return rva;
                }
            }
            // not found
            return std::nullopt;
        }
    };
    const auto findSegmentFromResultRVA = [&](uint64_t rva) {
        struct Result {
            Segment* seg;
            uint64_t offsetInSegment;
        };
        for (auto& seg: segments) {
            if (rva >= seg.offsetInMemory && rva < seg.offsetInMemory + seg.sizeInMemory) {
                uint64_t offsetInSegment = rva - seg.offsetInMemory;
                return std::make_optional(Result{&seg, offsetInSegment});
            }
        }
        return std::optional<Result>{};
    };
    if (hint.has_value()) {
        // try hint first
        auto hintResult = findSegmentFromResultRVA(hint.value());
        if (hintResult.has_value()) {
            const Segment* seg = hintResult.value().seg;
            uint64_t offsetInSegment = hintResult.value().offsetInSegment;
            const uint8_t* start;
            start = base + seg->offsetInSource + offsetInSegment;
            // check whether the segment is readable
            if (IsMemoryReadable(start, sequence.size())) {
                bool isMatch;
                if (mask.empty()) {
                    isMatch = memcmp(start, sequence.data(), sequence.size()) == 0;
                } else {
                    isMatch = true;
                    for (size_t i = 0; i < sequence.size(); ++i) {
                        if ((start[i] & mask[i]) != sequence[i]) {
                            isMatch = false;
                            break;
                        }
                    }
                }
                if (isMatch) {
                    results.push_back(ptrInSourceToResultRVA(start).value());
                    // we don't need to search other segments, because the hint is used as cache
                    return results;
                }
            }
        }
    }
    // try to find the sequence in each segment
    for (const auto& seg: segments) {
        const void* start;
        start = reinterpret_cast<const void*>(base + seg.offsetInSource);
        uint64_t size = seg.sizeInSource;
        if (!IsMemoryReadable(start, size)) {
            LOGW("segment is not readable, start: {}, size: {}", start, size);
            continue;
        }
        const auto fnOnFind = [base, isLoaded, start, &results, &seg](const void* ptrInSource) {
            if (isLoaded) {
                results.push_back(reinterpret_cast<const uint8_t*>(ptrInSource) - base);
            } else {
                // calculate the RVA when the file is loaded
                uint64_t offsetInSegment = reinterpret_cast<const uint8_t*>(ptrInSource) - reinterpret_cast<const uint8_t*>(start);
                uint64_t segmentRVA = seg.offsetInMemory;
                uint64_t rva = segmentRVA + offsetInSegment;
                results.push_back(rva);
            }
        };
        // brute force is just fine, since we will cache the result
        if (mask.empty()) {
            if (step == 8) {
                std::span<const uint64_t> pattern = {reinterpret_cast<const uint64_t*>(sequence.data()), sequence.size() / 8};
                const uint64_t* begin = reinterpret_cast<const uint64_t*>(start);
                const uint64_t* end = reinterpret_cast<const uint64_t*>(start) + size / 8;
                uint64_t first = *pattern.data();
                for (const uint64_t* it = begin; it < end; ++it) {
                    if (*it == first) [[unlikely]] {
                        if (memcmp(it, pattern.data(), pattern.size_bytes()) == 0) {
                            fnOnFind(it);
                        }
                    }
                }
            } else if (step == 4) {
                std::span<const uint32_t> pattern = {reinterpret_cast<const uint32_t*>(sequence.data()), sequence.size() / 4};
                const uint32_t* begin = reinterpret_cast<const uint32_t*>(start);
                const uint32_t* end = reinterpret_cast<const uint32_t*>(start) + size / 4;
                uint32_t first = *pattern.data();
                for (const uint32_t* it = begin; it < end; ++it) {
                    if (*it == first) [[unlikely]] {
                        if (memcmp(it, pattern.data(), pattern.size_bytes()) == 0) {
                            fnOnFind(it);
                        }
                    }
                }
            } else if (step == 2) {
                std::span<const uint16_t> pattern = {reinterpret_cast<const uint16_t*>(sequence.data()), sequence.size() / 2};
                const uint16_t* begin = reinterpret_cast<const uint16_t*>(start);
                const uint16_t* end = reinterpret_cast<const uint16_t*>(start) + size / 2;
                uint16_t first = *pattern.data();
                for (const uint16_t* it = begin; it < end; ++it) {
                    if (*it == first) [[unlikely]] {
                        if (memcmp(it, pattern.data(), pattern.size_bytes()) == 0) {
                            fnOnFind(it);
                        }
                    }
                }
            } else {
                std::span<const uint8_t> pattern = sequence;
                const uint8_t* begin = reinterpret_cast<const uint8_t*>(start);
                const uint8_t* end = reinterpret_cast<const uint8_t*>(start) + size;
                for (const uint8_t* it = begin; it < end; ++it) {
                    if (memcmp(it, pattern.data(), pattern.size_bytes()) == 0) [[unlikely]] {
                        fnOnFind(it);
                    }
                }
            }
        } else {
            if (step == 8) {
                std::span<const uint64_t> pattern = {reinterpret_cast<const uint64_t*>(sequence.data()), sequence.size() / 8};
                std::span<const uint64_t> maskSpan = {reinterpret_cast<const uint64_t*>(mask.data()), mask.size() / 8};
                const uint64_t* begin = reinterpret_cast<const uint64_t*>(start);
                const uint64_t* end = reinterpret_cast<const uint64_t*>(start) + size / 8;
                uint64_t first = *pattern.data();
                for (const uint64_t* it = begin; it < end; ++it) {
                    if ((*it & maskSpan[0]) == first) [[unlikely]] {
                        bool isMatch = true;
                        // now start to compare the second element
                        for (size_t i = 1; i < pattern.size(); ++i) {
                            if (((it[i] & maskSpan[i]) != pattern[i])) {
                                isMatch = false;
                                break;
                            }
                        }
                        if (isMatch) {
                            fnOnFind(it);
                        }
                    }
                }
            } else if (step == 4) {
                std::span<const uint32_t> pattern = {reinterpret_cast<const uint32_t*>(sequence.data()), sequence.size() / 4};
                std::span<const uint32_t> maskSpan = {reinterpret_cast<const uint32_t*>(mask.data()), mask.size() / 4};
                const uint32_t* begin = reinterpret_cast<const uint32_t*>(start);
                const uint32_t* end = reinterpret_cast<const uint32_t*>(start) + size / 4;
                uint32_t first = *pattern.data();
                for (const uint32_t* it = begin; it < end; ++it) {
                    if ((*it & maskSpan[0]) == first) [[unlikely]] {
                        bool isMatch = true;
                        // now start to compare the second element
                        for (size_t i = 1; i < pattern.size(); ++i) {
                            if (((it[i] & maskSpan[i]) != pattern[i])) {
                                isMatch = false;
                                break;
                            }
                        }
                        if (isMatch) {
                            fnOnFind(it);
                        }
                    }
                }
            } else if (step == 2) {
                std::span<const uint16_t> pattern = {reinterpret_cast<const uint16_t*>(sequence.data()), sequence.size() / 2};
                std::span<const uint16_t> maskSpan = {reinterpret_cast<const uint16_t*>(mask.data()), mask.size() / 2};
                const uint16_t* begin = reinterpret_cast<const uint16_t*>(start);
                const uint16_t* end = reinterpret_cast<const uint16_t*>(start) + size / 2;
                uint16_t first = *pattern.data();
                for (const uint16_t* it = begin; it < end; ++it) {
                    if ((*it & maskSpan[0]) == first) [[unlikely]] {
                        bool isMatch = true;
                        // now start to compare the second element
                        for (size_t i = 1; i < pattern.size(); ++i) {
                            if (((it[i] & maskSpan[i]) != pattern[i])) {
                                isMatch = false;
                                break;
                            }
                        }
                        if (isMatch) {
                            fnOnFind(it);
                        }
                    }
                }
            } else {
                std::span<const uint8_t> pattern = sequence;
                std::span<const uint8_t> maskSpan = mask;
                const uint8_t* begin = reinterpret_cast<const uint8_t*>(start);
                const uint8_t* end = reinterpret_cast<const uint8_t*>(start) + size;
                for (const uint8_t* it = begin; it < end; ++it) {
                    bool isMatch = true;
                    for (size_t i = 0; i < pattern.size(); ++i) {
                        if (((it[i] & maskSpan[i]) != pattern[i])) {
                            isMatch = false;
                            break;
                        }
                    }
                    if (isMatch) {
                        fnOnFind(it);
                    }
                }
            }
        }
    }
    return results;
}

std::vector<uint64_t> FindByteSequenceForImageFile(const void* baseAddress, std::span<const uint8_t> sequence,
                                                   std::span<const uint8_t> mask, bool execMemOnly, int step,
                                                   std::optional<uint64_t> hint) {
    return FindByteSequenceImpl(baseAddress, false, sequence, mask, execMemOnly, step, hint);
}

std::vector<uint64_t> FindByteSequenceForLoadedImage(const void* baseAddress, std::span<const uint8_t> sequence,
                                                     std::span<const uint8_t> mask, bool execMemOnly, int step,
                                                     std::optional<uint64_t> hint) {
    return FindByteSequenceImpl(baseAddress, true, sequence, mask, execMemOnly, step, hint);
}

}
