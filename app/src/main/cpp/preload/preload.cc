//
// Created by sulfate on 2024-08-16.
//
#include <cstdint>
#include <vector>
#include <array>
#include <string>
#include <string_view>

#include <fcntl.h>
#include <dlfcn.h>
#include <android/dlext.h>
#include <jni.h>

static std::string JstringToString(JNIEnv* env, jstring jstr) {
    if (jstr == nullptr) {
        return {};
    }
    const char* chars = env->GetStringUTFChars(jstr, nullptr);
    if (chars == nullptr) {
        env->ExceptionClear();
        return {};
    }
    std::string str(chars);
    env->ReleaseStringUTFChars(jstr, chars);
    return str;
}

static void ThrowIfNoPendingException(JNIEnv* env, const char* klass, std::string_view msg) {
    if (env->ExceptionCheck()) {
        return;
    }
    // in case string_view is not null-terminated
    env->ThrowNew(env->FindClass(klass), std::string(msg).c_str());
}

constexpr auto kRuntimeException = "java/lang/RuntimeException";
constexpr auto kIllegalArgumentException = "java/lang/IllegalArgumentException";
constexpr auto kIllegalStateException = "java/lang/IllegalStateException";
constexpr auto kNullPointerException = "java/lang/NullPointerException";

extern "C"
JNIEXPORT jint JNI_OnLoad([[maybe_unused]] JavaVM* vm, [[maybe_unused]] void* reserved) {
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_io_github_qauxv_isolated_soloader_LoadLibraryInvoker_nativeCallAndroidDlopenExt
        (JNIEnv* env, [[maybe_unused]] jclass clazz, jint fd_j, jstring name_j, jlong offset_j) {
    auto name = JstringToString(env, name_j);
    if (name.empty()) {
        ThrowIfNoPendingException(env, kIllegalArgumentException, "empty library name");
        return 0;
    }
    int fd = fd_j;
    auto offset = static_cast<off64_t>(offset_j);
    if (fd == 0) {
        ThrowIfNoPendingException(env, kIllegalArgumentException, "error-prone argument: fd = 0");
        return 0;
    }
    bool use_fd = fd >= 0;
    void* handle;
    if (use_fd) {
        android_dlextinfo ext = {};
        ext.flags = ANDROID_DLEXT_USE_LIBRARY_FD | ANDROID_DLEXT_USE_LIBRARY_FD_OFFSET;
        ext.library_fd = fd;
        ext.library_fd_offset = offset;
        handle = android_dlopen_ext(name.c_str(), RTLD_NOW, &ext);
    } else {
        handle = dlopen(name.c_str(), RTLD_NOW);
    }
    if (handle == nullptr) {
        std::string msg = "dlopen failed: ";
        msg += dlerror();
        ThrowIfNoPendingException(env, kRuntimeException, msg);
        return 0;
    } else {
        return reinterpret_cast<jlong>(handle);
    }
}
