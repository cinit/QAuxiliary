//
// Created by kinit on 2021-10-25.
//
// reference: https://flapenguin.me/elf-dt-gnu-hash

#include <elf.h>
#include <cstring>

#include "ElfView.h"
#include "MemoryBuffer.h"

using namespace utils;

void ElfView::attachFileMemMapping(void* address, size_t size) {
    mMemoryBuffer = MemoryBuffer{address, size};
    if (mMemoryBuffer.address != nullptr && mMemoryBuffer.length > 64 && *mMemoryBuffer.at<char>(0) == 0x7F
            && *mMemoryBuffer.at<char>(1) == 'E' && *mMemoryBuffer.at<char>(1) == 'L' && *mMemoryBuffer.at<char>(1) == 'F') {
        mPointerSize = 0;
        mArchitecture = 0;
    } else {
        char type = *mMemoryBuffer.at<char>(4);
        if (type == 1) {
            mPointerSize = 4;
            mArchitecture = static_cast<const Elf32_Ehdr*>(mMemoryBuffer.address)->e_machine;
        } else if (type == 2) {
            mPointerSize = 8;
            mArchitecture = static_cast<const Elf64_Ehdr*>(mMemoryBuffer.address)->e_machine;
        } else {
            mPointerSize = 0;
            mArchitecture = 0;
        }
    }
}

// SysV hash
static inline uint32_t elf_sysv_hash(const char* name) {
    uint32_t h = 0, g;
    while (*name) {
        h = (h << 4) + *name++;
        g = h & 0xf0000000;
        h ^= g;
        h ^= g >> 24;
    }
    return h;
}

// GNU hash
static inline uint32_t elf_gnu_hash(const char* name) {
    uint32_t h = 5381;
    while (*name != 0) {
        h += (h << 5) + *name++;
    }
    return h;
}


[[nodiscard]]
static bool findLocalSymByGnuHash64(const ElfView::ElfInfo& info, const char* symbol,
                                    const Elf64_Sym** ppsym, uint32_t* psymidx) {
    using bloom_el_t = uint64_t;
    constexpr int ELFCLASS_BITS = 64;
    const uint32_t* hashtab = static_cast<const uint32_t*>(info.gnu_hash);
    const uint32_t symbol_hash = elf_gnu_hash(symbol);
    const uint32_t nbuckets = hashtab[0];
    const uint32_t symoffset = hashtab[1];
    const uint32_t bloom_size = hashtab[2];
    const uint32_t bloom_shift = hashtab[3];
    const bloom_el_t* bloom = reinterpret_cast<const bloom_el_t*>(&hashtab[4]);
    const uint32_t* buckets = reinterpret_cast<const uint32_t*>(&bloom[bloom_size]);
    const uint32_t* chain = &buckets[nbuckets];
    bloom_el_t word = bloom[(symbol_hash / ELFCLASS_BITS) % bloom_size];
    bloom_el_t mask = (bloom_el_t) 1 << (symbol_hash % ELFCLASS_BITS)
            | (bloom_el_t) 1 << ((symbol_hash >> bloom_shift) % ELFCLASS_BITS);
    /* If at least one bit is not set, a symbol is surely missing. */
    if ((word & mask) != mask) {
        return false;
    }
    uint32_t symix = buckets[symbol_hash % nbuckets];
    if (symix < symoffset) {
        return false;
    }
    const auto* symtab = static_cast<const Elf64_Sym*>(info.symtab);
    /* Loop through the chain. */
    while (true) {
        const char* symname = info.symstr + symtab[symix].st_name;
        const uint32_t hash = chain[symix - symoffset];
        if ((symbol_hash | 1) == (hash | 1) && strcmp(symbol, symname) == 0) {
            const Elf64_Sym& sym = symtab[symix];
            // found.
            *psymidx = symix;
            *ppsym = &sym;
            return true;
        }
        /* Chain ends with an element with the lowest bit set to 1. */
        if (hash & 1) {
            break;
        }
        symix++;
    }
    // not found
    return false;
}

[[nodiscard]]
static bool findLocalSymByGnuHash32(const ElfView::ElfInfo& info, const char* symbol,
                                    const Elf32_Sym** ppsym, uint32_t* psymidx) {
    using bloom_el_t = uint32_t;
    constexpr int ELFCLASS_BITS = 32;
    const uint32_t* hashtab = static_cast<const uint32_t*>(info.gnu_hash);
    const uint32_t symbol_hash = elf_gnu_hash(symbol);
    const uint32_t nbuckets = hashtab[0];
    const uint32_t symoffset = hashtab[1];
    const uint32_t bloom_size = hashtab[2];
    const uint32_t bloom_shift = hashtab[3];
    const bloom_el_t* bloom = reinterpret_cast<const bloom_el_t*>(&hashtab[4]);
    const uint32_t* buckets = reinterpret_cast<const uint32_t*>(&bloom[bloom_size]);
    const uint32_t* chain = &buckets[nbuckets];
    bloom_el_t word = bloom[(symbol_hash / ELFCLASS_BITS) % bloom_size];
    bloom_el_t mask = (bloom_el_t) 1 << (symbol_hash % ELFCLASS_BITS)
            | (bloom_el_t) 1 << ((symbol_hash >> bloom_shift) % ELFCLASS_BITS);
    /* If at least one bit is not set, a symbol is surely missing. */
    if ((word & mask) != mask) {
        return false;
    }
    uint32_t symix = buckets[symbol_hash % nbuckets];
    if (symix < symoffset) {
        return false;
    }
    const auto* symtab = static_cast<const Elf32_Sym*>(info.symtab);
    /* Loop through the chain. */
    while (true) {
        const char* symname = info.symstr + symtab[symix].st_name;
        const uint32_t hash = chain[symix - symoffset];
        if ((symbol_hash | 1) == (hash | 1) && strcmp(symbol, symname) == 0) {
            const Elf32_Sym& sym = symtab[symix];
            // found.
            *psymidx = symix;
            *ppsym = &sym;
            return true;
        }
        /* Chain ends with an element with the lowest bit set to 1. */
        if (hash & 1) {
            break;
        }
        symix++;
    }
    // not found
    return false;
}

[[nodiscard]]
static bool findSymByName32(const ElfView::ElfInfo& info, const char* symbol,
                            const Elf32_Sym** ppsym, uint32_t* psymidx) {
    if (info.gnu_hash != nullptr) {
        if (findLocalSymByGnuHash32(info, symbol, ppsym, psymidx)) {
            return true;
        }
        // search the symtab
        const uint32_t* gnuhashtab = static_cast<const uint32_t*>(info.gnu_hash);
        const uint32_t symoffset = gnuhashtab[1];
        const Elf32_Sym* symtab = static_cast<const Elf32_Sym*>(info.symtab);
        for (uint32_t i = 0; i < symoffset; i++) {
            const char* symname = info.symstr + symtab[i].st_name;
            if (strcmp(symname, symbol) == 0) {
                *psymidx = i;
                *ppsym = &symtab[i];
                return true;
            }
        }
        return false;
    } else if (info.chain != nullptr) {
        const Elf32_Sym* target = nullptr;
        uint32_t hash = elf_sysv_hash(symbol);
        uint32_t index = info.bucket[hash % info.nbucket];
        const auto* sym = static_cast<const Elf32_Sym*>(info.symtab);
        if (!strcmp(info.symstr + sym[index].st_name, symbol)) {
            target = sym + index;
        }
        if (!target) {
            do {
                index = info.chain[index];
                if (!strcmp(info.symstr + sym[index].st_name, symbol)) {
                    target = sym + index;
                    break;
                }
            } while (index != 0);
        }
        if (target) {
            *ppsym = target;
            *psymidx = int(index);
            return true;
        }
    }
    return false;
}

[[nodiscard]]
static bool findSymByName64(const ElfView::ElfInfo& info, const char* symbol,
                            const Elf64_Sym** ppsym, uint32_t* psymidx) {
    if (info.gnu_hash != nullptr) {
        if (findLocalSymByGnuHash64(info, symbol, ppsym, psymidx)) {
            return true;
        }
        // search the symtab
        const uint32_t* gnuhashtab = static_cast<const uint32_t*>(info.gnu_hash);
        const uint32_t symoffset = gnuhashtab[1];
        const Elf64_Sym* symtab = static_cast<const Elf64_Sym*>(info.symtab);
        for (uint32_t i = 0; i < symoffset; i++) {
            const char* symname = info.symstr + symtab[i].st_name;
            if (strcmp(symname, symbol) == 0) {
                *psymidx = i;
                *ppsym = &symtab[i];
                return true;
            }
        }
        return false;
    } else if (info.chain != nullptr) {
        const Elf64_Sym* target = nullptr;
        uint32_t hash = elf_sysv_hash(symbol);
        uint32_t index = info.bucket[hash % info.nbucket];
        const auto* sym = static_cast<const Elf64_Sym*>(info.symtab);
        if (!strcmp(info.symstr + sym[index].st_name, symbol)) {
            target = sym + index;
        }
        if (!target) {
            do {
                index = info.chain[index];
                if (!strcmp(info.symstr + sym[index].st_name, symbol)) {
                    target = sym + index;
                    break;
                }
            } while (index != 0);
        }
        if (target) {
            *ppsym = target;
            *psymidx = int(index);
            return true;
        }
    }
    return false;
}

std::vector<uint64_t> ElfView::getExtSymGotRelVirtAddr(const char* symbol) const {
    if (symbol == nullptr) {
        return {};
    }
    if (mPointerSize == 4) {
        // ELF32
        std::vector<uint64_t> result;
        ElfInfo info = {};
        if (getElfInfo(info) != mPointerSize) {
            return result;
        }
        // find symbol
        const Elf32_Sym* sym = nullptr;
        uint32_t symidx = 0;
        if (findSymByName32(info, symbol, &sym, &symidx)) {
            if (info.use_rela) {
                for (int i = 0; i < info.relplt_size; i++) {
                    const Elf32_Rela& rel = ((const Elf32_Rela*) info.relplt)[i];
                    if (ELF32_R_SYM(rel.r_info) == symidx
                            && (ELF32_R_TYPE(rel.r_info) == R_ARM_JUMP_SLOT
                                    || ELF32_R_TYPE(rel.r_info) == R_386_JMP_SLOT)) {
                        result.emplace_back(uint64_t(rel.r_offset));
                        break;
                    }
                }
                for (int i = 0; i < info.reldyn_size; i++) {
                    const Elf32_Rela& rel = ((const Elf32_Rela*) info.reldyn)[i];
                    if (ELF32_R_SYM(rel.r_info) == symidx &&
                            (ELF32_R_TYPE(rel.r_info) == R_ARM_ABS32
                                    || ELF32_R_TYPE(rel.r_info) == R_ARM_GLOB_DAT
                                    || ELF32_R_TYPE(rel.r_info) == R_386_32
                                    || ELF32_R_TYPE(rel.r_info) == R_386_GLOB_DAT)) {
                        result.emplace_back(uint64_t(rel.r_offset));
                    }
                }
            } else {
                for (int i = 0; i < info.relplt_size; i++) {
                    const Elf32_Rela& rel = ((const Elf32_Rela*) info.relplt)[i];
                    if (ELF32_R_SYM(rel.r_info) == symidx
                            && (ELF32_R_TYPE(rel.r_info) == R_ARM_JUMP_SLOT
                                    || ELF32_R_TYPE(rel.r_info) == R_386_JMP_SLOT)) {
                        result.emplace_back(uint64_t(rel.r_offset));
                        break;
                    }
                }
                for (int i = 0; i < info.reldyn_size; i++) {
                    const Elf32_Rela& rel = ((const Elf32_Rela*) info.reldyn)[i];
                    if (ELF32_R_SYM(rel.r_info) == symidx &&
                            (ELF32_R_TYPE(rel.r_info) == R_ARM_ABS32
                                    || ELF32_R_TYPE(rel.r_info) == R_ARM_GLOB_DAT
                                    || ELF32_R_TYPE(rel.r_info) == R_386_32
                                    || ELF32_R_TYPE(rel.r_info) == R_386_GLOB_DAT)) {
                        result.emplace_back(uint64_t(rel.r_offset));
                    }
                }
            }
        }
        return result;
    } else if (mPointerSize == 8) {
        // ELF64
        std::vector<uint64_t> result;
        ElfInfo info = {};
        if (getElfInfo(info) != mPointerSize) {
            return result;
        }
        // find symbol
        const Elf64_Sym* sym = nullptr;
        uint32_t symidx = 0;
        if (findSymByName64(info, symbol, &sym, &symidx)) {
            if (info.use_rela) {
                for (int i = 0; i < info.relplt_size; i++) {
                    const Elf64_Rela& rel = ((const Elf64_Rela*) info.relplt)[i];
                    if (ELF64_R_SYM(rel.r_info) == symidx
                            && (ELF64_R_TYPE(rel.r_info) == R_AARCH64_JUMP_SLOT
                                    || ELF64_R_TYPE(rel.r_info) == R_X86_64_JUMP_SLOT)) {
                        result.emplace_back(uint64_t(rel.r_offset));
                        break;
                    }
                }
                for (int i = 0; i < info.reldyn_size; i++) {
                    const Elf64_Rela& rel = ((const Elf64_Rela*) info.reldyn)[i];
                    if (ELF64_R_SYM(rel.r_info) == symidx &&
                            (ELF64_R_TYPE(rel.r_info) == R_AARCH64_ABS64
                                    || ELF64_R_TYPE(rel.r_info) == R_AARCH64_GLOB_DAT
                                    || ELF64_R_TYPE(rel.r_info) == R_X86_64_64
                                    || ELF64_R_TYPE(rel.r_info) == R_X86_64_GLOB_DAT)) {
                        result.emplace_back(uint64_t(rel.r_offset));
                    }
                }
            } else {
                for (int i = 0; i < info.relplt_size; i++) {
                    const Elf64_Rel& rel = ((const Elf64_Rel*) info.relplt)[i];
                    if (ELF64_R_SYM(rel.r_info) == symidx
                            && (ELF64_R_TYPE(rel.r_info) == R_AARCH64_JUMP_SLOT
                                    || ELF64_R_TYPE(rel.r_info) == R_X86_64_JUMP_SLOT)) {
                        result.emplace_back(uint64_t(rel.r_offset));
                        break;
                    }
                }
                for (int i = 0; i < info.reldyn_size; i++) {
                    const Elf64_Rel& rel = ((const Elf64_Rel*) info.reldyn)[i];
                    if (ELF64_R_SYM(rel.r_info) == symidx &&
                            (ELF64_R_TYPE(rel.r_info) == R_AARCH64_ABS64
                                    || ELF64_R_TYPE(rel.r_info) == R_AARCH64_GLOB_DAT
                                    || ELF64_R_TYPE(rel.r_info) == R_X86_64_64
                                    || ELF64_R_TYPE(rel.r_info) == R_X86_64_GLOB_DAT)) {
                        result.emplace_back(uint64_t(rel.r_offset));
                    }
                }
            }
        }
        return result;
    } else {
        return {};
    }
}

[[nodiscard]]
static inline const Elf32_Phdr* findSegmentByType32(const ElfView::ElfInfo& info, Elf32_Word type) {
    const Elf32_Phdr* target = nullptr;
    const auto* phdr = (const Elf32_Phdr*) info.phdr;
    for (int i = 0; i < ((const Elf32_Ehdr*) info.ehdr)->e_phnum; i++) {
        if (phdr[i].p_type == type) {
            target = phdr + i;
            break;
        }
    }
    return target;
}

[[nodiscard]]
static inline const Elf64_Phdr* findSegmentByType64(const ElfView::ElfInfo& info, Elf64_Word type) {
    const Elf64_Phdr* target = nullptr;
    const auto* phdr = (const Elf64_Phdr*) info.phdr;
    for (int i = 0; i < ((const Elf64_Ehdr*) info.ehdr)->e_phnum; i++) {
        if (phdr[i].p_type == type) {
            target = phdr + i;
            break;
        }
    }
    return target;
}

template<class T>
[[nodiscard]]
static bool getSegmentInfoFromFileMemMap32(const ElfView::ElfInfo& info, Elf32_Word type,
                                           const Elf32_Phdr** ppPhdr, Elf32_Word* pSize, T* data) {
    auto* _phdr = findSegmentByType32(info, type);
    if (_phdr) {
        *ppPhdr = _phdr;
        *data = reinterpret_cast<T>((info.handle) + _phdr->p_offset);
        *pSize = _phdr->p_filesz;
        return true;
    } else {
        *ppPhdr = nullptr;
        *pSize = 0;
        return false;
    }
}

template<class T>
[[nodiscard]]
static bool getSegmentInfoFromFileMemMap64(const ElfView::ElfInfo& info, Elf64_Word type,
                                           const Elf64_Phdr** ppPhdr, Elf64_Xword* pSize, T* data) {
    auto* _phdr = findSegmentByType64(info, type);
    if (_phdr) {
        *ppPhdr = _phdr;
        *data = reinterpret_cast<T>((info.handle) + _phdr->p_offset);
        *pSize = _phdr->p_filesz;
        return true;
    } else {
        *ppPhdr = nullptr;
        *pSize = 0;
        return false;
    }
}

int ElfView::getElfInfo(ElfInfo& info) const {
    info.handle = static_cast<const uint8_t*>(mMemoryBuffer.address);
    info.ehdr = info.handle;
    if (mPointerSize == 4) {
        info.shdr = (const void*) (info.handle + ((const Elf32_Ehdr*) info.ehdr)->e_shoff);
        info.phdr = (const void*) (info.handle + ((const Elf32_Ehdr*) info.ehdr)->e_phoff);
        info.shstr = nullptr;
        Elf32_Phdr* dynamic = nullptr;
        Elf32_Word size = 0;
        if (!getSegmentInfoFromFileMemMap32(info, PT_DYNAMIC,
                                            const_cast<const Elf32_Phdr**>(&dynamic), &size, &info.dyn)) {
            return 0;
        }
        if (!dynamic) {
            return 0;
        }
        info.dyn_size = size / sizeof(Elf32_Dyn);
        const auto* dyn = static_cast<const Elf32_Dyn*>(info.dyn);
        for (int i = 0; i < info.dyn_size; i++, dyn++) {
            switch (dyn->d_tag) {
                case DT_SYMTAB:
                    info.symtab = reinterpret_cast<const Elf32_Sym*>(info.handle + dyn->d_un.d_ptr);
                    break;
                case DT_STRTAB:
                    info.symstr = reinterpret_cast<const char*>(info.handle + dyn->d_un.d_ptr);
                    break;
                case DT_PLTREL:
                    info.use_rela = dyn->d_un.d_val == DT_RELA;
                    break;
                case DT_REL:
                case DT_RELA:
                    info.reldyn = reinterpret_cast<const Elf32_Rel*>(info.handle + dyn->d_un.d_ptr);
                    break;
                case DT_RELSZ:
                case DT_RELASZ:
                    info.reldyn_size = uint32_t(dyn->d_un.d_val / sizeof(Elf32_Rel));
                    break;
                case DT_JMPREL:
                    info.relplt = reinterpret_cast<const Elf32_Rel*>(info.handle + dyn->d_un.d_ptr);
                    break;
                case DT_PLTRELSZ:
                    info.relplt_size = dyn->d_un.d_val / sizeof(Elf32_Rel);
                    break;
                case DT_HASH: {
                    const auto* rawdata = reinterpret_cast<const uint32_t*>(info.handle + dyn->d_un.d_ptr);
                    info.nbucket = rawdata[0];
                    info.nchain = rawdata[1];
                    info.bucket = rawdata + 2;
                    info.chain = info.bucket + info.nbucket;
                    info.sym_size = info.nchain;
                    break;
                }
                case DT_GNU_HASH: {
                    info.gnu_hash = reinterpret_cast<const void*>(info.handle + dyn->d_un.d_ptr);
                    break;
                }
                default:
                    break;
            }
        }
        return 4;
    } else if (mPointerSize == 8) {
        info.shdr = (const void*) (info.handle + ((const Elf64_Ehdr*) info.ehdr)->e_shoff);
        info.phdr = (const void*) (info.handle + ((const Elf64_Ehdr*) info.ehdr)->e_phoff);
        info.shstr = nullptr;
        Elf64_Phdr* dynamic = nullptr;
        Elf64_Xword size = 0;
        if (!getSegmentInfoFromFileMemMap64(info, PT_DYNAMIC,
                                            const_cast<const Elf64_Phdr**>(&dynamic), &size, &info.dyn)) {
            return 0;
        }
        if (!dynamic) {
            return 0;
        }
        info.dyn_size = (uint32_t) (size / sizeof(Elf64_Dyn));
        const auto* dyn = static_cast<const Elf64_Dyn*>(info.dyn);
        for (int i = 0; i < info.dyn_size; i++, dyn++) {
            switch (dyn->d_tag) {
                case DT_SYMTAB:
                    info.symtab = reinterpret_cast<const Elf64_Sym*>(info.handle + dyn->d_un.d_ptr);
                    break;
                case DT_STRTAB:
                    info.symstr = reinterpret_cast<const char*>(info.handle + dyn->d_un.d_ptr);
                    break;
                case DT_PLTREL:
                    info.use_rela = dyn->d_un.d_val == DT_RELA;
                    break;
                case DT_REL:
                case DT_RELA:
                    info.reldyn = reinterpret_cast<const Elf64_Rel*>(info.handle + dyn->d_un.d_ptr);
                    break;
                case DT_RELSZ:
                case DT_RELASZ:
                    info.reldyn_size = uint32_t(dyn->d_un.d_val / sizeof(Elf64_Rel));
                    break;
                case DT_JMPREL:
                    info.relplt = reinterpret_cast<const Elf64_Rel*>(info.handle + dyn->d_un.d_ptr);
                    break;
                case DT_PLTRELSZ:
                    info.relplt_size = uint32_t(dyn->d_un.d_val / sizeof(Elf64_Rel));
                    break;
                case DT_HASH: {
                    const auto* rawdata = reinterpret_cast<const uint32_t*>(info.handle + dyn->d_un.d_ptr);
                    info.nbucket = rawdata[0];
                    info.nchain = rawdata[1];
                    info.bucket = rawdata + 2;
                    info.chain = info.bucket + info.nbucket;
                    info.sym_size = info.nchain;
                    break;
                }
                case DT_GNU_HASH: {
                    info.gnu_hash = reinterpret_cast<const void*>(info.handle + dyn->d_un.d_ptr);
                    break;
                }
                default:
                    break;
            }
        }
        return 8;
    } else {
        return 0;
    }
}

int ElfView::getSymbolIndex(const char* symbol) const {
    if (symbol == nullptr) {
        return {};
    }
    ElfInfo info = {};
    if (getElfInfo(info) != mPointerSize) {
        return -1;
    }
    if (mPointerSize == 4) {
        // ELF32
        const Elf32_Sym* sym = nullptr;
        uint32_t symidx = 0;
        if (findSymByName32(info, symbol, &sym, &symidx)) {
            return int(symidx);
        } else {
            return -1;
        }
    } else if (mPointerSize == 8) {
        // ELF64
        const Elf64_Sym* sym = nullptr;
        uint32_t symidx = 0;
        if (findSymByName64(info, symbol, &sym, &symidx)) {
            return int(symidx);
        } else {
            return -1;
        }
    } else {
        return -1;
    }
}

int ElfView::getSymbolAddress(const char* symbol) const {
    ElfInfo elfInfo = {};
    if (getElfInfo(elfInfo) < 0) {
        return 0;
    }
    int index = getSymbolIndex(symbol);
    if (index < 0) {
        return 0;
    }
    if (getPointerSize() == 8) {
        const auto* sym = static_cast<const Elf64_Sym*>(elfInfo.symtab);
        return int(sym[index].st_value);
    } else if (getPointerSize() == 4) {
        const auto* sym = static_cast<const Elf32_Sym*>(elfInfo.symtab);
        return int(sym[index].st_value);
    } else {
        return 0;
    }
}

