//
// Created by sulfate on 2023-05-17.
//

#ifndef QAUXILIARY_HOSTINFO_H
#define QAUXILIARY_HOSTINFO_H

#include <cstdint>
#include <string>
#include <string_view>

struct _JavaVM;
typedef _JavaVM JavaVM;

namespace qauxv {

class HostInfo {
public:
    // no instance
    HostInfo() = delete;

    static int GetSdkInt() noexcept;
    static std::string GetPackageName();
    static std::string GetVersionName();
    static std::string GetDataDir();
    static uint32_t GetVersionCode32() noexcept;
    static uint64_t GetLongVersionCode() noexcept;
    static bool IsDebugBuild() noexcept;
    static JavaVM* GetJavaVM() noexcept;

    static void PreInitHostInfo(JavaVM* jvm, std::string dataDir);

    static void InitHostInfo(
            JavaVM* jvm,
            std::string dataDir,
            std::string_view packageName,
            std::string_view versionName,
            uint64_t longVersionCode,
            bool isDebugBuild
    );

};

} // qauxv

#endif // QAUXILIARY_HOSTINFO_H
