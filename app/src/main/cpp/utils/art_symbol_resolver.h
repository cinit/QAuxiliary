//
// Created by sulfate on 2024-08-10.
//

#ifndef QAUXV_ART_SYMBOL_RESOLVER_H
#define QAUXV_ART_SYMBOL_RESOLVER_H

#include <string>
#include <string_view>
#include <memory>

#include <jni.h>

namespace qauxv {

class ModuleInfoData;

class ModuleSymbolResolver {
public:
    ModuleSymbolResolver(std::string name, std::string path, void* baseAddress, std::unique_ptr<ModuleInfoData> data);
    // disable copy and move constructor and assignment operator
    ModuleSymbolResolver(const ModuleSymbolResolver&) = delete;
    ModuleSymbolResolver& operator=(const ModuleSymbolResolver&) = delete;

    std::string name;
    std::string path;
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

    [[nodiscard]] void* GetSymbol(std::string_view symbol_name) const;

    [[nodiscard]] void* GetSymbolPrefix(std::string_view symbol_prefix) const;

};

const ModuleSymbolResolver* GetModuleSymbolResolver(std::string_view module_name);

std::string GetLibArtPath();

bool InitLibArtElfView();

void* GetLibArtSymbol(std::string_view symbol_name);

void* GetLibArtSymbolPrefix(std::string_view symbol_prefix);

}

#endif //QAUXV_ART_SYMBOL_RESOLVER_H
