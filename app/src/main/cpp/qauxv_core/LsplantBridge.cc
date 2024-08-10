//
// Created by sulfate on 2024-08-03.
//

#include <string>
#include <string_view>
#include <vector>

#include <jni.h>
#include <android/log.h>

#include <fmt/format.h>
#include "natives_utils.h"
#include "utils/MemoryDexLoader.h"
#include "qauxv_core/jni_method_registry.h"

#include "utils/art_symbol_resolver.h"
#include "lsplant.hpp"

static bool sLsplantInitSuccess = false;

extern "C"
JNIEXPORT void JNICALL
Java_io_github_qauxv_util_hookimpl_lsplant_LsplantBridge_nativeInitializeLsplant(JNIEnv* env, jclass clazz) {
    if (sLsplantInitSuccess) {
        return;
    }
    using namespace qauxv;
    if (!InitLibArtElfView()) {
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "libart symbol resolver init failed");
        return;
    }
    if (!InitLSPlantImpl(env)) {
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "lsplant init failed");
        return;
    }
    sLsplantInitSuccess = true;
}

// static native Method nativeHookMethod(Member target, Member callback, Object context)
extern "C"
JNIEXPORT jobject JNICALL
Java_io_github_qauxv_util_hookimpl_lsplant_LsplantBridge_nativeHookMethod(JNIEnv* env, jclass clazz, jobject target, jobject callback, jobject context) {
    if (!sLsplantInitSuccess) {
        env->ThrowNew(env->FindClass("java/lang/IllegalAccessException"), "lsplant not initialized");
        return nullptr;
    }
    return ::lsplant::Hook(env, target, context, callback);
}

// static native boolean nativeIsMethodHooked(Member target)
extern "C"
JNIEXPORT jboolean JNICALL
Java_io_github_qauxv_util_hookimpl_lsplant_LsplantBridge_nativeIsMethodHooked(JNIEnv* env, jclass clazz, jobject target) {
    if (!sLsplantInitSuccess) {
        env->ThrowNew(env->FindClass("java/lang/IllegalAccessException"), "lsplant not initialized");
        return false;
    }
    return ::lsplant::IsHooked(env, target);
}

// static native boolean nativeUnhookMethod(Member target)
extern "C"
JNIEXPORT jboolean JNICALL
Java_io_github_qauxv_util_hookimpl_lsplant_LsplantBridge_nativeUnhookMethod(JNIEnv* env, jclass clazz, jobject target) {
    if (!sLsplantInitSuccess) {
        env->ThrowNew(env->FindClass("java/lang/IllegalAccessException"), "lsplant not initialized");
        return false;
    }
    return ::lsplant::UnHook(env, target);
}

// static native boolean nativeDeoptimizeMethod(Member target)
extern "C"
JNIEXPORT jboolean JNICALL
Java_io_github_qauxv_util_hookimpl_lsplant_LsplantBridge_nativeDeoptimizeMethod(JNIEnv* env, jclass clazz, jobject target) {
    if (!sLsplantInitSuccess) {
        env->ThrowNew(env->FindClass("java/lang/IllegalAccessException"), "lsplant not initialized");
        return false;
    }
    return ::lsplant::Deoptimize(env, target);
}

// @formatter:off
static JNINativeMethod gMethods[] = {
    {"nativeInitializeLsplant", "()V", reinterpret_cast<void*>(Java_io_github_qauxv_util_hookimpl_lsplant_LsplantBridge_nativeInitializeLsplant)},
    {"nativeHookMethod", "(Ljava/lang/reflect/Member;Ljava/lang/reflect/Member;Ljava/lang/Object;)Ljava/lang/reflect/Method;", reinterpret_cast<void*>(Java_io_github_qauxv_util_hookimpl_lsplant_LsplantBridge_nativeHookMethod)},
    {"nativeIsMethodHooked", "(Ljava/lang/reflect/Member;)Z", reinterpret_cast<void*>(Java_io_github_qauxv_util_hookimpl_lsplant_LsplantBridge_nativeIsMethodHooked)},
    {"nativeUnhookMethod", "(Ljava/lang/reflect/Member;)Z", reinterpret_cast<void*>(Java_io_github_qauxv_util_hookimpl_lsplant_LsplantBridge_nativeUnhookMethod)},
    {"nativeDeoptimizeMethod", "(Ljava/lang/reflect/Member;)Z", reinterpret_cast<void*>(Java_io_github_qauxv_util_hookimpl_lsplant_LsplantBridge_nativeDeoptimizeMethod)},
};
// @formatter:on

REGISTER_PRIMARY_PRE_INIT_NATIVE_METHODS("io/github/qauxv/util/hookimpl/lsplant/LsplantBridge", gMethods);
