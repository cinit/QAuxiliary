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
#include <vector>
#include <memory>
// do not #include <elf.h> here, too many macros

namespace utils {

class ElfView {
public:

    enum class ElfClass {
        kNone = 0,
        kElf32 = 1,
        kElf64 = 2,
    };

    class ElfInfo;

private:
    bool mIsValid = false;
    bool mIsLoaded = false;
    std::unique_ptr<ElfInfo> mElfInfo;

public:
    ElfView();

    ~ElfView() noexcept;

    // no copy, no assign
    ElfView(const ElfView&) = delete;
    ElfView& operator=(const ElfView&) = delete;

    void ParseFileMemMapping(std::span<const uint8_t> fileMap) noexcept;

    inline void ParseFileMemMapping(const void* address, size_t length) noexcept {
        ParseFileMemMapping(std::span<const uint8_t>((const uint8_t*) address, length));
    }

    void ParseLoadedMemoryView(std::span<const uint8_t> memory);

    inline void ParseLoadedMemoryView(const void* address, size_t length) {
        ParseLoadedMemoryView(std::span<const uint8_t>((const uint8_t*) address, length));
    }

    [[nodiscard]] bool IsValid() const noexcept;

    void Detach() noexcept;

    [[nodiscard]] int GetPointerSize() const noexcept;

    [[nodiscard]] uint8_t GetElfClass() const noexcept;

    /**
     * Get the ELF machine type.
     * @return the machine type, or 0 if elf is invalid.
     */
    [[nodiscard]] uint16_t GetArchitecture() const noexcept;

    /**
     * Get the load bias of the elf file. Typically, you don't need to use this value.
     * @return the load bias of the elf file.
     */
    [[nodiscard]] uint64_t GetLoadBias() const noexcept;

    [[nodiscard]] size_t GetLoadedSize() const noexcept;

    /**
     * Get the soname of the elf file.
     * @return may be empty string.
     */
    [[nodiscard]] const std::string& GetSoname() const noexcept;

    /**
     * Find a symbol in the elf file.
     * It will search the dynamic symbol table first, then the debug symbol table, then the mini debug info.
     * @param symbol the name of the symbol.
     * @return offset relative to the base address of the elf image, or 0 if not found.
     */
    [[nodiscard]] uint64_t GetSymbolOffset(std::string_view symbol) const;

    /**
     * Just like GetSymbolOffset, but return the first symbol offset with the given prefix.
     * @param symbolPrefix the prefix of the symbol.
     * @return the offset of the symbol, or 0 if not found.
     */
    [[nodiscard]] uint64_t GetFirstSymbolOffsetWithPrefix(std::string_view symbolPrefix) const;

    /**
     * Find the offset of a symbol in the GOT table.
     * @param symbol the name of the symbol.
     * @return a vector of offsets, may be empty.
     */
    [[nodiscard]] std::vector<uint64_t> GetSymbolGotOffset(std::string_view symbol) const;

private:
    void ParseMiniDebugInfo(std::span<const uint8_t> input);

    static void ParseDebugSymbol(std::span<const uint8_t> input, ElfInfo* pInfo, bool isMiniDebugInfo);

};

}

#endif //NATIVES_ELFVIEW_H
