//
// Created by sulfate on 2023-05-17.
//

#include "HostInfo.h"

#include <cstdlib>
#include <optional>

#include <android/set_abort_message.h>
#include <android/api-level.h>

#include "utils/Log.h"

[[noreturn]] static void AbortWithMsg(const char* msg) {
    android_set_abort_message(msg);
    abort();
}

#define CHECK(x) if (!(x)) [[unlikely]] { \
    AbortWithMsg("HostInfo: " #x " check failed"); \
} ((void)0)

namespace qauxv {

static int sSdkInt = 0;
static std::optional<std::string> sPackageName;
static std::optional<std::string> sVersionName;
static std::optional<std::string> sDataDir;
static std::optional<uint64_t> sLongVersionCode = 0;
static std::optional<JavaVM*> sJavaVM = nullptr;

// can be overridden by HostInfo::InitHostInfo
#ifdef NDEBUG
static bool sIsDebugBuild = false;
#else
static bool sIsDebugBuild = true;
#endif

int HostInfo::GetSdkInt() noexcept {
    static int sdkInt = android_get_device_api_level();
    return sdkInt;
}

std::string HostInfo::GetPackageName() {
    return sPackageName.value();
}

std::string HostInfo::GetVersionName() {
    return sVersionName.value();
}

uint32_t HostInfo::GetVersionCode32() noexcept {
    return static_cast<uint32_t>(sLongVersionCode.value());
}

uint64_t HostInfo::GetLongVersionCode() noexcept {
    return sLongVersionCode.value();
}

JavaVM* HostInfo::GetJavaVM() noexcept {
    return sJavaVM.value();
}

std::string HostInfo::GetDataDir() {
    return sDataDir.value();
}

bool HostInfo::IsDebugBuild() noexcept {
    return sIsDebugBuild;
}

void HostInfo::PreInitHostInfo(JavaVM* jvm, std::string dataDir) {
    CHECK(jvm != nullptr);
    CHECK(!dataDir.empty());
    sJavaVM = jvm;
    sDataDir = std::move(dataDir);
}

void HostInfo::InitHostInfo(
        JavaVM* jvm,
        std::string dataDir,
        std::string_view packageName,
        std::string_view versionName,
        uint64_t longVersionCode,
        bool isDebugBuild
) {
    CHECK(!packageName.empty());
    CHECK(jvm != nullptr);
    CHECK(!dataDir.empty());
    sJavaVM = jvm;
    sDataDir = std::move(dataDir);
    sPackageName = packageName;
    sVersionName = versionName;
    sLongVersionCode = longVersionCode;
    sIsDebugBuild = isDebugBuild;
}

} // qauxv
