// QAuxiliary - An Xposed module for QQ/TIM
// Copyright (C) 2019-2023 QAuxiliary developers
// https://github.com/cinit/QAuxiliary
//
// This software is non-free but opensource software: you can redistribute it
// and/or modify it under the terms of the GNU Affero General Public License
// as published by the Free Software Foundation; either
// version 3 of the License, or any later version and our eula as published
// by QAuxiliary contributors.
//
// This software is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// and eula along with this software.  If not, see
// <https://www.gnu.org/licenses/>
// <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.

//
// Created by sulfate on 2023-05-18.
//

#include "NativeCoreBridge.h"

#include <jni.h>
#include <string>
#include <mutex>
#include <vector>
#include <algorithm>
#include <optional>
#include <unistd.h>
#include <cstring>
#include <cerrno>


#include "HostInfo.h"
#include "utils/JniUtils.h"
#include "utils/Log.h"
#include "dobby.h"
#include "utils/FileMemMap.h"
#include "utils/ProcessView.h"
#include "utils/ElfView.h"

#include "natives_utils.h"


struct NativeHookHandle {
    int (* hookFunction)(void* func, void* replace, void** backup);
};

static NativeHookHandle sNativeHookHandle = {};

namespace qauxv {

void HandleLoadLibrary(const char* name, void* handle);

}


namespace qauxv {

std::vector<LoadLibraryCallback> sCallbacks;

std::mutex sCallbacksMutex;
bool sHandleLoadLibraryCallbackInitialized = false;

void HandleLoadLibrary(const char* name, void* handle) {
    std::vector<LoadLibraryCallback> callbacks;
    {
        std::scoped_lock lock(sCallbacksMutex);
        callbacks = sCallbacks;
    }
    for (const auto& callback: callbacks) {
        callback(name, handle);
    }
}

void RegisterLoadLibraryCallback(const LoadLibraryCallback& callback) {
    std::scoped_lock lock(sCallbacksMutex);
    sCallbacks.push_back(callback);
}

static volatile bool sIsNativeInitialized = false;

int CreateInlineHook(void* func, void* replace, void** backup) {
    if (!sIsNativeInitialized) {
        LOGE("CreateInlineHook: native core is not initialized");
        return RS_FAILED;
    }
    return sNativeHookHandle.hookFunction(func, replace, backup);
}

void* (* backup_do_dlopen)(const char* name, int flags, const void* extinfo, const void* caller) = nullptr;

void* fake_do_dlopen(const char* name, int flags, const void* extinfo, const void* caller) {
    auto handle = backup_do_dlopen(name, flags, extinfo, caller);
    HandleLoadLibrary(name, handle);
    return handle;
}


void HookLoadLibrary() {
    using namespace utils;
    LOGD("HookLoadLibrary: native load library callback is not initialized, hook ourselves");
    const char* soname;
    if constexpr (sizeof(void*) == 8) {
        soname = "linker64";
    } else {
        soname = "linker";
    }
    // __dl__Z9do_dlopenPKciPK17android_dlextinfoPKv
    void* symbol = DobbySymbolResolver(soname, "__dl__Z9do_dlopenPKciPK17android_dlextinfoPKv");
    if (symbol == nullptr) {
        LOGE("HookLoadLibrary: failed to find __dl__Z9do_dlopenPKciPK17android_dlextinfoPKv");
        return;
    }
    if (DobbyHook(symbol, (dobby_dummy_func_t) qauxv::fake_do_dlopen, (dobby_dummy_func_t*) &qauxv::backup_do_dlopen) != RS_SUCCESS) {
        LOGE("HookLoadLibrary: failed to hook __dl__Z9do_dlopenPKciPK17android_dlextinfoPKv");
        return;
    }
    LOGD("HookLoadLibrary: hooked __dl__Z9do_dlopenPKciPK17android_dlextinfoPKv");
}

}

// called by Xposed framework
EXPORT extern "C" NativeOnModuleLoaded native_init(const NativeAPIEntries* entries) {
    sNativeHookHandle.hookFunction = entries->hookFunc;
    qauxv::sHandleLoadLibraryCallbackInitialized = true;
    return &qauxv::HandleLoadLibrary;
}


extern "C" JNIEXPORT void JNICALL
Java_io_github_qauxv_core_NativeCoreBridge_initNativeCore(JNIEnv* env,
                                                          jclass,
                                                          jstring package_name,
                                                          jint current_sdk_level,
                                                          jstring version_name,
                                                          jlong long_version_code) {
    using namespace qauxv;
    if (sIsNativeInitialized) {
        LOGE("initNativeCore: native core is already initialized");
        return;
    }
    auto packageName = JstringToString(env, package_name);
    auto versionName = JstringToString(env, version_name);
    if (!packageName.has_value() || !versionName.has_value()) {
        LOGE("initNativeCore: failed to get package name or version name");
        return;
    }
    HostInfo::InitHostInfo(current_sdk_level, packageName.value(),
                           versionName.value(), uint64_t(long_version_code));
    if (sNativeHookHandle.hookFunction == nullptr) {
        LOGD("initNativeCore: native hook function is null, Dobby will be used");
        sNativeHookHandle.hookFunction = +[](void* func, void* replace, void** backup) {
            return DobbyHook((void*) func, (dobby_dummy_func_t) replace, (dobby_dummy_func_t*) backup);
        };
        if (!sHandleLoadLibraryCallbackInitialized) {
            HookLoadLibrary();
        }
    } else {
        LOGD("initNativeCore: native hook function is not null, use it directly");
    }
    sIsNativeInitialized = true;
}
