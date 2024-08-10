//
// Created by sulfate on 2024-08-10.
//

#ifndef QAUXV_NATIVE_LOADER_H
#define QAUXV_NATIVE_LOADER_H

#include <cstdint>

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

bool SetClassLoaderNativeNamespaceNonBridged(JNIEnv* env, jobject class_loader);

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

}

#endif //QAUXV_NATIVE_LOADER_H
