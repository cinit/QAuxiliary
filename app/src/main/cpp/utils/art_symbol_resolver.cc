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
    if (processView.readProcess(getpid()) != 0) {
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
        return nullptr;
    }
    auto data = std::make_unique<ModuleInfoData>();
    // the file map will be un-map when the FileMemMap is destroyed
    FileMemMap fileMap;
    if (fileMap.mapFilePath(path.c_str()) != 0) {
        return nullptr;
    }
    auto startTs = std::chrono::steady_clock::now();
    data->elfView.ParseFileMemMapping(fileMap.getAddress(), fileMap.getLength());
    auto endTs = std::chrono::steady_clock::now();
    if (!data->elfView.IsValid()) {
        return nullptr;
    }
    // LOGD("parse elf file '{}' took {:.3f}ms", path, std::chrono::duration<double, std::milli>(endTs - startTs).count());
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
    if (offset != 0) {
        result = reinterpret_cast<void*>(reinterpret_cast<uintptr_t>(baseAddress) + offset);
    } else {
        result = nullptr;
    }
    return result;
}

void* ModuleSymbolResolver::GetSymbolPrefix(std::string_view symbol_prefix) const {
    auto& info = *data;
    auto offset = info.elfView.GetFirstSymbolOffsetWithPrefix(symbol_prefix);
    void* result;
    if (offset != 0) {
        result = reinterpret_cast<void*>(reinterpret_cast<uintptr_t>(baseAddress) + offset);
    } else {
        result = nullptr;
    }
    return result;
}

ModuleSymbolResolver::ModuleSymbolResolver(std::string name, std::string path, void* baseAddress,
                                           std::unique_ptr<ModuleInfoData> data, qauxv::nativeloader::LibraryIsa isa)
        : name(std::move(name)), path(std::move(path)), baseAddress(baseAddress), data(std::move(data)), isa(isa) {}

} // namespace qauxv
