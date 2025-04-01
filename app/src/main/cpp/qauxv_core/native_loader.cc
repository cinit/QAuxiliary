//
// Created by sulfate on 2024-08-10.
//

#include "native_loader.h"

#include <array>
#include <cstdint>
#include <vector>
#include <memory>

#include <unistd.h>
#include <fcntl.h>
#include <dlfcn.h>
#include <sys/stat.h>

#include <fmt/format.h>
#include <jni.h>

#include "NativeCoreBridge.h"
#include "HostInfo.h"
#include "jni_method_registry.h"
#include "utils/JniUtils.h"
#include "utils/Log.h"
#include "natives_utils.h"
#include "utils/art_symbol_resolver.h"
#include "nativebridge/native_bridge.h"
#include "LsplantBridge.h"

#include "MMKV.h"

using qauxv::JstringToString;

qauxv::nativeloader::LibraryIsa qauxv::nativeloader::GetLibraryIsaWithElfHeader(std::span<const uint8_t, 32> elf_header) {
    using qauxv::nativeloader::LibraryIsa;
    // don't include elf.h here.
    // check magic number
    if (elf_header[0] != 0x7F || elf_header[1] != 'E' || elf_header[2] != 'L' || elf_header[3] != 'F') {
        return LibraryIsa::kUnknown;
    }
    uint8_t klass = elf_header[4];
    if (klass != 1 && klass != 2) {
        return LibraryIsa::kUnknown;
    }
    int offsetMachine = 16 + 2;
    uint32_t m0 = elf_header[offsetMachine];
    uint32_t m1 = elf_header[offsetMachine + 1];
    uint32_t machine = ((m1 << 8) & 0xff00u) | (m0 & 0xffu);
    return LibraryIsa((uint32_t(klass) << 16u) | machine);
}

static jobject sModuleMainClassLoader = nullptr;

static bool sPrimaryPreInitMethodsRegistered = false;
static bool sPrimaryMmkvMethodsRegistered = false;
static bool sPrimaryFullInitMethodsRegistered = false;
static bool sSecondaryFullInitMethodsRegistered = false;

// private static native void nativePrimaryNativeLibraryPreInit(@NonNull String dataDir, boolean allowHookLinker);
extern "C"
JNIEXPORT void JNICALL
Java_io_github_qauxv_util_soloader_NativeLoader_nativePrimaryNativeLibraryPreInit(JNIEnv* env, jclass clazz, jstring data_dir_j, jboolean allow_hook_linker) {
    // env must associate with the the main class loader when this method is called
    auto dataDir = JstringToString(env, data_dir_j).value_or("");
    if (dataDir.empty()) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "dataDir is empty");
        return;
    }
    if (access(dataDir.c_str(), F_OK) != 0) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), fmt::format("dataDir {} not exists", dataDir).c_str());
        return;
    }
    JavaVM* vm = nullptr;
    env->GetJavaVM(&vm);
    if (vm == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), "GetJavaVM failed");
        return;
    }
    static bool sPrimaryPreInitDone = false;
    if (sPrimaryPreInitDone) {
        return;
    }
    if (sModuleMainClassLoader == nullptr) {
        // use the current class loader
        jclass kClass = env->FindClass("java/lang/Class");
        jmethodID kGetClassLoader = env->GetMethodID(kClass, "getClassLoader", "()Ljava/lang/ClassLoader;");
        sModuleMainClassLoader = env->NewGlobalRef(env->CallObjectMethod(clazz, kGetClassLoader));
    }
    qauxv::HostInfo::PreInitHostInfo(vm, dataDir);
    qauxv::InitializeNativeHookApi(allow_hook_linker);
    if (!sPrimaryPreInitMethodsRegistered) {
        qauxv::jniutil::RegisterJniLateInitMethodsToClassLoader(env, qauxv::jniutil::JniMethodInitType::kPrimaryPreInit, sModuleMainClassLoader);
        sPrimaryPreInitMethodsRegistered = true;
    }
    if (!sPrimaryMmkvMethodsRegistered) {
        if (jint rc; (rc = MMKV_JNI_OnLoad(vm, nullptr)) < 0) {
            qauxv::ThrowIfNoPendingException(env, "java/lang/RuntimeException", fmt::format("MMKV_JNI_OnLoad failed with code {}", rc));
            return;
        }
        sPrimaryMmkvMethodsRegistered = true;
    }
    sPrimaryPreInitDone = true;
}

void DoNativeLibraryFullInitializeFormJni(JNIEnv* env,
                                          jclass clazz,
                                          jint init_mode,
                                          jstring data_dir_j,
                                          jstring package_name_j,
                                          [[maybe_unused]] jint current_sdk_level,
                                          jstring version_name_j,
                                          jlong long_version_code,
                                          jboolean is_debug_build) {
    using namespace qauxv;
    using namespace qauxv::jniutil;
    using namespace qauxv::nativeloader;
    // when this method is called, the primary pre-init must be done for primary library
    // env must associate with the the main class loader when this method is called
    auto dataDir = JstringToString(env, data_dir_j).value_or("");
    if (dataDir.empty()) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "dataDir is empty");
        return;
    }
    if (access(dataDir.c_str(), F_OK) != 0) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), fmt::format("dataDir {} not exists", dataDir).c_str());
        return;
    }
    auto packageName = JstringToString(env, package_name_j).value_or("");
    if (packageName.empty()) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "packageName is empty");
        return;
    }
    auto versionName = JstringToString(env, version_name_j).value_or("");
    long versionCode = static_cast<long>(long_version_code);
    auto initMode = static_cast<NativeLibraryInitMode>(init_mode);
    // check init mode value
    if (initMode != NativeLibraryInitMode::kPrimaryOnly && initMode != NativeLibraryInitMode::kSecondaryOnly &&
            initMode != NativeLibraryInitMode::kBothPrimaryAndSecondary) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), fmt::format("Invalid init mode {}", init_mode).c_str());
        return;
    }
    if (sModuleMainClassLoader == nullptr) {
        // use the current class loader
        jclass kClass = env->FindClass("java/lang/Class");
        jmethodID kGetClassLoader = env->GetMethodID(kClass, "getClassLoader", "()Ljava/lang/ClassLoader;");
        sModuleMainClassLoader = env->NewGlobalRef(env->CallObjectMethod(clazz, kGetClassLoader));
    }
    JavaVM* vm = nullptr;
    env->GetJavaVM(&vm);
    if (vm == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), "GetJavaVM failed");
        return;
    }
    // full initialize start
    SetCurrentNativeLibraryInitMode(initMode);
    bool isPrimary = initMode == NativeLibraryInitMode::kPrimaryOnly || initMode == NativeLibraryInitMode::kBothPrimaryAndSecondary;
    HostInfo::InitHostInfo(vm, dataDir, packageName, versionName, versionCode, is_debug_build);
    if (!IsNativeHookApiInitialized()) {
        InitializeNativeHookApi(isPrimary);
    }
    if (isPrimary) {
        // is pre-init done?
        if (!sPrimaryPreInitMethodsRegistered) {
            qauxv::jniutil::RegisterJniLateInitMethodsToClassLoader(env, qauxv::jniutil::JniMethodInitType::kPrimaryPreInit, sModuleMainClassLoader);
            sPrimaryPreInitMethodsRegistered = true;
        }
        if (!sPrimaryMmkvMethodsRegistered) {
            if (jint rc; (rc = MMKV_JNI_OnLoad(vm, nullptr)) < 0) {
                qauxv::ThrowIfNoPendingException(env, "java/lang/RuntimeException", fmt::format("MMKV_JNI_OnLoad failed with code {}", rc));
                return;
            }
            sPrimaryMmkvMethodsRegistered = true;
        }
    }
    // register full init methods
    if (initMode == NativeLibraryInitMode::kPrimaryOnly) {
        if (!sPrimaryFullInitMethodsRegistered) {
            qauxv::jniutil::RegisterJniLateInitMethodsToClassLoader(env, qauxv::jniutil::JniMethodInitType::kPrimaryFullInit, sModuleMainClassLoader);
            sPrimaryFullInitMethodsRegistered = true;
        }
    } else if (initMode == NativeLibraryInitMode::kSecondaryOnly) {
        if (!sSecondaryFullInitMethodsRegistered) {
            qauxv::jniutil::RegisterJniLateInitMethodsToClassLoader(env, qauxv::jniutil::JniMethodInitType::kSecondaryFullInit, sModuleMainClassLoader);
            sSecondaryFullInitMethodsRegistered = true;
        }
    } else {
        if (!sPrimaryFullInitMethodsRegistered) {
            qauxv::jniutil::RegisterJniLateInitMethodsToClassLoader(env, qauxv::jniutil::JniMethodInitType::kPrimaryFullInit, sModuleMainClassLoader);
            sPrimaryFullInitMethodsRegistered = true;
        }
        if (!sSecondaryFullInitMethodsRegistered) {
            qauxv::jniutil::RegisterJniLateInitMethodsToClassLoader(env, qauxv::jniutil::JniMethodInitType::kSecondaryFullInit, sModuleMainClassLoader);
            sSecondaryFullInitMethodsRegistered = true;
        }
    }
    // If we are in secondary only mode, we need to initialize MMKV here.
    // Primary mode will initialize MMKV from Java side. Secondary mode do not have Java binding, so we need to initialize it here.
    if (initMode == NativeLibraryInitMode::kSecondaryOnly) {
        std::string mmkvRootDir = dataDir + "/files/qa_mmkv";
        // Since primary mode will initialize MMKV from Java side, and that must happen before this method is called,
        // so this directory should be created by primary mode.
        // If the directory does not exist, it means there is something wrong.
        if (access(mmkvRootDir.c_str(), F_OK) != 0) {
            env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), fmt::format("MMKV root directory {} not exists", mmkvRootDir).c_str());
            return;
        }
        MMKV::initializeMMKV(mmkvRootDir, HostInfo::IsDebugBuild() ? MMKVLogLevel::MMKVLogDebug : MMKVLogLevel::MMKVLogInfo);
    }
    if (isPrimary) {
        HookArtProfileSaver();
    }
}


//  private static native void nativePrimaryNativeLibraryFullInit(int initMode, @NonNull String dataDir,
//            String packageName, int currentSdkLevel, String versionName, long longVersionCode, boolean isDebugBuild);
extern "C"
JNIEXPORT void JNICALL
Java_io_github_qauxv_util_soloader_NativeLoader_nativePrimaryNativeLibraryFullInit(JNIEnv* env,
                                                                                   jclass clazz,
                                                                                   jint init_mode,
                                                                                   jstring data_dir,
                                                                                   jstring package_name,
                                                                                   [[maybe_unused]] jint current_sdk_level,
                                                                                   jstring version_name,
                                                                                   jlong long_version_code,
                                                                                   jboolean is_debug_build) {
    DoNativeLibraryFullInitializeFormJni(env, clazz, init_mode, data_dir, package_name, current_sdk_level, version_name, long_version_code, is_debug_build);
}


// private static native void nativeSecondaryNativeLibraryFullInit(int initMode, @NonNull String dataDir,
//            String packageName, int currentSdkLevel, String versionName, long longVersionCode, boolean isDebugBuild);
extern "C"
JNIEXPORT void JNICALL
Java_io_github_qauxv_util_soloader_NativeLoader_nativeSecondaryNativeLibraryFullInit(JNIEnv* env,
                                                                                     jclass clazz,
                                                                                     jint init_mode,
                                                                                     jstring data_dir,
                                                                                     jstring package_name,
                                                                                     [[maybe_unused]] jint current_sdk_level,
                                                                                     jstring version_name,
                                                                                     jlong long_version_code,
                                                                                     jboolean is_debug_build) {
    DoNativeLibraryFullInitializeFormJni(env, clazz, init_mode, data_dir, package_name, current_sdk_level, version_name, long_version_code, is_debug_build);
}

// public static native int getPrimaryNativeLibraryIsa();
extern "C"
JNIEXPORT jint JNICALL
Java_io_github_qauxv_util_soloader_NativeLoader_getPrimaryNativeLibraryIsa([[maybe_unused]] JNIEnv*, [[maybe_unused]]  jclass) {
    return qauxv::nativeloader::GetCurrentLibraryIsa();
}

// public static native int getSecondaryNativeLibraryIsa();
extern "C"
JNIEXPORT jint JNICALL
Java_io_github_qauxv_util_soloader_NativeLoader_getSecondaryNativeLibraryIsa([[maybe_unused]] JNIEnv*, [[maybe_unused]] jclass) {
    return qauxv::nativeloader::GetCurrentLibraryIsa();
}

struct SecondaryLibraryInitInputInfo {
    qauxv::nativeloader::LibraryIsa runtimeIsa;
    qauxv::nativeloader::LibraryIsa secondaryIsa;
};

struct SecondaryLibraryInitOutputInfo {
    struct JniClass {
        const char* klass;
        std::vector<JNINativeMethod> methods;
    };
    int64_t result;
    std::vector<JniClass> classes;
};

std::string GetShortyFromSignature(std::string_view signature) {
    // without return type
    // "(IJLjava/lang/String;Ljava/lang/String;ILjava/lang/String;J)V" -> "IJLLILJ"
    std::string shorty;
    for (size_t i = 1; i < signature.size(); i++) {
        char c = signature[i];
        // start at '('
        if (c == '(') {
            continue;
        } else if (c == ')') {
            break;
        } else if (c == 'L') {
            shorty.push_back('L');
            i++;
            while (signature[i] != ';') {
                i++;
            }
        } else if (c == '[') {
            shorty.push_back('L');
            // is multi-dimensional array?
            while (signature[i] == '[') {
                i++;
            }
            // check component type
            if (signature[i] == 'L') {
                i++;
                while (signature[i] != ';') {
                    i++;
                }
            }
        } else {
            // primitive type
            shorty.push_back(c);
        }
    }
    return shorty;
}

// private static native void nativeLoadSecondaryNativeLibrary(@NonNull String modulePath, @NonNull String entryPath,
//      @NonNull ClassLoader classLoader, int isa);
extern "C"
JNIEXPORT jlong JNICALL
Java_io_github_qauxv_util_soloader_NativeLoader_nativeLoadSecondaryNativeLibrary
        (JNIEnv* env, jclass clazz, jstring module_path_j, jstring entry_path_l, jobject native_loader, jint isa) {
    using namespace qauxv;
    using namespace qauxv::nativeloader;
    auto modulePath = JstringToString(env, module_path_j).value_or("");
    auto entryPath = JstringToString(env, entry_path_l).value_or("");
    if (modulePath.empty() || entryPath.empty()) {
        ThrowIfNoPendingException(env, ExceptionNames::kIllegalArgumentException, "modulePath or entryPath is empty");
        return 0;
    }
    if (native_loader == nullptr) {
        ThrowIfNoPendingException(env, ExceptionNames::kNullPointerException, "class_loader is null");
        return 0;
    }
    // check file existence
    if (access(modulePath.c_str(), F_OK) != 0) {
        ThrowIfNoPendingException(env, ExceptionNames::kIllegalArgumentException, fmt::format("modulePath {} not exists", modulePath));
        return 0;
    }
    auto libnativeloader = GetModuleSymbolResolver("libnativeloader.so");
    if (libnativeloader == nullptr) {
        ThrowIfNoPendingException(env, ExceptionNames::kIllegalStateException, "libnativeloader.so not found");
        return 0;
    }
    auto libnativebridge = GetModuleSymbolResolver("libnativebridge.so");
    if (libnativebridge == nullptr) {
        ThrowIfNoPendingException(env, ExceptionNames::kIllegalStateException, "libnativebridge.so not found");
        return 0;
    }
    // Android 10-14 / SDK 29-34
    //  void* OpenNativeLibrary(
    //    JNIEnv* env, int32_t target_sdk_version, const char* path, jobject class_loader,
    //    const char* caller_location, jstring library_path, bool* needs_native_bridge, char** error_msg);
    using OpenNativeLibrary29_t = void* (*)(JNIEnv*, int32_t, const char*, jobject, const char*, jstring, bool*, char**);
    auto OpenNativeLibrary29 = libnativeloader->GetSymbol<OpenNativeLibrary29_t>("OpenNativeLibrary");
    // Android 8-9 / SDK 26-28
    // void* OpenNativeLibrary(JNIEnv* env,
    //                        int32_t target_sdk_version,
    //                        const char* path,
    //                        jobject class_loader,
    //                        jstring library_path,
    //                        bool* needs_native_bridge,
    //                        std::string* error_msg)
    using OpenNativeLibrary28_t = void* (*)(JNIEnv*, int32_t, const char*, jobject, jstring, bool*, std::string*);
    auto OpenNativeLibrary28 = libnativeloader->GetSymbol<OpenNativeLibrary28_t>(
            "_ZN7android17OpenNativeLibraryEP7_JNIEnviPKcP8_jobjectP8_jstringPbPNSt3__112basic_stringIcNS9_11char_traitsIcEENS9_9allocatorIcEEEE"
    );
    // Android 7.x / SDK 24-25
    // void* OpenNativeLibrary(JNIEnv* env,
    //                        int32_t target_sdk_version,
    //                        const char* path,
    //                        jobject class_loader,
    //                        jstring library_path);
    using OpenNativeLibrary24_t = void* (*)(JNIEnv*, int32_t, const char*, jobject, jstring);
    auto OpenNativeLibrary24 = libnativeloader->GetSymbol<OpenNativeLibrary24_t>(
            "_ZN7android17OpenNativeLibraryEP7_JNIEnviPKcP8_jobjectP8_jstring"
    );
    if (OpenNativeLibrary29 == nullptr && OpenNativeLibrary28 == nullptr && OpenNativeLibrary24 == nullptr) {
        ThrowIfNoPendingException(env, ExceptionNames::kIllegalStateException, "OpenNativeLibrary not found");
        return 0;
    }
    // Android 10-14 / SDK 29-34
    auto NativeBridgeGetTrampoline =
            libnativebridge->GetSymbol<decltype(android::NativeBridgeGetTrampoline)*>("NativeBridgeGetTrampoline");
    if (NativeBridgeGetTrampoline == nullptr) {
        // Android 9 / SDK 28
        NativeBridgeGetTrampoline = libnativebridge->GetSymbol<decltype(android::NativeBridgeGetTrampoline)*>(
                "_ZN7android25NativeBridgeGetTrampolineEPvPKcS2_j"
        );
    }
    if (NativeBridgeGetTrampoline == nullptr) {
        ThrowIfNoPendingException(env, ExceptionNames::kIllegalStateException, "NativeBridgeGetTrampoline not found");
        return 0;
    }
    std::string path = modulePath + "!/" + entryPath;
    bool needsNativeBridge = false;
    std::string errorMsg;
    void* lib;
    if (OpenNativeLibrary29 != nullptr) {
        char* dlextError = nullptr;
        lib = OpenNativeLibrary29(env, 33, path.c_str(),
                                  native_loader, path.c_str(), nullptr,
                                  &needsNativeBridge, &dlextError);
        if (lib == nullptr) {
            errorMsg = dlextError;
        }
    } else if (OpenNativeLibrary28 != nullptr) {
        std::string dlextError;
        lib = OpenNativeLibrary28(env, 28, path.c_str(),
                                  native_loader, nullptr,
                                  &needsNativeBridge, &dlextError);
        if (lib == nullptr) {
            errorMsg = dlextError;
        }
    } else {
        lib = OpenNativeLibrary24(env, 24, path.c_str(), native_loader, nullptr);
        if (lib == nullptr) {
            errorMsg = "OpenNativeLibrary24 failed, no error message available for this API level, dlerror(): ";
            errorMsg += dlerror();
        }
    }
    if (lib == nullptr) {
        ThrowIfNoPendingException(env, ExceptionNames::kRuntimeException,
                                  fmt::format("OpenNativeLibrary failed: {}", errorMsg));
        return 0;
    }
    auto sym = "Java_io_github_qauxv_isolated_soloader_LoadLibraryInvoker_nativeSecondaryNativeLibraryAttachClassLoader";
    void* fn = NativeBridgeGetTrampoline(lib, sym, "JLL", 0);
    if (fn == nullptr) {
        ThrowIfNoPendingException(env, ExceptionNames::kRuntimeException, "NativeBridgeGetTrampoline failed");
        return 0;
    }
    using func_t2 = jint (*)(JNIEnv*, jclass, jobject, jlongArray);
    auto func2 = reinterpret_cast<func_t2>(fn);
    static SecondaryLibraryInitInputInfo sSecondaryLibraryInitInputInfo;
    sSecondaryLibraryInitInputInfo.runtimeIsa = GetCurrentLibraryIsa();
    sSecondaryLibraryInitInputInfo.secondaryIsa = static_cast<LibraryIsa>(isa);
    jlongArray input = env->NewLongArray(2);
    // [input, output]
    jlong inputArray[2] = {reinterpret_cast<jlong>(&sSecondaryLibraryInitInputInfo), 0};
    env->SetLongArrayRegion(input, 0, 2, inputArray);
    auto ret = func2(env, clazz, sModuleMainClassLoader, input);
    // check if any exception occurred
    if (env->ExceptionCheck()) {
        return 0; // with exception
    }
    if (ret < 0) {
        ThrowIfNoPendingException(env, ExceptionNames::kRuntimeException,
                                  fmt::format("nativeSecondaryNativeLibraryAttachClassLoader failed with code {}", ret));
        return 0;
    }
    return reinterpret_cast<jlong>(lib);
}

// private static native void nativePrimaryNativeLibraryAttachClassLoader(@NonNull ClassLoader agent);
// ART will find this function automatically, so we do not need to register it.
extern "C"
JNIEXPORT void JNICALL
Java_io_github_qauxv_isolated_soloader_LoadLibraryInvoker_nativePrimaryNativeLibraryAttachClassLoader(JNIEnv* env, jclass clazz, jobject agent) {
    using namespace qauxv;
    using namespace qauxv::nativeloader;
    if (agent == nullptr) {
        ThrowIfNoPendingException(env, ExceptionNames::kNullPointerException, "agent is null");
        return;
    }
    // check ISA
    auto libart = GetModuleSymbolResolver("libart.so");
    if (libart == nullptr) {
        ThrowIfNoPendingException(env, ExceptionNames::kIllegalStateException, "libart.so not found");
        return;
    }
    auto runtimeIsa = libart->GetLibraryIsa();
    if (runtimeIsa != GetCurrentLibraryIsa()) {
        ThrowIfNoPendingException(env, ExceptionNames::kIllegalStateException,
                                  fmt::format("ISA mismatch: runtime = {}, current = {}", runtimeIsa, GetCurrentLibraryIsa()));
        return;
    }
    sModuleMainClassLoader = env->NewGlobalRef(agent);
    qauxv::nativeloader::CheckClassLoaderNativeNamespaceBridged(env, sModuleMainClassLoader, false);
    if (env->ExceptionCheck()) {
        return; // exception occurred
    }
    // register primary pre-init methods
    if (!sPrimaryPreInitMethodsRegistered) {
        qauxv::jniutil::RegisterJniLateInitMethodsToClassLoader(env, qauxv::jniutil::JniMethodInitType::kPrimaryPreInit, sModuleMainClassLoader);
        sPrimaryPreInitMethodsRegistered = true;
    }
}

extern "C"
JNIEXPORT jlong JNICALL
Java_io_github_qauxv_util_soloader_NativeLoader_nativeGetPrimaryNativeLibraryHandle(JNIEnv* env, jclass clazz) {
    using namespace qauxv;
    static std::string errorMsg;
    static void* handle = nullptr;
    static std::once_flag flag;
    const char* soname = "libqauxv-core0.so";
    std::call_once(flag, [&]() {
        handle = dlopen(soname, RTLD_NOW | RTLD_NOLOAD);
        if (handle == nullptr) {
            errorMsg = dlerror();
        }
    });
    if (handle == nullptr) {
        ThrowIfNoPendingException(env, ExceptionNames::kRuntimeException, fmt::format("dlopen failed: {}", errorMsg));
        return 0;
    }
    return reinterpret_cast<jlong>(handle);
}

//@formatter:off
static JNINativeMethod gPrimaryPreInitMethods[] = {
        {"nativePrimaryNativeLibraryPreInit", "(Ljava/lang/String;Z)V", (void*) Java_io_github_qauxv_util_soloader_NativeLoader_nativePrimaryNativeLibraryPreInit},
        {"nativePrimaryNativeLibraryFullInit", "(ILjava/lang/String;Ljava/lang/String;ILjava/lang/String;JZ)V", (void*) Java_io_github_qauxv_util_soloader_NativeLoader_nativePrimaryNativeLibraryFullInit},
        {"getPrimaryNativeLibraryIsa", "()I", (void*) Java_io_github_qauxv_util_soloader_NativeLoader_getPrimaryNativeLibraryIsa},
        {"nativeLoadSecondaryNativeLibrary", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/ClassLoader;I)J", (void*) Java_io_github_qauxv_util_soloader_NativeLoader_nativeLoadSecondaryNativeLibrary},
        {"nativeGetPrimaryNativeLibraryHandle", "()J", (void*) Java_io_github_qauxv_util_soloader_NativeLoader_nativeGetPrimaryNativeLibraryHandle},
};
//@formatter:on
REGISTER_PRIMARY_PRE_INIT_NATIVE_METHODS("io/github/qauxv/util/soloader/NativeLoader", gPrimaryPreInitMethods);

//@formatter:off
static JNINativeMethod gSecondaryFullInitMethods[] = {
        {"nativeSecondaryNativeLibraryFullInit", "(ILjava/lang/String;Ljava/lang/String;ILjava/lang/String;JZ)V", (void*) Java_io_github_qauxv_util_soloader_NativeLoader_nativeSecondaryNativeLibraryFullInit},
        {"getSecondaryNativeLibraryIsa", "()I", (void*) Java_io_github_qauxv_util_soloader_NativeLoader_getSecondaryNativeLibraryIsa},
};
//@formatter:on
REGISTER_SECONDARY_FULL_INIT_NATIVE_METHODS("io/github/qauxv/util/soloader/NativeLoader", gSecondaryFullInitMethods);

bool qauxv::nativeloader::CheckClassLoaderNativeNamespaceBridged(JNIEnv* env, jobject class_loader, bool is_bridge) {
    if (class_loader == nullptr) {
        ThrowIfNoPendingException(env, ExceptionNames::kNullPointerException, "class_loader is null");
        return false;
    }
    // check if the class loader is an instance of ClassLoader
    jclass kClassLoader = env->FindClass("java/lang/ClassLoader");
    if (!env->IsInstanceOf(class_loader, kClassLoader)) {
        ThrowIfNoPendingException(env, ExceptionNames::kIllegalArgumentException, "class_loader is not an instance of ClassLoader");
        return false;
    }
    auto libnativeloader = GetModuleSymbolResolver("libnativeloader.so");
    if (libnativeloader == nullptr) {
        LOGW("unable to find libnativeloader.so");
        return false;
    }
    // NativeLoaderNamespace* FindNativeLoaderNamespaceByClassLoader(JNIEnv* env, jobject class_loader)
    struct android_namespace_t;
    struct native_bridge_namespace_t;
    struct NativeLoaderNamespace {
        [[nodiscard]] bool IsBridged() const { return raw_.index() == 1; }

        std::string name_;
        std::variant<android_namespace_t*, native_bridge_namespace_t*> raw_;
    };
    using func_t = NativeLoaderNamespace* (*)(JNIEnv*, jobject);
    auto FindNativeLoaderNamespaceByClassLoader =
            libnativeloader->GetSymbol<func_t>("FindNativeLoaderNamespaceByClassLoader");
    if (FindNativeLoaderNamespaceByClassLoader == nullptr) {
        LOGW("unable to find libnativeloader.so!FindNativeLoaderNamespaceByClassLoader");
        return false;
    }
    auto ns = FindNativeLoaderNamespaceByClassLoader(env, class_loader);
    // LOGD("ns = {:p}", (void*) ns);
    if (ns == nullptr) {
        // this means the class loader namespace is not created yet
        return false;
    }
    bool bridged = ns->IsBridged();
    // LOGD("bridged = {}", bridged);
    if (bridged != is_bridge) {
        ThrowIfNoPendingException(env, ExceptionNames::kIllegalArgumentException,
                                  fmt::format("class_loader namespace bridged mismatch: expected {}, actual {}", is_bridge, bridged));
        return false;
    }
    // everything is fine
    return true;
}

// this function is called by dlsym, so we do not need to register it.
extern "C"
JNIEXPORT jint JNICALL
Java_io_github_qauxv_isolated_soloader_LoadLibraryInvoker_nativeSecondaryNativeLibraryAttachClassLoader
        (JNIEnv* env, jclass clazz, jobject agent, jlongArray init_info_arr) {
    using namespace qauxv;
    using namespace qauxv::nativeloader;
    using namespace qauxv::jniutil;
    LOGD("nativeSecondaryNativeLibraryAttachClassLoader: ISA = {}", GetCurrentLibraryIsa());
    // check class loader
    // check if the class loader is an instance of ClassLoader
    jclass kClassLoader = env->FindClass("java/lang/ClassLoader");
    if (!env->IsInstanceOf(agent, kClassLoader)) {
        ThrowIfNoPendingException(env, ExceptionNames::kIllegalArgumentException, "class_loader is not an instance of ClassLoader");
        return JNI_ERR;
    }
    sModuleMainClassLoader = env->NewGlobalRef(agent);
    // register secondary library full init methods, but we don't perform full init here
    if (!sSecondaryFullInitMethodsRegistered) {
        qauxv::jniutil::RegisterJniLateInitMethodsToClassLoader(env, qauxv::jniutil::JniMethodInitType::kSecondaryFullInit, sModuleMainClassLoader);
        sSecondaryFullInitMethodsRegistered = true;
    }
    return JNI_OK;
}

__attribute__((visibility("protected")))
extern "C" jobject GetModuleMainClassLoader() {
    return sModuleMainClassLoader;
}
