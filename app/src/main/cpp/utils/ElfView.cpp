//
// Created by kinit on 2021-10-25.
//
// This file(ElfView.cpp) is licensed under the Apache License 2.0.
// reference: https://flapenguin.me/elf-dt-gnu-hash

#include <cstring>
#include <type_traits>
#include <optional>
#include <tuple>
#include <limits>
#include <unordered_map>
#include <unordered_set>

#include <elf.h>

#include <fmt/format.h>

#include "utils/Log.h"

#include "ElfView.h"
#include "debug_utils.h"

#ifndef SHT_GNU_HASH
#define SHT_GNU_HASH      0x6ffffff6
#endif

// for mmkv::KeyHasher, mmkv::KeyEqualer
#include "MMKV.h"

#include "xz_decoder.h"

static auto constexpr LOG_TAG = "ElfView";

using namespace utils;

static inline auto constexpr kElf32 = ElfView::ElfClass::kElf32;
static inline auto constexpr kElf64 = ElfView::ElfClass::kElf64;

class ElfView::ElfInfo {
public:
    // prevent accidental copy
    ElfInfo() = default;
    ElfInfo(const ElfInfo&) = delete;
    ElfInfo& operator=(const ElfInfo&) = delete;

    ElfClass elfClass = ElfClass::kNone;
    uint16_t machine = 0;
    std::string soname;
    // the p_vaddr of the first PT_LOAD segment in ELF **file**
    uint64_t loadBias = 0;
    size_t loadedSize = 0;
    const void* symtab = nullptr;
    size_t symtab_size = 0;
    const char* strtab = nullptr;
    const void* dynsym = nullptr;
    size_t dynsym_size = 0;
    const char* dynstr = nullptr;
    bool use_rela = true;
    const void* reldyn = nullptr;
    size_t reldyn_size = 0;
    const void* reladyn = nullptr;
    size_t reladyn_size = 0;
    const void* relplt = nullptr;
    size_t relplt_size_bytes = 0;
    // dynamic symbol table
    std::unordered_map<std::string, uint64_t, mmkv::KeyHasher, mmkv::KeyEqualer> dynamicSymbols;
    // debug symbol table
    std::unordered_map<std::string, uint64_t, mmkv::KeyHasher, mmkv::KeyEqualer> debugSymbols;
    // import address table, for GOT/PLT/IAT hooking
    std::unordered_map<std::string, std::vector<uint64_t>, mmkv::KeyHasher, mmkv::KeyEqualer> pltOffsets;
    std::span<const uint8_t> miniDebugInfo;
    // symbol from compressed ".gnu_debugdata", aka MiniDebugInfo
    std::unordered_map<std::string, uint64_t, mmkv::KeyHasher, mmkv::KeyEqualer> compressedDebugSymbols;
};

ElfView::ElfView() {
    mElfInfo = std::make_unique<ElfInfo>();
}

ElfView::~ElfView() noexcept = default;

using ElfInfo = ElfView::ElfInfo;
using ElfClass = ElfView::ElfClass;

bool ElfView::IsValid() const noexcept {
    return mIsValid && mElfInfo && mElfInfo->elfClass != ElfClass::kNone;
}

void ElfView::Detach() noexcept {
    mElfInfo = nullptr;
    mIsLoaded = false;
}

int ElfView::GetPointerSize() const noexcept {
    if (!IsValid()) {
        return 0;
    }
    switch (mElfInfo->elfClass) {
        case ElfClass::kElf32:
            return 4;
        case ElfClass::kElf64:
            return 8;
        default:
            return 0;
    }
}

uint8_t ElfView::GetElfClass() const noexcept {
    if (!IsValid()) {
        return 0;
    }
    return static_cast<uint8_t>(mElfInfo->elfClass);
}

uint16_t ElfView::GetArchitecture() const noexcept {
    if (!IsValid()) {
        return 0;
    }
    return mElfInfo->machine;
}

/**
 * Get the load bias of the elf file. Typically, you don't need to use this value.
 * @return the load bias of the elf file.
 */
uint64_t ElfView::GetLoadBias() const noexcept {
    return mElfInfo->loadBias;
}

size_t ElfView::GetLoadedSize() const noexcept {
    return mElfInfo->loadedSize;
}

/**
 * Get the soname of the elf file.
 * @return may be empty string.
 */
const std::string& ElfView::GetSoname() const noexcept {
    return mElfInfo->soname;
}


template<ElfClass kElfClass>
static void InitElfInfo(std::span<const uint8_t> file, ElfInfo& info, bool isLoaded, bool isEmbedded) {
    using Elf_Ehdr = std::conditional_t<kElfClass == kElf32, Elf32_Ehdr, Elf64_Ehdr>;
    using Elf_Phdr = std::conditional_t<kElfClass == kElf32, Elf32_Phdr, Elf64_Phdr>;
    using Elf_Dyn = std::conditional_t<kElfClass == kElf32, Elf32_Dyn, Elf64_Dyn>;
    using Elf_Shdr = std::conditional_t<kElfClass == kElf32, Elf32_Shdr, Elf64_Shdr>;
    using Elf_Sym = std::conditional_t<kElfClass == kElf32, Elf32_Sym, Elf64_Sym>;
    using Elf_Rel = std::conditional_t<kElfClass == kElf32, Elf32_Rel, Elf64_Rel>;
    using Elf_Rela = std::conditional_t<kElfClass == kElf32, Elf32_Rela, Elf64_Rela>;
    info.machine = reinterpret_cast<const Elf_Ehdr*>(file.data())->e_machine;
    // walk through program header
    auto phoff = reinterpret_cast<const Elf_Ehdr*>(file.data())->e_phoff;
    if (phoff != 0) {
        auto phnum = reinterpret_cast<const Elf_Ehdr*>(file.data())->e_phnum;
        auto phentsize = reinterpret_cast<const Elf_Ehdr*>(file.data())->e_phentsize;
        uint64_t firstLoadedSegmentStart = std::numeric_limits<uint64_t>::max();
        uint64_t lastLoadedSegmentEnd = 0;
        const Elf_Phdr* phdrSelf = nullptr;
        const Elf_Phdr* phdrDynamic = nullptr;
        for (int i = 0; i < phnum; i++) {
            const auto* phdr = reinterpret_cast<const Elf_Phdr*>(file.data() + phoff + i * phentsize);
            if (phdr->p_type == PT_PHDR) {
                phdrSelf = phdr;
            } else if (phdr->p_type == PT_DYNAMIC) {
                phdrDynamic = phdr;
            } else if (phdr->p_type == PT_LOAD) {
                if (phdr->p_vaddr < firstLoadedSegmentStart) {
                    firstLoadedSegmentStart = phdr->p_vaddr;
                }
                if (phdr->p_vaddr + phdr->p_memsz > lastLoadedSegmentEnd) {
                    lastLoadedSegmentEnd = phdr->p_vaddr + phdr->p_memsz;
                }
            }
        }
        info.loadBias = firstLoadedSegmentStart;
        info.loadedSize = lastLoadedSegmentEnd - firstLoadedSegmentStart;
        if (phdrDynamic != nullptr) {
            // walk through dynamic section
            uint64_t sonameOffset = 0;
            const char* strtab = nullptr;
            for (int i = 0; i < phdrDynamic->p_memsz / sizeof(Elf_Dyn); i++) {
                auto offset = (isLoaded ? phdrDynamic->p_vaddr : phdrDynamic->p_offset) + i * sizeof(Elf_Dyn);
                const auto* dyn = reinterpret_cast<const Elf_Dyn*>(file.data() + offset);
                switch (dyn->d_tag) {
                    case DT_NULL: {
                        break;
                    }
                    case DT_SONAME: {
                        sonameOffset = dyn->d_un.d_val;
                        break;
                    }
                    case DT_STRTAB: {
                        strtab = reinterpret_cast<const char*>(file.data() + dyn->d_un.d_val - info.loadBias);
                        break;
                    }
                    case DT_PLTREL: {
                        info.use_rela = dyn->d_un.d_val == DT_RELA;
                        break;
                    }
                    case DT_REL: {
                        info.reldyn = reinterpret_cast<const Elf_Rel*>(file.data() + dyn->d_un.d_ptr - info.loadBias);
                        break;
                    }
                    case DT_RELA: {
                        info.reladyn = reinterpret_cast<const Elf_Rela*>(file.data() + dyn->d_un.d_ptr - info.loadBias);
                        break;
                    }
                    case DT_RELSZ: {
                        info.reldyn_size = dyn->d_un.d_val / sizeof(Elf_Rel);
                        break;
                    }
                    case DT_RELASZ: {
                        info.reladyn_size = dyn->d_un.d_val / sizeof(Elf_Rela);
                        break;
                    }
                    case DT_JMPREL: {
                        info.relplt = reinterpret_cast<const void*>(file.data() + dyn->d_un.d_ptr - info.loadBias);
                        break;
                    }
                    case DT_PLTRELSZ: {
                        info.relplt_size_bytes = dyn->d_un.d_val;
                        break;
                    }
                    default: {
                        // ignore
                        break;
                    }
                }
            }
            if (sonameOffset != 0 && strtab != nullptr) {
                info.soname = strtab + sonameOffset;
            }
        }
    }
    // walk through section header
    auto shoff = reinterpret_cast<const Elf_Ehdr*>(file.data())->e_shoff;
    if (shoff == 0) {
        LOGW("No section header found");
        return;
    }
    const auto* ehdr = reinterpret_cast<const Elf_Ehdr*>(file.data());
    auto shnum = reinterpret_cast<const Elf_Ehdr*>(file.data())->e_shnum;
    auto shentsize = reinterpret_cast<const Elf_Ehdr*>(file.data())->e_shentsize;
    const char* sectionHeaderStringTable = [ehdr, file, isLoaded, shoff, shentsize]() {
        auto* shstrtab = reinterpret_cast<const Elf_Shdr*>(file.data() + shoff + ehdr->e_shstrndx * shentsize);
        return reinterpret_cast<const char*>(file.data() + (isLoaded ? shstrtab->sh_addr : shstrtab->sh_offset));
    }();
    for (int i = 0; i < shnum; i++) {
        const auto* shdr = reinterpret_cast<const Elf_Shdr*>(file.data() + shoff + i * shentsize);
        const char* name = sectionHeaderStringTable + shdr->sh_name;
        switch (shdr->sh_type) {
            case SHT_STRTAB: {
                if (strcmp(name, ".dynstr") == 0) {
                    info.dynstr = reinterpret_cast<const char*>(file.data() + (isLoaded ? shdr->sh_addr : shdr->sh_offset));
                } else if (strcmp(name, ".strtab") == 0) {
                    info.strtab = reinterpret_cast<const char*>(file.data() + (isLoaded ? shdr->sh_addr : shdr->sh_offset));
                }
                break;
            }
            case SHT_SYMTAB: {
                if (strcmp(name, ".symtab") == 0) {
                    info.symtab = file.data() + shdr->sh_offset;
                    info.symtab_size = shdr->sh_size / sizeof(Elf_Sym);
                }
                break;
            }
            case SHT_DYNSYM: {
                info.dynsym = file.data() + (isLoaded ? shdr->sh_addr : shdr->sh_offset);
                info.dynsym_size = shdr->sh_size / sizeof(Elf_Sym);
                break;
            }
            case SHT_HASH:
            case SHT_GNU_HASH: {
                // we no longer use this
                break;
            }
            case SHT_PROGBITS: {
                if (strcmp(name, ".gnu_debugdata") == 0) {
                    // mini debug info
                    if (!isLoaded) {
                        // obviously, debug data is not loaded, so only file(not memory) is supported
                        std::span<const uint8_t> debugData(file.data() + shdr->sh_offset, shdr->sh_size);
                        info.miniDebugInfo = debugData;
                    }
                }
                break;
            }
            default: {
                // ignore
                break;
            }
        }
    }
    // LOGD("{}: dynsym size: {}, symtab size: {}, bias=0x{:x}", info.soname, info.dynsym_size, info.symtab_size, info.loadBias);
}


void ElfView::ParseMiniDebugInfo(std::span<const uint8_t> input) {
    // check xz magic
    if (input.size() < 6 || input[0] != 0xFD || input[1] != '7' || input[2] != 'z' || input[3] != 'X' || input[4] != 'Z') {
        LOGW("Invalid mini debug info magic before decompression, expected 0xFD 7zXZ, got: {:02X} {:02X} {:02X} {:02X} {:02X}",
             input[0], input[1], input[2], input[3], input[4]);
        return;
    }
    bool isSuccess = false;
    std::string errorMsg;
    const auto decompressed = util::DecodeXzData(input, &isSuccess, &errorMsg);
    if (decompressed.empty() || !isSuccess) {
        LOGW("Failed to decompress mini debug info: {}", errorMsg);
        return;
    }
    ElfInfo embedded;
    auto type = static_cast<ElfClass>(decompressed[4]);
    embedded.elfClass = type;
    if (type == kElf32) {
        InitElfInfo<kElf32>(decompressed, embedded, false, true);
    } else if (type == kElf64) {
        InitElfInfo<kElf64>(decompressed, embedded, false, true);
    } else {
        LOGW("Invalid elf class: {}", static_cast<uint32_t>(type));
        return;
    }
    embedded.loadBias = mElfInfo->loadBias;
    if (embedded.soname.empty()) {
        embedded.soname = mElfInfo->soname;
    }
    ParseDebugSymbol(decompressed, &embedded, true);
    // merge the symbol table
    mElfInfo->compressedDebugSymbols.merge(embedded.compressedDebugSymbols);
}

void ElfView::ParseDebugSymbol(std::span<const uint8_t> input, ElfInfo* pInfo, bool isMiniDebugInfo) {
    // check elf magic
    if (input.size() < 64 || (memcmp(input.data(), ELFMAG, SELFMAG) != 0)) {
        return;
    }
    // walk through the elf file, and get the symbol table
    auto& info = *pInfo;
    auto& map = isMiniDebugInfo ? info.compressedDebugSymbols : info.debugSymbols;
    // LOGD("input size: {}, symtab size: {}, compressed: {}", input.size(), info.symtab_size, isMiniDebugInfo);
    // walk through the symbol table, and add it to the symbol map
    // finally release the memory, because debug symbol is typically large (and it is not file-backed)
    if (info.elfClass == kElf32) {
        const auto* symtab = static_cast<const Elf32_Sym*>(info.symtab);
        for (uint32_t i = 0; i < info.symtab_size; i++) {
            const char* symname = info.strtab + symtab[i].st_name;
            if (symname[0] == '\0') {
                continue;
            }
            map.emplace(symname, symtab[i].st_value - info.loadBias);
        }
    } else if (info.elfClass == kElf64) {
        const auto* symtab = static_cast<const Elf64_Sym*>(info.symtab);
        for (uint32_t i = 0; i < info.symtab_size; i++) {
            const char* symname = info.strtab + symtab[i].st_name;
            if (symname[0] == '\0') {
                continue;
            }
            map.emplace(symname, symtab[i].st_value - info.loadBias);
        }
    }
}

template<ElfClass kElfClass>
static void WalkThroughDynamicSymbolTable(
        const ElfInfo& info,
        std::unordered_map<std::string, uint64_t, mmkv::KeyHasher, mmkv::KeyEqualer>& outDefinedSymbols,
        std::unordered_map<std::string, std::vector<uint64_t>, mmkv::KeyHasher, mmkv::KeyEqualer>& outPltOffsets
) {
    using Elf_Sym = std::conditional_t<kElfClass == kElf32, Elf32_Sym, Elf64_Sym>;
    const auto* symtab = static_cast<const Elf_Sym*>(info.dynsym);
    uint64_t loadBias = info.loadBias;
    // walk through the dynamic symbol table to get defined symbols
    for (uint32_t i = 0; i < info.dynsym_size; i++) {
        const char* symname = info.dynstr + symtab[i].st_name;
        uint64_t value = symtab[i].st_value;
        if (value != 0) {
            outDefinedSymbols.emplace(symname, value - loadBias);
        }
    }
    // walk through the relocation table to get the PLT offsets
    // We don't need PLT hook for now.
    /*
    constexpr auto kRelPltTypes = []() {
        if constexpr (kElfClass == kElf32) {
            return std::make_tuple(
                    static_cast<uint32_t>(R_ARM_JUMP_SLOT),
                    static_cast<uint32_t>(R_386_JMP_SLOT)
            );
        } else if constexpr (kElfClass == kElf64) {
            return std::make_tuple(
                    static_cast<uint32_t>(R_AARCH64_JUMP_SLOT),
                    static_cast<uint32_t>(R_X86_64_JUMP_SLOT)
            );
        }
    }();
    constexpr auto kRelDynTypes = []() {
        if constexpr (kElfClass == kElf32) {
            // R_ARM_ABS32, R_ARM_GLOB_DAT, R_386_32, R_386_GLOB_DAT
            return std::make_tuple(
                    static_cast<uint32_t>(R_ARM_ABS32),
                    static_cast<uint32_t>(R_ARM_GLOB_DAT),
                    static_cast<uint32_t>(R_386_32),
                    static_cast<uint32_t>(R_386_GLOB_DAT)
            );
        } else if constexpr (kElfClass == kElf64) {
            // R_AARCH64_ABS64, R_AARCH64_GLOB_DAT, R_X86_64_64, R_X86_64_GLOB_DAT
            return std::make_tuple(
                    static_cast<uint32_t>(R_AARCH64_ABS64),
                    static_cast<uint32_t>(R_AARCH64_GLOB_DAT),
                    static_cast<uint32_t>(R_X86_64_64),
                    static_cast<uint32_t>(R_X86_64_GLOB_DAT)
            );
        }
    }();
    constexpr auto ELFX_R_SYM = [](uint64_t i) constexpr {
        if constexpr (kElfClass == kElf32) {
            return uint32_t(i >> 8u);
        } else if constexpr (kElfClass == kElf64) {
            return uint32_t(i >> 32u);
        }
    };
    constexpr auto ELFX_R_TYPE = [](uint64_t i) constexpr {
        if constexpr (kElfClass == kElf32) {
            return uint32_t(i & 0xffu);
        } else if constexpr (kElfClass == kElf64) {
            return uint32_t(i & 0xffffffffu);
        }
    };
    auto funcWalkThroughRelocationTable = [
            &info, &outPltOffsets, &kRelPltTypes, &kRelDynTypes, symtab, loadBias, ELFX_R_SYM, ELFX_R_TYPE
    ]<bool kUseRela>() {
        using ElfX_RelX = std::conditional_t<kElfClass == kElf32,
                                             std::conditional_t<kUseRela, Elf32_Rela, Elf32_Rel>,
                                             std::conditional_t<kUseRela, Elf64_Rela, Elf64_Rel>>;
        // walk through the relocation table to get the GOT offsets
        {
            const auto* relplt = static_cast<const ElfX_RelX*>(info.relplt);
            const auto relplt_size = info.relplt_size_bytes / sizeof(ElfX_RelX);
            for (uint32_t i = 0; i < relplt_size; i++) {
                const auto& rel = relplt[i];
                const auto symidx = ELFX_R_SYM(rel.r_info);
                const auto type = ELFX_R_TYPE(rel.r_info);
                if (std::apply([&type](auto... args) { return ((type == args) || ...); }, kRelPltTypes)) {
                    const char* symname = info.dynstr + symtab[symidx].st_name;
                    outPltOffsets[symname].emplace_back(rel.r_offset - loadBias);
                }
            }
        }
        {
            const ElfX_RelX* relxdyn;
            size_t relxdyn_size;
            if constexpr (kUseRela) {
                relxdyn = static_cast<const ElfX_RelX*>(info.reladyn);
                relxdyn_size = info.reladyn_size;
            } else {
                relxdyn = static_cast<const ElfX_RelX*>(info.reldyn);
                relxdyn_size = info.reldyn_size;
            }
            for (uint32_t i = 0; i < relxdyn_size; i++) {
                const auto& rel = relxdyn[i];
                const auto symidx = ELFX_R_SYM(rel.r_info);
                const auto type = ELFX_R_TYPE(rel.r_info);
                if (std::apply([&type](auto... args) { return ((type == args) || ...); }, kRelDynTypes)) {
                    const char* symname = info.dynstr + symtab[symidx].st_name;
                    outPltOffsets[symname].emplace_back(rel.r_offset - loadBias);
                }
            }
        }
    };
    if (info.use_rela) {
        funcWalkThroughRelocationTable.template operator()<true>();
    } else {
        funcWalkThroughRelocationTable.template operator()<false>();
    }
     */
}

void ElfView::ParseFileMemMapping(std::span<const uint8_t> fileMap) noexcept {
    mIsValid = false;
    mIsLoaded = false;
    mElfInfo = std::make_unique<ElfInfo>();
    if (fileMap.data() == nullptr || fileMap.size() < 64 || (memcmp(fileMap.data(), ELFMAG, SELFMAG) != 0)) {
        // invalid elf, ignore
    } else {
        auto& elfInfo = *mElfInfo;
        auto type = static_cast<ElfClass>(fileMap[4]);
        elfInfo.elfClass = type;
        if (type == kElf32) {
            InitElfInfo<kElf32>(fileMap, elfInfo, mIsLoaded, false);
        } else if (type == kElf64) {
            InitElfInfo<kElf64>(fileMap, elfInfo, mIsLoaded, false);
        }
        // walk through dynamic symbol table
        if (elfInfo.dynsym != nullptr && elfInfo.dynstr != nullptr) {
            if (elfInfo.elfClass == kElf32) {
                WalkThroughDynamicSymbolTable<kElf32>(elfInfo, elfInfo.dynamicSymbols, elfInfo.pltOffsets);
            } else if (elfInfo.elfClass == kElf64) {
                WalkThroughDynamicSymbolTable<kElf64>(elfInfo, elfInfo.dynamicSymbols, elfInfo.pltOffsets);
            }
        }
        // if we have debug symbol table, parse it
        if (elfInfo.symtab != nullptr && elfInfo.strtab != nullptr) {
            ParseDebugSymbol(fileMap, &elfInfo, false);
        }
        // if we found mini debug info, parse it
        if (!elfInfo.miniDebugInfo.empty()) {
            ParseMiniDebugInfo(elfInfo.miniDebugInfo);
        }
        // LOGD("{}: dynamic symbols: {}, debug symbols: {}, compressed debug symbols: {}",
        //      elfInfo.soname, elfInfo.dynamicSymbols.size(), elfInfo.debugSymbols.size(), elfInfo.compressedDebugSymbols.size());
        mIsValid = true;
    }
}

void ElfView::ParseLoadedMemoryView(std::span<const uint8_t> memory) {
    mIsValid = false;
    mIsLoaded = true;
    mElfInfo = std::make_unique<ElfInfo>();
    if (memory.data() == nullptr || memory.size() < 64 || (memcmp(memory.data(), ELFMAG, SELFMAG) != 0)) {
        // invalid elf, ignore
    } else {
        auto& elfInfo = *mElfInfo;
        auto type = static_cast<ElfClass>(memory[4]);
        elfInfo.elfClass = type;
        if (type == kElf32) {
            InitElfInfo<kElf32>(memory, elfInfo, mIsLoaded, false);
        } else if (type == kElf64) {
            InitElfInfo<kElf64>(memory, elfInfo, mIsLoaded, false);
        }
        // walk through dynamic symbol table
        if (elfInfo.dynsym != nullptr && elfInfo.dynstr != nullptr) {
            if (elfInfo.elfClass == kElf32) {
                WalkThroughDynamicSymbolTable<kElf32>(elfInfo, elfInfo.dynamicSymbols, elfInfo.pltOffsets);
            } else if (elfInfo.elfClass == kElf64) {
                WalkThroughDynamicSymbolTable<kElf64>(elfInfo, elfInfo.dynamicSymbols, elfInfo.pltOffsets);
            }
        }
        mIsValid = true;
    }
}

uint64_t ElfView::GetSymbolOffset(std::string_view symbol) const {
    if (symbol.empty()) {
        return 0;
    }
    // search dynamic symbol table first
    if (auto it = mElfInfo->dynamicSymbols.find(symbol); it != mElfInfo->dynamicSymbols.end()) {
        return it->second;
    }
    // search debug symbol table
    if (auto it = mElfInfo->debugSymbols.find(symbol); it != mElfInfo->debugSymbols.end()) {
        return it->second;
    }
    // search mini debug info
    if (auto it = mElfInfo->compressedDebugSymbols.find(symbol); it != mElfInfo->compressedDebugSymbols.end()) {
        return it->second;
    }
    // not found
    return 0;
}


[[nodiscard]] uint64_t ElfView::GetFirstSymbolOffsetWithPrefix(std::string_view symbolPrefix) const {
    if (symbolPrefix.empty() || !IsValid()) {
        return 0;
    }
    // search dynamic symbol table first
    for (const auto& [sym, offset]: mElfInfo->dynamicSymbols) {
        if (sym.starts_with(symbolPrefix)) {
            return offset;
        }
    }
    // search debug symbol table
    for (const auto& [sym, offset]: mElfInfo->debugSymbols) {
        if (sym.starts_with(symbolPrefix)) {
            return offset;
        }
    }
    // search mini debug info
    for (const auto& [sym, offset]: mElfInfo->compressedDebugSymbols) {
        if (sym.starts_with(symbolPrefix)) {
            return offset;
        }
    }
    // not found
    return 0;
}

std::vector<uint64_t> ElfView::GetSymbolGotOffset(std::string_view symbol) const {
    if (symbol.empty()) {
        return {};
    }
    if (!IsValid()) {
        return {};
    }
    if (auto it = mElfInfo->pltOffsets.find(symbol); it != mElfInfo->pltOffsets.end()) {
        return it->second;
    }
    // not found
    return {};
}
