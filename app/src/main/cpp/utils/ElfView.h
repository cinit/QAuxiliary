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
    std::span<const uint8_t> mMemory;
    bool mIsLoaded = false;
    std::unique_ptr<ElfInfo> mElfInfo;

public:
    ElfView();

    ~ElfView() noexcept;

    // no copy, no assign
    ElfView(const ElfView&) = delete;
    ElfView& operator=(const ElfView&) = delete;

    void AttachFileMemMapping(std::span<const uint8_t> fileMap) noexcept;

    inline void AttachFileMemMapping(const void* address, size_t length) noexcept {
        AttachFileMemMapping(std::span<const uint8_t>((const uint8_t*) address, length));
    }

    void AttachLoadedMemoryView(std::span<const uint8_t> memory);

    inline void AttachLoadedMemoryView(const void* address, size_t length) {
        AttachLoadedMemoryView(std::span<const uint8_t>((const uint8_t*) address, length));
    }

    [[nodiscard]] bool IsValid() const noexcept;

    void Detach() noexcept;

    [[nodiscard]] int GetPointerSize() const noexcept;

    [[nodiscard]] const ElfInfo& GetElfInfo() const noexcept;

    [[nodiscard]] int GetArchitecture() const noexcept;

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

    [[nodiscard]] uint64_t GetSymbolOffset(std::string_view symbol) const;

    [[nodiscard]] uint64_t GetFirstSymbolOffsetWithPrefix(std::string_view symbolPrefix) const;

    [[nodiscard]] std::vector<uint64_t> GetSymbolGotOffset(std::string_view symbol) const;

private:
    void ParseMiniDebugInfo(std::span<const uint8_t> input);

    void ParseDebugSymbol(std::span<const uint8_t> input);

};

}

#endif //NATIVES_ELFVIEW_H
