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
#include <array>
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
#include "utils/ConfigManager.h"

#include "unwindstack/AndroidUnwinder.h"

#include "natives_utils.h"


struct NativeHookHandle {
    int (* hookFunction)(void* func, void* replace, void** backup);
    int (* unhookFunction)(void* func);
};

static NativeHookHandle sNativeHookHandle = {};
static const NativeAPIEntries* sXposedNativeHookEntries = nullptr;

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

int RegisterLoadLibraryCallback(const LoadLibraryCallback& callback) {
    if (!sHandleLoadLibraryCallbackInitialized) {
        return -1;
    }
    std::scoped_lock lock(sCallbacksMutex);
    sCallbacks.push_back(callback);
    return 0;
}

// true means native hook is ready, e.g dobby is used
// this does not guarantee that the linker!do_dlopen is hooked
static volatile bool sIsNativeHookInitialized = false;

int CreateInlineHook(void* func, void* replace, void** backup) {
    if (!sIsNativeHookInitialized) {
        LOGE("CreateInlineHook: native core is not initialized");
        return RS_FAILED;
    }
    return sNativeHookHandle.hookFunction(func, replace, backup);
}

int DestroyInlineHook(void* func) {
    if (!sIsNativeHookInitialized) {
        LOGE("DestroyInlineHook: native core is not initialized");
        return RS_FAILED;
    }
    return sNativeHookHandle.unhookFunction(func);
}

void* backup_do_dlopen = nullptr;

void* fake_do_dlopen_24(const char* name, int flags, const void* extinfo, const void* caller) {
    auto* backup = (void* (*)(const char* name, int flags, const void* extinfo, const void* caller)) backup_do_dlopen;
    auto handle = backup(name, flags, extinfo, caller);
    HandleLoadLibrary(name, handle);
    return handle;
}

void* fake_do_dlopen_23(const char* name, int flags, const void* extinfo) {
    // below Android 7.0, the caller address is not passed to do_dlopen,
    // because there is no linker namespace concept before Android 7.0
    auto* backup = (void* (*)(const char* name, int flags, const void* extinfo)) backup_do_dlopen;
    auto handle = backup(name, flags, extinfo);
    HandleLoadLibrary(name, handle);
    return handle;
}

void HookLoadLibrary() {
    using namespace utils;
    LOGD("HookLoadLibrary: attempting to hook ld-android.so!__dl__Z9do_dlopenPKciPK17android_dlextinfo(PK?v)?");
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
        return;
    }
    const void* linkerBaseAddress = nullptr;
    std::string linkerPath;
    for (const auto& m: processView.getModules()) {
        if (m.name == soname || m.name == "ld-android.so") {
            linkerBaseAddress = reinterpret_cast<void*>(m.baseAddress);
            linkerPath = m.path;
            break;
        }
    }
    if (linkerBaseAddress == nullptr || linkerPath.empty()) {
        LOGE("HookLoadLibrary: failed to find linker module");
        return;
    }
    FileMemMap linkerFileMap;
    if ((rc = linkerFileMap.mapFilePath(linkerPath.c_str())) != 0) {
        LOGE("HookLoadLibrary: failed to map linker file, rc = {}", rc);
        return;
    }
    ::utils::ElfView linkerElfView;
    linkerElfView.ParseFileMemMapping(linkerFileMap.getAddress(), linkerFileMap.getLength());
    if (!linkerElfView.IsValid()) {
        LOGE("HookLoadLibrary: failed to attach linker file");
        return;
    }
    auto linkerSymbolResolver = [&](const char* symbol) -> void* {
        auto offset = linkerElfView.GetSymbolOffset(symbol);
        if (offset == 0) {
            return nullptr;
        }
        return reinterpret_cast<void*>(reinterpret_cast<uintptr_t>(linkerBaseAddress) + static_cast<uintptr_t>(offset));
    };
    bool isBelow24;
    std::string symbolName;
    struct SymbolInfo {
        const char* symbolName;
        bool isBelow24;
    };
    constexpr std::array symbolInfos = {
            // Android 8.0+
            SymbolInfo{"__dl__Z9do_dlopenPKciPK17android_dlextinfoPKv", false},
            // Android 7.x
            SymbolInfo{"__dl__Z9do_dlopenPKciPK17android_dlextinfoPv", false},
            // Android 6.x
            SymbolInfo{"__dl__Z9do_dlopenPKciPK17android_dlextinfo", true},
    };
    void* symbol = nullptr;
    for (const auto& info: symbolInfos) {
        symbol = linkerSymbolResolver(info.symbolName);
        if (symbol != nullptr) {
            symbolName = info.symbolName;
            isBelow24 = info.isBelow24;
            break;
        }
    }
    if (symbol == nullptr) {
        // give up
        LOGE("HookLoadLibrary: failed to find __dl__Z9do_dlopenPKciPK17android_dlextinfo(PK?v)?");
        return;
    }
    // LOGD("linker base {}, do_dlopen {}, path {}", linkerBaseAddress, symbol, linkerPath);
    auto hookHandler = isBelow24 ? (dobby_dummy_func_t) fake_do_dlopen_23 : (dobby_dummy_func_t) fake_do_dlopen_24;
    if (DobbyHook(symbol, hookHandler, (dobby_dummy_func_t*) &qauxv::backup_do_dlopen) != RS_SUCCESS) {
        LOGE("HookLoadLibrary: failed to hook {} at {}", symbolName, symbol);
        return;
    }
    sHandleLoadLibraryCallbackInitialized = true;
    LOGD("HookLoadLibrary: hooked {}", symbolName);
}

std::string TakeStackTraceAsString() {
    using namespace unwindstack;
    std::string stackTrace;
    AndroidUnwinderData data;
    AndroidLocalUnwinder unwinder;
    auto tid = gettid();
    bool result = unwinder.Unwind(tid, data);
    if (!result) {
        stackTrace = fmt::format("Unwind failed for pid {}, tid {}, error: {}", getpid(), tid, data.GetErrorString());
        return stackTrace;
    }
    for (const auto& frame: data.frames) {
        stackTrace += unwinder.FormatFrame(frame) + "\n";
    }
    return stackTrace;
}

void TraceError(JNIEnv* env, jobject thiz, std::string_view errMsg) {
    bool isAttachedManually = false;
    if (thiz == nullptr) {
        LOGE("TraceError fatal thiz == null");
        return;
    }
    if (errMsg.empty()) {
        LOGE("TraceError fatal errMsg == null");
        return;
    }
    std::string errMsgWithStack = std::string(errMsg) + "\n" + TakeStackTraceAsString();
    if (env == nullptr) {
        JavaVM* vm = HostInfo::GetJavaVM();
        if (vm == nullptr) {
            LOGE("TraceError fatal vm == null");
            return;
        }
        // check if current thread is attached to jvm
        jint err = vm->GetEnv((void**) &env, JNI_VERSION_1_6);
        if (err == JNI_EDETACHED) {
            if (vm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
                LOGE("TraceError fatal AttachCurrentThread failed");
                return;
            }
            isAttachedManually = true;
        } else if (env == nullptr) {
            LOGE("TraceError fatal GetEnv failed, err = {}", err);
            return;
        }
    }
    const auto fnDetachCurrentThread = [isAttachedManually, env]() {
        if (isAttachedManually) {
            JavaVM* vm = nullptr;
            if (env->GetJavaVM(&vm) != JNI_OK) {
                LOGE("TraceError fatal GetJavaVM failed");
                return;
            }
            if (vm->DetachCurrentThread() != JNI_OK) {
                LOGE("TraceError fatal DetachCurrentThread failed");
                return;
            }
        }
    };
    // this method is typically not called frequently, so we don't need to care about performance
    if (env->PushLocalFrame(16) != JNI_OK) {
        LOGE("TraceError fatal PushLocalFrame failed");
        env->ExceptionDescribe();
        env->ExceptionClear();
        fnDetachCurrentThread();
        return;
    }
    const auto fnPopLocalFrame = [env]() {
        if (env->PopLocalFrame(nullptr) != JNI_OK) {
            LOGE("TraceError fatal PopLocalFrame failed");
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
    };
    const auto fnGetClassLoader = [env, thiz]() -> jobject {
        jclass clazz = env->GetObjectClass(thiz);
        if (clazz == nullptr) {
            LOGE("TraceError fatal GetObjectClass failed");
            env->ExceptionDescribe();
            env->ExceptionClear();
            return nullptr;
        }
        jclass kClass = env->FindClass("java/lang/Class");
        if (kClass == nullptr) {
            LOGE("TraceError fatal GetObjectClass failed");
            env->ExceptionDescribe();
            env->ExceptionClear();
            return nullptr;
        }
        jmethodID getClassLoaderMethod = env->GetMethodID(kClass, "getClassLoader", "()Ljava/lang/ClassLoader;");
        if (getClassLoaderMethod == nullptr) {
            LOGE("TraceError fatal GetMethodID getClassLoader failed");
            env->ExceptionDescribe();
            env->ExceptionClear();
            return nullptr;
        }
        jobject classLoader = env->CallObjectMethod(clazz, getClassLoaderMethod);
        if (classLoader == nullptr) {
            LOGE("TraceError fatal CallObjectMethod getClassLoader failed");
            env->ExceptionDescribe();
            env->ExceptionClear();
            return nullptr;
        }
        return classLoader;
    };
    const auto fnCallTraceErrorMethod = [env, thiz, errMsgWithStack](jobject classloader) {
        jclass kClassLoader = env->FindClass("java/lang/ClassLoader");
        jmethodID loadClassMethod = env->GetMethodID(kClassLoader, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
        jclass kClass = (jclass) env->CallObjectMethod(classloader, loadClassMethod, env->NewStringUTF("io.github.qauxv.core.NativeCoreBridge"));
        if (kClass == nullptr) {
            LOGE("TraceError fatal CallObjectMethod loadClass NativeCoreBridge failed");
            env->ExceptionDescribe();
            env->ExceptionClear();
            return;
        }
        jmethodID nativeTraceErrorHelperMethod = env->GetStaticMethodID(kClass, "nativeTraceErrorHelper", "(Ljava/lang/Object;Ljava/lang/Throwable;)V");
        if (nativeTraceErrorHelperMethod == nullptr) {
            LOGE("TraceError fatal GetStaticMethodID nativeTraceErrorHelper failed");
            env->ExceptionDescribe();
            env->ExceptionClear();
            return;
        }
        jstring errMsgJString = env->NewStringUTF(errMsgWithStack.data());
        if (errMsgJString == nullptr) {
            LOGE("TraceError fatal NewStringUTF failed, original error message: {}", errMsgWithStack);
            env->ExceptionDescribe();
            env->ExceptionClear();
            return;
        }
        auto th = (jthrowable) env->NewObject(env->FindClass("java/lang/RuntimeException"),
                                              env->GetMethodID(env->FindClass("java/lang/RuntimeException"),
                                                               "<init>", "(Ljava/lang/String;)V"),
                                              errMsgJString);
        env->CallStaticVoidMethod(kClass, nativeTraceErrorHelperMethod, thiz, th);
        if (env->ExceptionCheck()) {
            LOGE("TraceError fatal CallStaticVoidMethod nativeTraceErrorHelper failed, original error message: {}",
                 errMsgWithStack);
            env->ExceptionDescribe();
            env->ExceptionClear();
            return;
        }
    };
    auto classLoader = fnGetClassLoader();
    if (classLoader != nullptr) {
        fnCallTraceErrorMethod(classLoader);
    }
    fnPopLocalFrame();
    fnDetachCurrentThread();
}

static NativeLibraryInitMode sCurrentNativeLibraryInitMode = NativeLibraryInitMode::kNone;

NativeLibraryInitMode GetCurrentNativeLibraryInitMode() {
    return sCurrentNativeLibraryInitMode;
}

void SetCurrentNativeLibraryInitMode(NativeLibraryInitMode mode) {
    auto current = sCurrentNativeLibraryInitMode;
    if (current != mode && current != NativeLibraryInitMode::kNone) {
        qauxv::utils::Abort(fmt::format("attempt to change current native library init mode from {} to {}", uint32_t(current), uint32_t(mode)));
    }
    sCurrentNativeLibraryInitMode = mode;
}

}

// called by Xposed framework
EXPORT extern "C" [[maybe_unused]] NativeOnModuleLoaded native_init(const NativeAPIEntries* entries) {
    sXposedNativeHookEntries = entries;
    sNativeHookHandle.hookFunction = entries->hookFunc;
    sNativeHookHandle.unhookFunction = entries->unhookFunc;
    qauxv::sHandleLoadLibraryCallbackInitialized = true;
    return &qauxv::HandleLoadLibrary;
}

__attribute__((visibility("protected")))
void ChainLoaderCallTargetNativeInit(NativeInit func) {
    static const NativeAPIEntries sFallbackNativeAPIEntries = {
            .version=1,
            .hookFunc=qauxv::CreateInlineHook,
            .unhookFunc=qauxv::DestroyInlineHook
    };
    const NativeAPIEntries* entries = sXposedNativeHookEntries ? sXposedNativeHookEntries : &sFallbackNativeAPIEntries;
    auto callback = func(entries);
    if (callback != nullptr) {
        qauxv::RegisterLoadLibraryCallback(callback);
    }
}

void qauxv::InitializeNativeHookApi(bool allowHookLinker) {
    using namespace qauxv;
    if (sIsNativeHookInitialized) {
        LOGE("initNativeCore: native core is already initialized");
        return;
    }
    if (sNativeHookHandle.hookFunction == nullptr) {
        LOGD("initNativeCore: native hook function is null, Dobby will be used");
        sNativeHookHandle.hookFunction = +[](void* func, void* replace, void** backup) {
            return DobbyHook((void*) func, (dobby_dummy_func_t) replace, (dobby_dummy_func_t*) backup);
        };
        sNativeHookHandle.unhookFunction = +[](void* func) {
            return DobbyDestroy((void*) func);
        };
        if (!sHandleLoadLibraryCallbackInitialized) {
            if (allowHookLinker) {
                HookLoadLibrary();
            } else {
                LOGD("No HandleLoadLibrary callback and linker hooking is disabled");
            }
        }
    } else {
        LOGD("initNativeCore: native hook function is not null, use it directly");
    }
    sIsNativeHookInitialized = true;
}

bool qauxv::IsNativeHookApiInitialized() {
    return sIsNativeHookInitialized;
}
