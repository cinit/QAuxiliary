//
// Created by sulfate on 2023-06-24.
//

#include "ElfScan.h"

#include <elf.h>

#include "utils/Log.h"
#include "utils/MemoryUtils.h"

namespace utils {

std::vector<uint64_t> FindByteSequenceImpl(const void* baseAddress, bool isLoaded, std::span<const uint8_t> sequence, bool execMemOnly, int step) {
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
    if (reinterpret_cast<uintptr_t>(baseAddress) % 4096 != 0) {
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
    struct Segments {
        uint64_t offsetInFile;
        uint64_t offsetInMemory;
        uint64_t sizeInFile;
        uint64_t sizeInMemory;
        int flags;
    };
    std::vector<Segments> segments;
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
        uint64_t sizeInFile;
        uint64_t sizeInMemory;
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
        segments.push_back({offsetInFile, offsetInMemory, sizeInFile, sizeInMemory, flags});
    }
    if (segments.empty()) {
        LOGE("no segments found");
        return {};
    }
    std::vector<uint64_t> results;
    // try to find the sequence in each segment
    for (const auto& seg: segments) {
        const void* start;
        if (isLoaded) {
            start = reinterpret_cast<const void*>(base + seg.offsetInMemory);
        } else {
            start = reinterpret_cast<const void*>(base + seg.offsetInFile);
        }
        uint64_t size = isLoaded ? seg.sizeInMemory : seg.sizeInFile;
        if (!IsMemoryReadable(start, size)) {
            LOGW("segment is not readable, start: {}, size: {}", start, size);
            continue;
        }
        const auto fnOnFind = [base, isLoaded, start, &results, &seg](const void* it) {
            if (isLoaded) {
                results.push_back(reinterpret_cast<const uint8_t*>(it) - base);
            } else {
                // calculate the RVA when the file is loaded
                uint64_t offsetInSegment = reinterpret_cast<const uint8_t*>(it) - reinterpret_cast<const uint8_t*>(start);
                uint64_t segmentRVA = seg.offsetInMemory;
                uint64_t rva = segmentRVA + offsetInSegment;
                results.push_back(rva);
            }
        };
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
    }
    return results;
}

std::vector<uint64_t> FindByteSequenceForImageFile(const void* baseAddress, std::span<const uint8_t> sequence, bool execMemOnly, int step) {
    return FindByteSequenceImpl(baseAddress, false, sequence, execMemOnly, step);
}

std::vector<uint64_t> FindByteSequenceForLoadedImage(const void* baseAddress, std::span<const uint8_t> sequence, bool execMemOnly, int step) {
    return FindByteSequenceImpl(baseAddress, true, sequence, execMemOnly, step);
}

}
