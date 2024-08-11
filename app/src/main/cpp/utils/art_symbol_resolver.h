//
// Created by sulfate on 2024-08-10.
//

#ifndef QAUXV_ART_SYMBOL_RESOLVER_H
#define QAUXV_ART_SYMBOL_RESOLVER_H

#include <string>
#include <string_view>
#include <memory>

#include <jni.h>

#include "qauxv_core/native_loader.h"

namespace qauxv {

class ModuleInfoData;

class ModuleSymbolResolver {
public:
    ModuleSymbolResolver(std::string name, std::string path, void* baseAddress,
                         std::unique_ptr<ModuleInfoData> data, qauxv::nativeloader::LibraryIsa isa);
    // disable copy and move constructor and assignment operator
    ModuleSymbolResolver(const ModuleSymbolResolver&) = delete;
    ModuleSymbolResolver& operator=(const ModuleSymbolResolver&) = delete;

    std::string name;
    std::string path;
    qauxv::nativeloader::LibraryIsa isa;
    void* baseAddress = nullptr;
    std::unique_ptr<ModuleInfoData> data;

    [[nodiscard]] inline std::string_view GetName() const {
        return name;
    }

    [[nodiscard]] inline std::string_view GetPath() const {
        return path;
    }

    [[nodiscard]] inline void* GetBaseAddress() const {
        return baseAddress;
    }

    [[nodiscard]] inline qauxv::nativeloader::LibraryIsa GetLibraryIsa() const {
        return isa;
    }

    [[nodiscard]] void* GetSymbol(std::string_view symbol_name) const;

    [[nodiscard]] void* GetSymbolPrefix(std::string_view symbol_prefix) const;

    template<typename T>
    requires std::is_pointer_v<T>
    [[nodiscard]] T GetSymbol(std::string_view symbol_name) const {
        return reinterpret_cast<T>(GetSymbol(symbol_name));
    }

};

const ModuleSymbolResolver* GetModuleSymbolResolver(std::string_view module_name);

std::string GetLibArtPath();

bool InitLibArtElfView();

void* GetLibArtSymbol(std::string_view symbol_name);

void* GetLibArtSymbolPrefix(std::string_view symbol_prefix);

}

#endif //QAUXV_ART_SYMBOL_RESOLVER_H
