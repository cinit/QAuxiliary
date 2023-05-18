//
// Created by kinit on 2021-10-25.
//

#ifndef NATIVES_ELFVIEW_H
#define NATIVES_ELFVIEW_H

#include <cstdint>
#include <vector>
// do not #include <elf.h> here, too many macros

#include "utils/MemoryBuffer.h"

namespace utils {

class ElfView {
public:
    using ElfInfo = struct {
        const uint8_t* handle;
        const void* ehdr;
        const void* phdr;
        const void* shdr;
        const void* dyn;
        uint32_t dyn_size;
        const void* symtab;
        uint32_t sym_size;
        const void* relplt;
        uint32_t relplt_size;
        const void* reldyn;
        bool use_rela;
        uint32_t reldyn_size;
        const void* gnu_hash;
        uint32_t nbucket;
        uint32_t nchain;
        const uint32_t* bucket;
        const uint32_t* chain;
        const char* shstr;
        const char* symstr;
    };

    static constexpr int ARCH_X86 = 3;
    static constexpr int ARCH_X86_64 = 62;
    static constexpr int ARCH_ARM = 40;
    static constexpr int ARCH_AARCH64 = 183;
private:
    MemoryBuffer mMemoryBuffer{nullptr, 0};
    int mPointerSize = 0;
    int mArchitecture = 0;

public:
    void attachFileMemMapping(void* address, size_t size);

    [[nodiscard]] bool isValid() const noexcept {
        return mPointerSize != 0;
    }

    inline void detach() {
        mMemoryBuffer = {nullptr, 0};
        mPointerSize = 0;
        mArchitecture = 0;
    }

    [[nodiscard]] inline int getPointerSize() const noexcept {
        return mPointerSize;
    }

    [[nodiscard]] inline int getArchitecture() const noexcept {
        return mArchitecture;
    }

    [[nodiscard]] int getElfInfo(ElfInfo& info) const;

    /**
     * @return -1 if not found
     */
    [[nodiscard]] int getSymbolIndex(const char* symbol) const;

    /**
     * @return 0 if not found
     */
    [[nodiscard]] int getSymbolAddress(const char* symbol) const;

    [[nodiscard]] std::vector<uint64_t> getExtSymGotRelVirtAddr(const char* symbol) const;
};

}

#endif //NATIVES_ELFVIEW_H
