//
// Created by sulfate on 2023-05-17.
//

#include "HostInfo.h"

#include "utils/Log.h"

namespace qauxv {

static int sSdkInt = 0;
static std::string sPackageName;
static std::string sVersionName;
static uint64_t sLongVersionCode = 0;

int HostInfo::GetSdkInt() noexcept {
    return sSdkInt;
}

std::string HostInfo::GetPackageName() {
    return sPackageName;
}

std::string HostInfo::GetVersionName() {
    return sVersionName;
}

uint32_t HostInfo::GetVersionCode32() noexcept {
    return static_cast<uint32_t>(sLongVersionCode);
}

uint64_t HostInfo::GetLongVersionCode() noexcept {
    return sLongVersionCode;
}

void HostInfo::InitHostInfo(int sdkInt,
                            std::string_view packageName,
                            std::string_view versionName,
                            uint64_t longVersionCode) {
    if (packageName.empty() || versionName.empty()) {
        LOGE("InitHostInfo failed, invalid packageName or versionName");
        return;
    }
    sSdkInt = sdkInt;
    sPackageName = packageName;
    sVersionName = versionName;
    sLongVersionCode = longVersionCode;
}

} // qauxv
