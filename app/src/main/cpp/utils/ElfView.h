//
// Created by kinit on 2021-10-25.
//
// This file(ElfView.h) is licensed under the Apache License 2.0.

#ifndef NATIVES_ELFVIEW_H
#define NATIVES_ELFVIEW_H

#include <cstddef>
#include <cstdint>
#include <string_view>
#include <vector>
#include <span>
#include <string>
// do not #include <elf.h> here, too many macros

namespace utils {

class ElfView {
public:

    enum class ElfClass {
        kNone = 0,
        kElf32 = 1,
        kElf64 = 2,
    };

    class ElfInfo {
    public:
        ElfClass elfClass = ElfClass::kNone;
        uint16_t machine = 0;
        std::string soname;
        // the p_vaddr of the first PT_LOAD segment in ELF **file**
        uint64_t loadBias = 0;
        size_t loadedSize = 0;
        const void* sysv_hash = nullptr;
        uint32_t sysv_hash_nbucket = 0;
        uint32_t sysv_hash_nchain = 0;
        const uint32_t* sysv_hash_bucket = nullptr;
        const uint32_t* sysv_hash_chain = nullptr;
        const void* gnu_hash = nullptr;
        const void* symtab = nullptr;
        size_t symtab_size = 0;
        const char* strtab = nullptr;
        const void* dynsym = nullptr;
        size_t dynsym_size = 0;
        const char* dynstr = nullptr;
        bool use_rela = false;
        const void* reldyn = nullptr;
        size_t reldyn_size = 0;
        const void* reladyn = nullptr;
        size_t reladyn_size = 0;
        const void* relplt = nullptr;
        size_t relplt_size = 0;
    };

private:
    std::span<const uint8_t> mMemory;
    bool mIsLoaded = false;
    ElfInfo mElfInfo;

public:

    void AttachFileMemMapping(std::span<const uint8_t> fileMap) noexcept;

    inline void AttachFileMemMapping(const void* address, size_t length) noexcept {
        AttachFileMemMapping(std::span<const uint8_t>((const uint8_t*) address, length));
    }

    void AttachLoadedMemoryView(std::span<const uint8_t> memory);

    inline void AttachLoadedMemoryView(const void* address, size_t length) {
        AttachLoadedMemoryView(std::span<const uint8_t>((const uint8_t*) address, length));
    }

    [[nodiscard]] inline bool IsValid() const noexcept {
        return !mMemory.empty() && mElfInfo.elfClass != ElfClass::kNone;
    }

    inline void Detach() noexcept {
        mMemory = {};
        mElfInfo = {};
        mIsLoaded = false;
    }

    [[nodiscard]] inline int GetPointerSize() const noexcept {
        switch (mElfInfo.elfClass) {
            case ElfClass::kElf32:
                return 4;
            case ElfClass::kElf64:
                return 8;
            default:
                return 0;
        }
    }

    [[nodiscard]] inline const ElfInfo& GetElfInfo() const noexcept {
        return mElfInfo;
    }

    [[nodiscard]] inline int GetArchitecture() const noexcept {
        return mElfInfo.machine;
    }

    /**
     * Get the load bias of the elf file. Typically, you don't need to use this value.
     * @return the load bias of the elf file.
     */
    [[nodiscard]] inline uint64_t GetLoadBias() const noexcept {
        return mElfInfo.loadBias;
    }

    [[nodiscard]] inline size_t GetLoadedSize() const noexcept {
        return mElfInfo.loadedSize;
    }

    /**
     * Get the soname of the elf file.
     * @return may be empty string.
     */
    [[nodiscard]] inline const std::string& GetSoname() const noexcept {
        return mElfInfo.soname;
    }

    [[nodiscard]] uint64_t GetSymbolOffset(std::string_view symbol) const;

    [[nodiscard]] uint64_t GetFirstSymbolOffsetWithPrefix(std::string_view symbolPrefix) const;

    [[nodiscard]] std::vector<uint64_t> GetSymbolGotOffset(std::string_view symbol) const;

};

}

#endif //NATIVES_ELFVIEW_H
