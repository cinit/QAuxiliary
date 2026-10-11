//
// Created by sulfate on 2024-08-10.
//

#include "art_symbol_resolver.h"

#include <cstdint>
#include <vector>
#include <mutex>
#include <string>
#include <jni.h>
#include <unistd.h>
#include <sys/mman.h>
#include <cerrno>
#include <unordered_map>
#include <chrono>

#include <fmt/format.h>

#include "qauxv_core/NativeCoreBridge.h"
#include "utils/FileMemMap.h"
#include "utils/ProcessView.h"
#include "utils/ElfView.h"
#include "utils/Log.h"

// for mmkv::KeyHasher, mmkv::KeyEqualer
#include "MMKV.h"

// Set to 1, or pass -DQAUXV_ART_SYMBOL_RESOLVER_DEBUG=1, for verbose ART symbol lookup diagnostics.
#ifndef QAUXV_ART_SYMBOL_RESOLVER_DEBUG
#define QAUXV_ART_SYMBOL_RESOLVER_DEBUG 0
#endif

#if QAUXV_ART_SYMBOL_RESOLVER_DEBUG
#define ART_SYMBOL_RESOLVER_LOGD(...) LOGD(__VA_ARGS__)
#else
#define ART_SYMBOL_RESOLVER_LOGD(...) ((void) 0)
#endif

namespace qauxv {

class ModuleInfoData {
public:
    ::utils::ElfView elfView;
};

static std::mutex sInitMutex;
static std::unordered_map<std::string, ModuleSymbolResolver*, mmkv::KeyHasher, mmkv::KeyEqualer> sModuleInfoMap;

const ModuleSymbolResolver* GetModuleSymbolResolver(std::string_view module_name) {
    {
        // look up in cache
        std::lock_guard<std::mutex> lock(sInitMutex);
        auto it = sModuleInfoMap.find(module_name);
        if (it != sModuleInfoMap.end()) {
            return it->second;
        }
    }
    ::utils::ProcessView processView;
    if (int rc = processView.readProcess(getpid()); rc != 0) {
        LOGW("ProcessView::readProcess failed, rc = {}", rc);
        return nullptr;
    }
    std::string path;
    void* baseAddress;
    for (const auto& m: processView.getModules()) {
        if (m.name == module_name) {
            baseAddress = reinterpret_cast<void*>(m.baseAddress);
            path = m.path;
        }
    }
    if (path.empty() || baseAddress == nullptr) {
#if QAUXV_ART_SYMBOL_RESOLVER_DEBUG
        LOGW("GetModuleSymbolResolver: module '{}' not found in process maps", module_name);
        // do a simple dump /proc/pid/maps | grep soname with ART_SYMBOL_RESOLVER_LOGD
        int pid = getpid();
        auto mapsPath = fmt::format("/proc/{}/maps", pid);
        std::vector<std::string> relatedLines;
        FILE* pf = fopen(mapsPath.c_str(), "r");
        if (pf) {
            char buffer[4096];
            while (fgets(buffer, sizeof(buffer), pf)) {
                if (strstr(buffer, module_name.data())) {
                    relatedLines.emplace_back(buffer);
                }
            }
            fclose(pf);
        }
        ART_SYMBOL_RESOLVER_LOGD("Maps related to module '{}' ({} lines):", module_name, relatedLines.size());
        // logd may randomly discard lines, add line numbers, dump 100 line for max in case of SW bug
        for (size_t i = 0; i < relatedLines.size() && i < 100; ++i) {
            ART_SYMBOL_RESOLVER_LOGD("  [{:02d}] {}", i, relatedLines[i]);
        }
        ART_SYMBOL_RESOLVER_LOGD("End of maps related to module '{}'", module_name);
#endif
        return nullptr;
    }
    auto data = std::make_unique<ModuleInfoData>();
    // the file map will be un-map when the FileMemMap is destroyed
    FileMemMap fileMap;
    if (int rc = fileMap.mapFilePath(path.c_str()); rc != 0) {
        LOGW("FileMemMap::mapFilePath '{}' failed for module '{}', rc = {}", path, module_name, rc);
        return nullptr;
    }
#if QAUXV_ART_SYMBOL_RESOLVER_DEBUG
    const auto startTs = std::chrono::steady_clock::now();
#endif
    data->elfView.ParseFileMemMapping(fileMap.getAddress(), fileMap.getLength());
#if QAUXV_ART_SYMBOL_RESOLVER_DEBUG
    const auto endTs = std::chrono::steady_clock::now();
#endif
    if (!data->elfView.IsValid()) {
        return nullptr;
    }
    ART_SYMBOL_RESOLVER_LOGD("ART resolver module '{}': path='{}', base=0x{:x}, loadBias=0x{:x}, loadedSize=0x{:x}, parseTimeMs={:.3f}",
                             module_name, path, reinterpret_cast<uintptr_t>(baseAddress), data->elfView.GetLoadBias(),
                             data->elfView.GetLoadedSize(),
                             std::chrono::duration<double, std::milli>(endTs - startTs).count());
    std::span<const uint8_t, 32> header{reinterpret_cast<const uint8_t*>(fileMap.getAddress()), 32};
    auto isa = qauxv::nativeloader::GetLibraryIsaWithElfHeader(header);
    auto* resolver = new ModuleSymbolResolver(std::string(module_name), path, baseAddress, std::move(data), isa);
    {
        std::lock_guard<std::mutex> lock(sInitMutex);
        sModuleInfoMap.emplace(module_name, resolver);
    }
    return resolver;
}

std::string GetLibArtPath() {
    auto libart = GetModuleSymbolResolver("libart.so");
    if (libart == nullptr) {
        return "";
    }
    return std::string{libart->GetPath()};
}

bool InitLibArtElfView() {
    return GetModuleSymbolResolver("libart.so") != nullptr;
}

void* GetLibArtSymbol(std::string_view symbol_name) {
    auto libart = GetModuleSymbolResolver("libart.so");
    if (libart == nullptr) {
        LOGE("GetModuleSymbolResolver(libart.so) failed");
        return nullptr;
    }
    return libart->GetSymbol(symbol_name);
}

void* GetLibArtSymbolPrefix(std::string_view symbol_prefix) {
    auto libart = GetModuleSymbolResolver("libart.so");
    if (libart == nullptr) {
        LOGE("GetModuleSymbolResolver(libart.so) failed");
        return nullptr;
    }
    return libart->GetSymbolPrefix(symbol_prefix);
}

void* ModuleSymbolResolver::GetSymbol(std::string_view symbol_name) const {
    auto& info = *data;
    auto offset = info.elfView.GetSymbolOffset(symbol_name);
    void* result;
    auto base = reinterpret_cast<uintptr_t>(baseAddress);
    if (offset != 0) {
        auto va = base + offset;
        result = reinterpret_cast<void*>(va);
        ART_SYMBOL_RESOLVER_LOGD(
                "ART resolver exact hit: module='{}', symbol='{}', base=0x{:x}, offset=0x{:x}, base+offset=0x{:x}, va={}",
                name, symbol_name, base, offset, va, result);
    } else {
        result = nullptr;
        ART_SYMBOL_RESOLVER_LOGD("ART resolver exact miss: module='{}', symbol='{}', base=0x{:x}", name, symbol_name, base);
    }
    return result;
}

void* ModuleSymbolResolver::GetSymbolPrefix(std::string_view symbol_prefix) const {
    auto& info = *data;
#if QAUXV_ART_SYMBOL_RESOLVER_DEBUG
    auto [matchedSymbol, offset] = info.elfView.GetFirstSymbolWithPrefix(symbol_prefix);
#else
    auto offset = info.elfView.GetFirstSymbolOffsetWithPrefix(symbol_prefix);
#endif
    void* result;
    auto base = reinterpret_cast<uintptr_t>(baseAddress);
    if (offset != 0) {
        auto va = base + offset;
        result = reinterpret_cast<void*>(va);
        ART_SYMBOL_RESOLVER_LOGD(
                "ART resolver prefix hit: module='{}', prefix='{}', match='{}', base=0x{:x}, offset=0x{:x}, base+offset=0x{:x}, va={}",
                name, symbol_prefix, matchedSymbol, base, offset, va, result);
    } else {
        result = nullptr;
        ART_SYMBOL_RESOLVER_LOGD("ART resolver prefix miss: module='{}', prefix='{}', base=0x{:x}", name, symbol_prefix, base);
    }
    return result;
}

ModuleSymbolResolver::ModuleSymbolResolver(std::string name, std::string path, void* baseAddress,
                                           std::unique_ptr<ModuleInfoData> data, qauxv::nativeloader::LibraryIsa isa)
        : name(std::move(name)), path(std::move(path)), baseAddress(baseAddress), data(std::move(data)), isa(isa) {}

} // namespace qauxv
