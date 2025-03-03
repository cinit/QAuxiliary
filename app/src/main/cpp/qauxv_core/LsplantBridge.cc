//
// Created by sulfate on 2024-08-03.
//

#include "LsplantBridge.h"

#include <string>
#include <string_view>
#include <vector>

#include <jni.h>
#include <android/log.h>

#include <fmt/format.h>
#include "natives_utils.h"
#include "utils/MemoryDexLoader.h"
#include "qauxv_core/jni_method_registry.h"
#include "qauxv_core/NativeCoreBridge.h"
#include "dobby.h"
#include "utils/art_symbol_resolver.h"
#include "lsplant.hpp"

#undef LOGD

#include <unistd.h>
#include <stdlib.h>
#include "utils/Log.h"
#include "utils/ConfigManager.h"
#include "qauxv_core/HostInfo.h"

static bool sLsplantInitSuccess = false;

namespace qauxv {

bool InitLSPlantImpl(JNIEnv* env) {
    const auto initProc = [env] {
        ::lsplant::InitInfo sLSPlantInitInfo = {
                .inline_hooker = [](auto t, auto r) {
                    void* backup = nullptr;
                    return qauxv::CreateInlineHook(t, r, &backup) == RT_SUCCESS ? backup : nullptr;
                },
                .inline_unhooker = [](auto t) {
                    return qauxv::DestroyInlineHook(t) == RT_SUCCESS;
                },
                .art_symbol_resolver = [](auto symbol) {
                    auto ret = GetLibArtSymbol(symbol);
                    if (ret == nullptr) {
                        LOGD("symbol '{}' not found", symbol);
                    }
                    return ret;
                },
                .art_symbol_prefix_resolver = [](auto symbol) {
                    auto ret = GetLibArtSymbolPrefix(symbol);
                    if (ret == nullptr) {
                        LOGD("symbol prefix '{}' not found", symbol);
                    }
                    return ret;
                }
        };
        return ::lsplant::Init(env, sLSPlantInitInfo);
    };
    static bool sLSPlantInitializeResult = initProc();
    HookArtProfileSaver();
    return sLSPlantInitializeResult;
}

bool ShouldDisableArtProfileSaver() {
    auto dataDir = qauxv::HostInfo::GetDataDir();
    CHECK(!dataDir.empty());
    auto path = fmt::format("{}/files/qa_misc/KEY_DISABLE_ART_PROFILE_SAVER", dataDir);
    return access(path.c_str(), F_OK) == 0;
}

void HookArtProfileSaver() {
    if (!InitLibArtElfView()) {
        LOGE("HookArtProfileSaver: InitLibArtElfView fail");
        return;
    }
    if (ShouldDisableArtProfileSaver()) {
        static bool hooked = false;
        if (hooked) {
            return;
        }
        hooked = true;
        void* fn2 = GetLibArtSymbol("_ZN3art12ProfileSaver20ProcessProfilingInfoEbPt");
        LOGD("_ZN3art12ProfileSaver20ProcessProfilingInfoEbPt={}", fn2);
        {
            static void* fn2backup = nullptr;
            int res = 0;
            if (fn2 != nullptr) {
                res = CreateInlineHook(fn2, reinterpret_cast<void*>(+[](void* thiz, bool, uint16_t*) {
                    LOGD("skip _ZN3art12ProfileSaver20ProcessProfilingInfoEbPt");
                }), &fn2backup);
                if (res != 0) {
                    LOGE("hook _ZN3art12ProfileSaver20ProcessProfilingInfoEbPt {} fail, rc {}", fn2, res);
                }
            }
        }
        void* fn3 = GetLibArtSymbol("_ZN3art12ProfileSaver20ProcessProfilingInfoEbbPt");
        LOGD("_ZN3art12ProfileSaver20ProcessProfilingInfoEbbPt={}", fn3);
        {
            static void* fn3backup = nullptr;
            int res = 0;
            if (fn3 != nullptr) {
                res = CreateInlineHook(fn3, reinterpret_cast<void*>(+[](void* thiz, bool, bool, uint16_t*) {
                    LOGD("skip _ZN3art12ProfileSaver20ProcessProfilingInfoEbbPt");
                }), &fn3backup);
                if (res != 0) {
                    LOGE("hook _ZN3art12ProfileSaver20ProcessProfilingInfoEbbPt {} fail, rc {}", fn3, res);
                }
            }
        }
    }
}

}

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
