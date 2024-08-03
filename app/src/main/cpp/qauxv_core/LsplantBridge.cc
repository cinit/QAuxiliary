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

extern "C"
JNIEXPORT jobject JNICALL
Java_io_github_qauxv_util_hookimpl_lsplant_LsplantBridge_nativeHookMethod(JNIEnv* env, jclass clazz, jobject target, jobject callback, jobject context) {
    if (!sLsplantInitSuccess) {
        env->ThrowNew(env->FindClass("java/lang/IllegalAccessException"), "lsplant not initialized");
        return nullptr;
    }
    return ::lsplant::Hook(env, target, context, callback);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_io_github_qauxv_util_hookimpl_lsplant_LsplantBridge_nativeIsMethodHooked(JNIEnv* env, jclass clazz, jobject target) {
    if (!sLsplantInitSuccess) {
        env->ThrowNew(env->FindClass("java/lang/IllegalAccessException"), "lsplant not initialized");
        return false;
    }
    return ::lsplant::IsHooked(env, target);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_io_github_qauxv_util_hookimpl_lsplant_LsplantBridge_nativeUnhookMethod(JNIEnv* env, jclass clazz, jobject target) {
    if (!sLsplantInitSuccess) {
        env->ThrowNew(env->FindClass("java/lang/IllegalAccessException"), "lsplant not initialized");
        return false;
    }
    return ::lsplant::UnHook(env, target);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_io_github_qauxv_util_hookimpl_lsplant_LsplantBridge_nativeDeoptimizeMethod(JNIEnv* env, jclass clazz, jobject target) {
    if (!sLsplantInitSuccess) {
        env->ThrowNew(env->FindClass("java/lang/IllegalAccessException"), "lsplant not initialized");
        return false;
    }
    return ::lsplant::Deoptimize(env, target);
}
