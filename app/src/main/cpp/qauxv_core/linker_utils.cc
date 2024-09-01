//
// Created by sulfate on 2024-07-11.
//

#include "linker_utils.h"

#include <cstdint>
#include <atomic>
#include <optional>
#include <mutex>

#include <dlfcn.h>
#include <android/dlext.h>

#include "utils/Log.h"
#include "qauxv_core/HostInfo.h"
#include "utils/ProcessView.h"
#include "utils/ElfScan.h"
#include "utils/ElfView.h"
#include "utils/FileMemMap.h"
#include "utils/ThreadUtils.h"

namespace qauxv {

void* loader_android_dlopen_ext(const char* filename,
                                int flag,
                                const android_dlextinfo* extinfo,
                                const void* caller_addr) {
    int sdk = qauxv::HostInfo::GetSdkInt();
    // there is no linker namespace pre-N
    if (sdk < 24) {
        return android_dlopen_ext(filename, flag, extinfo);
    }
    static std::mutex sMutex;
    std::scoped_lock lock_(sMutex);
    // 1. get ld-android.so
    static void* handleLoader = nullptr;
    if (handleLoader == nullptr) {
        handleLoader = dlopen("libdl.so", RTLD_NOW | RTLD_NOLOAD);
    }
    if (handleLoader == nullptr) {
        // this should not happen
        LOGE("loader_android_dlopen_ext: failed to find libdl.so");
        return nullptr;
    }
    // for 8.0/SDK26+, we have the famous __loader_android_dlopen_ext
    static std::optional<void*> p_loader_android_dlopen_ext = std::nullopt;
    if (!p_loader_android_dlopen_ext.has_value()) {
        p_loader_android_dlopen_ext = dlsym(handleLoader, "__loader_android_dlopen_ext");
    }
    // for 7.0/7.1, use static symbol __dl__ZL10dlopen_extPKciPK17android_dlextinfoPv
    if (p_loader_android_dlopen_ext.value() == nullptr) {
        using ::utils::ElfView;
        const char* soname;
        // it's actually ld-android.so, not linker(64)
        if constexpr (sizeof(void*) == 8) {
            soname = "linker64";
        } else {
            soname = "linker";
        }
        ::utils::ProcessView processView;
        int rc;
        if ((rc = processView.readProcess(getpid())) != 0) {
            LOGE("HookLoadLibrary: failed to read process, rc = {}", rc);
            return nullptr;
        }
        const void* linkerBaseAddress = nullptr;
        std::string linkerPath;
        for (const auto& m: processView.getModules()) {
            if (m.name == soname || m.name == "ld.so" || m.name == "ld-android.so") {
                linkerBaseAddress = reinterpret_cast<void*>(m.baseAddress);
                linkerPath = m.path;
                break;
            }
        }
        if (linkerBaseAddress == nullptr || linkerPath.empty()) {
            LOGE("HookLoadLibrary: failed to find linker module");
            return nullptr;
        }
        FileMemMap linkerFileMap;
        if ((rc = linkerFileMap.mapFilePath(linkerPath.c_str())) != 0) {
            LOGE("HookLoadLibrary: failed to map linker file, rc = {}", rc);
            return nullptr;
        }
        ElfView linkerElfView;
        linkerElfView.ParseFileMemMapping(linkerFileMap.getAddress(), linkerFileMap.getLength());
        if (!linkerElfView.IsValid()) {
            LOGE("HookLoadLibrary: failed to attach linker file");
            return nullptr;
        }
        auto linkerSymbolResolver = [&](const char* symbol) -> void* {
            auto offset = linkerElfView.GetSymbolOffset(symbol);
            if (offset == 0) {
                return nullptr;
            }
            return reinterpret_cast<void*>(reinterpret_cast<uintptr_t>(linkerBaseAddress) + static_cast<uintptr_t>(offset));
        };
        const char* sym_dlopen_ext = "__dl__ZL10dlopen_extPKciPK17android_dlextinfoPv";
        p_loader_android_dlopen_ext = linkerSymbolResolver(sym_dlopen_ext);
        if (p_loader_android_dlopen_ext.value() == nullptr) {
            LOGE("loader_android_dlopen_ext: failed to find symbol {}", sym_dlopen_ext);
            return nullptr;
        }
    }
    using loader_android_dlopen_ext_t = void* (*)(const char*, int, const android_dlextinfo*, const void*);
    auto fn_loader_android_dlopen_ext = reinterpret_cast<loader_android_dlopen_ext_t>(p_loader_android_dlopen_ext.value());
    if (fn_loader_android_dlopen_ext == nullptr) {
        LOGE("loader_android_dlopen_ext: failed to find symbol");
        return nullptr;
    }
    // invoke the function
    return fn_loader_android_dlopen_ext(filename, flag, extinfo, caller_addr);
}

void* loader_dlopen(const char* filename, int flag, const void* caller_addr) {
    return loader_android_dlopen_ext(filename, flag, nullptr, caller_addr);
}

}
