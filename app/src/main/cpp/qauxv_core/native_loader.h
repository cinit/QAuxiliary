//
// Created by sulfate on 2024-08-10.
//

#ifndef QAUXV_NATIVE_LOADER_H
#define QAUXV_NATIVE_LOADER_H

#include <cstdint>
#include <span>

#include <fmt/format.h>

#include <jni.h>

namespace qauxv::nativeloader {

enum class ElfClass: uint32_t {
    kNone = 0,
    k32 = 1,
    k64 = 2,
};


enum LibraryIsa: uint32_t {
    kNone = 0,
    // EM_386 = 3
    kX86 = (static_cast<uint32_t>(ElfClass::k32) << 16u) | 3u,
    // EM_X86_64 = 62
    kX86_64 = (static_cast<uint32_t>(ElfClass::k64) << 16u) | 62u,
    // EM_ARM = 40
    kArm = (static_cast<uint32_t>(ElfClass::k32) << 16u) | 40u,
    // EM_AARCH64 = 183
    kArm64 = (static_cast<uint32_t>(ElfClass::k64) << 16) | 183u,
    // EM_MIPS = 8
    kMips = (static_cast<uint32_t>(ElfClass::k32) << 16) | 8u,
    kMips64 = (static_cast<uint32_t>(ElfClass::k64) << 16) | 8u,
    // EM_RISCV = 243
    kRiscv32 = (static_cast<uint32_t>(ElfClass::k32) << 16) | 243u,
    kRiscv64 = (static_cast<uint32_t>(ElfClass::k64) << 16) | 243u,
    kUnknown = 0xFFFFFFFFu,
};

bool CheckClassLoaderNativeNamespaceBridged(JNIEnv* env, jobject class_loader, bool is_bridge);

constexpr LibraryIsa GetCurrentLibraryIsa() noexcept {
#if defined(__i386__) || defined(__x86_64__)
    if constexpr (sizeof(void*) == 4) {
        return LibraryIsa::kX86;
    } else {
        return LibraryIsa::kX86_64;
    }
#elif defined(__arm__) || defined(__aarch64__)
    if constexpr (sizeof(void*) == 4) {
        return LibraryIsa::kArm;
    } else {
        return LibraryIsa::kArm64;
    }
#elif defined(__mips__) || defined(__mips64__)
    if constexpr (sizeof(void*) == 4) {
        return LibraryIsa::kMips;
    } else {
        return LibraryIsa::kMips64;
    }
#elif defined(__riscv)
    if constexpr (sizeof(void*) == 4) {
        return LibraryIsa::kRiscv32;
    } else {
        return LibraryIsa::kRiscv64;
    }
#else
#error "Unsupported architecture"
#endif
}

LibraryIsa GetLibraryIsaWithElfHeader(std::span<const uint8_t, 32> elf_header);

}

// fmt::formatter specialization for LibraryIsa
template<>
struct fmt::formatter<qauxv::nativeloader::LibraryIsa> {
    constexpr auto parse(format_parse_context& ctx) {
        return ctx.begin();
    }

    template<typename FormatContext>
    auto format(const qauxv::nativeloader::LibraryIsa& isa, FormatContext& ctx) const {
        switch (isa) {
            case qauxv::nativeloader::LibraryIsa::kX86:
                return format_to(ctx.out(), "x86");
            case qauxv::nativeloader::LibraryIsa::kX86_64:
                return format_to(ctx.out(), "x86_64");
            case qauxv::nativeloader::LibraryIsa::kArm:
                return format_to(ctx.out(), "arm");
            case qauxv::nativeloader::LibraryIsa::kArm64:
                return format_to(ctx.out(), "arm64");
            case qauxv::nativeloader::LibraryIsa::kMips:
                return format_to(ctx.out(), "mips");
            case qauxv::nativeloader::LibraryIsa::kMips64:
                return format_to(ctx.out(), "mips64");
            case qauxv::nativeloader::LibraryIsa::kRiscv32:
                return format_to(ctx.out(), "riscv32");
            case qauxv::nativeloader::LibraryIsa::kRiscv64:
                return format_to(ctx.out(), "riscv64");
            case qauxv::nativeloader::LibraryIsa::kUnknown:
                return format_to(ctx.out(), "unknown");
            case qauxv::nativeloader::LibraryIsa::kNone:
                return format_to(ctx.out(), "none");
            default:
                return format_to(ctx.out(), "LibraryIsa[{}]", static_cast<uint32_t>(isa));
        }
    }
};

extern "C" jobject GetModuleMainClassLoader();

#endif //QAUXV_NATIVE_LOADER_H
