//
// Created by sulfate on 2024-08-10.
//

#include "native_loader.h"

#include <array>
#include <cstdint>
#include <vector>

#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>

#include <fmt/format.h>
#include <jni.h>

#include "NativeCoreBridge.h"
#include "HostInfo.h"
#include "jni_method_registry.h"
#include "utils/JniUtils.h"
#include "utils/Log.h"

#include "MMKV.h"

using qauxv::JstringToString;

static jobject sModuleMainClassLoader = nullptr;

static bool sPrimaryPreInitMethodsRegistered = false;
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
    sPrimaryPreInitDone = true;
}

void DoNativeLibraryFullInitializeFormJni(JNIEnv* env,
                                          jclass clazz,
                                          jint init_mode,
                                          jstring data_dir_j,
                                          jstring package_name_j,
                                          [[maybe_unused]] jint current_sdk_level,
                                          jstring version_name_j,
                                          jlong long_version_code) {
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
    HostInfo::InitHostInfo(vm, dataDir, packageName, versionName, versionCode);
    if (!IsNativeHookApiInitialized()) {
        InitializeNativeHookApi(isPrimary);
    }
    if (isPrimary) {
        // is pre-init done?
        if (!sPrimaryPreInitMethodsRegistered) {
            qauxv::jniutil::RegisterJniLateInitMethodsToClassLoader(env, qauxv::jniutil::JniMethodInitType::kPrimaryPreInit, sModuleMainClassLoader);
            sPrimaryPreInitMethodsRegistered = true;
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
        MMKV::initializeMMKV(mmkvRootDir, MMKVLogLevel::MMKVLogWarning);
    }
}


//  private static native void nativePrimaryNativeLibraryFullInit(int initMode, @NonNull String dataDir,
//            String packageName, int currentSdkLevel, String versionName, long longVersionCode);
extern "C"
JNIEXPORT void JNICALL
Java_io_github_qauxv_util_soloader_NativeLoader_nativePrimaryNativeLibraryFullInit(JNIEnv* env,
                                                                                   jclass clazz,
                                                                                   jint init_mode,
                                                                                   jstring data_dir,
                                                                                   jstring package_name,
                                                                                   [[maybe_unused]] jint current_sdk_level,
                                                                                   jstring version_name,
                                                                                   jlong long_version_code) {
    DoNativeLibraryFullInitializeFormJni(env, clazz, init_mode, data_dir, package_name, current_sdk_level, version_name, long_version_code);
}


// private static native void nativeSecondaryNativeLibraryFullInit(int initMode, @NonNull String dataDir,
//            String packageName, int currentSdkLevel, String versionName, long longVersionCode);
extern "C"
JNIEXPORT void JNICALL
Java_io_github_qauxv_util_soloader_NativeLoader_nativeSecondaryNativeLibraryFullInit(JNIEnv* env,
                                                                                     jclass clazz,
                                                                                     jint init_mode,
                                                                                     jstring data_dir,
                                                                                     jstring package_name,
                                                                                     [[maybe_unused]] jint current_sdk_level,
                                                                                     jstring version_name,
                                                                                     jlong long_version_code) {
    DoNativeLibraryFullInitializeFormJni(env, clazz, init_mode, data_dir, package_name, current_sdk_level, version_name, long_version_code);
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

// private static native void nativeLoadSecondaryNativeLibrary(@NonNull String modulePath, @NonNull String entryPath, int isa);
extern "C"
JNIEXPORT void JNICALL
Java_io_github_qauxv_util_soloader_NativeLoader_nativeLoadSecondaryNativeLibrary(JNIEnv* env, jclass clazz, jstring module_path, jstring entry_path, jint isa) {
    LOGW("nativeLoadSecondaryNativeLibrary not implemented");
    // TODO: implement nativeLoadSecondaryNativeLibrary()
}

// private static native void nativePrimaryNativeLibraryAttachClassLoader(@NonNull ClassLoader agent);
// ART will find this function automatically, so we do not need to register it.
extern "C"
JNIEXPORT void JNICALL
Java_io_github_qauxv_isolated_soloader_LoadLibraryInvoker_nativePrimaryNativeLibraryAttachClassLoader(JNIEnv* env, jclass clazz, jobject agent) {
    if (agent == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "agent is null");
        return;
    }
    sModuleMainClassLoader = env->NewGlobalRef(agent);
    qauxv::nativeloader::SetClassLoaderNativeNamespaceNonBridged(env, sModuleMainClassLoader);
    // register primary pre-init methods
    if (!sPrimaryPreInitMethodsRegistered) {
        qauxv::jniutil::RegisterJniLateInitMethodsToClassLoader(env, qauxv::jniutil::JniMethodInitType::kPrimaryPreInit, sModuleMainClassLoader);
        sPrimaryPreInitMethodsRegistered = true;
    }
}

//@formatter:off
static JNINativeMethod gPrimaryPreInitMethods[] = {
        {"nativePrimaryNativeLibraryPreInit", "(Ljava/lang/String;Z)V", (void*) Java_io_github_qauxv_util_soloader_NativeLoader_nativePrimaryNativeLibraryPreInit},
        {"nativePrimaryNativeLibraryFullInit", "(ILjava/lang/String;Ljava/lang/String;ILjava/lang/String;J)V", (void*) Java_io_github_qauxv_util_soloader_NativeLoader_nativePrimaryNativeLibraryFullInit},
        {"getPrimaryNativeLibraryIsa", "()I", (void*) Java_io_github_qauxv_util_soloader_NativeLoader_getPrimaryNativeLibraryIsa},
        {"nativeLoadSecondaryNativeLibrary", "(Ljava/lang/String;Ljava/lang/String;I)V", (void*) Java_io_github_qauxv_util_soloader_NativeLoader_nativeLoadSecondaryNativeLibrary},
};
//@formatter:on
REGISTER_PRIMARY_PRE_INIT_NATIVE_METHODS("io/github/qauxv/util/soloader/NativeLoader", gPrimaryPreInitMethods);

//@formatter:off
static JNINativeMethod gSecondaryFullInitMethods[] = {
        {"nativeSecondaryNativeLibraryFullInit", "(ILjava/lang/String;Ljava/lang/String;ILjava/lang/String;J)V", (void*) Java_io_github_qauxv_util_soloader_NativeLoader_nativeSecondaryNativeLibraryFullInit},
        {"getSecondaryNativeLibraryIsa", "()I", (void*) Java_io_github_qauxv_util_soloader_NativeLoader_getSecondaryNativeLibraryIsa},
};
//@formatter:on
REGISTER_SECONDARY_FULL_INIT_NATIVE_METHODS("io/github/qauxv/util/soloader/NativeLoader", gSecondaryFullInitMethods);

bool qauxv::nativeloader::SetClassLoaderNativeNamespaceNonBridged(JNIEnv* env, jobject class_loader) {
    // TODO: implement SetClassLoaderNativeNamespaceNonBridged()
    LOGW("SetClassLoaderNativeNamespaceNonBridged not implemented");
    return true;
}
