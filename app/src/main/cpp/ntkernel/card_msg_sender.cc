//
// Created by sulfate on 2024-08-10.
//

#include <jni.h>

#include "qauxv_core/jni_method_registry.h"

jboolean handleSendCardMsg(JNIEnv* env, jclass clazz, jobject rt, jobject session, jstring msg) {
    if (rt == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/NullPointerException"), "appInterface == null");
        return false;
    }
    if (session == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/NullPointerException"), "session == null");
        return false;
    }
    if (msg == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/NullPointerException"), "msg == null");
        return false;
    }
    if (env->GetStringLength(msg) < 3)return false;

    jclass cardMsgListClass = env->FindClass("me/singleneuron/util/KotlinUtilsKt");
    jmethodID getInstance = env->GetStaticMethodID(cardMsgListClass, "checkCardMsg", "(Ljava/lang/String;)Lme/singleneuron/data/CardMsgCheckResult;");
    jobject result = env->CallStaticObjectMethod(cardMsgListClass, getInstance, msg);
    jclass cardMsgCheckResultClass = env->FindClass("me/singleneuron/data/CardMsgCheckResult");
    jmethodID toString = env->GetMethodID(cardMsgCheckResultClass, "toString", "()Ljava/lang/String;");
    jmethodID getAccepted = env->GetMethodID(cardMsgCheckResultClass, "getAccept", "()Z");
    auto resultString = (jstring) env->CallObjectMethod(result, toString);
    bool boolean = env->CallBooleanMethod(result, getAccepted);
    if (!boolean) {
        jmethodID getReason = env->GetMethodID(cardMsgCheckResultClass,
                                               "getReason",
                                               "()Ljava/lang/String;");
        auto reason = (jstring) env->CallObjectMethod(result, getReason);
        jclass cl_Toasts = env->FindClass("io/github/qauxv/util/Toasts");
        jmethodID showErrorToastAnywhere = env->GetStaticMethodID(
                cl_Toasts, "error",
                "(Landroid/content/Context;Ljava/lang/CharSequence;)V");
        env->CallStaticVoidMethod(cl_Toasts, showErrorToastAnywhere,
                                  (jobject) nullptr, reason);
        return true;
    }

    jchar format;
    env->GetStringRegion(msg, 0, 1, &format);
    if (format == '<') {
        jclass AbsStructMsg = env->FindClass("com/tencent/mobileqq/structmsg/AbsStructMsg");
        if (!AbsStructMsg)return false;
        jclass DexKit = env->FindClass("io/github/qauxv/util/dexkit/DexKit");
        jmethodID cid = env->GetStaticMethodID(DexKit, "loadClassFromCache", "(Lio/github/qauxv/util/dexkit/DexKitTarget;)Ljava/lang/Class;");
        auto TestStructMsg = (jclass) env->CallStaticObjectMethod(DexKit, cid, env->GetStaticObjectField(
                env->FindClass("io/github/qauxv/util/dexkit/CTestStructMsg"), env->GetStaticFieldID(
                        env->FindClass("io/github/qauxv/util/dexkit/CTestStructMsg"), "INSTANCE",
                        "Lio/github/qauxv/util/dexkit/CTestStructMsg;")));
        if (TestStructMsg == nullptr) {
            env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "class TestStructMsg not found");
            return false;
        }
        jclass cl_Utils = env->FindClass("cc/ioctl/util/Reflex");
        cid = env->GetStaticMethodID(cl_Utils, "invokeStaticAny",
                                     "(Ljava/lang/Class;[Ljava/lang/Object;)Ljava/lang/Object;");
        jobjectArray va = env->NewObjectArray(3, env->FindClass("java/lang/Object"), nullptr);
        env->SetObjectArrayElement(va, 0, msg);
        env->SetObjectArrayElement(va, 1, env->FindClass("java/lang/String"));
        env->SetObjectArrayElement(va, 2, AbsStructMsg);
        jobject structMsg = env->CallStaticObjectMethod(cl_Utils, cid, TestStructMsg, va);
        if (env->ExceptionCheck())return false;
        if (structMsg == nullptr)return false;
        // check cast: expected AbsStructMsg
        if (!env->IsInstanceOf(structMsg, AbsStructMsg)) {
            env->ThrowNew(env->FindClass("java/lang/ClassCastException"), "expected AbsStructMsg");
            return false;
        }
        jclass ChatActivityFacade = env->FindClass("io/github/qauxv/bridge/ChatActivityFacade");
        jmethodID sendAbsStructMsg = env->GetStaticMethodID(ChatActivityFacade, "sendAbsStructMsg",
                                                            "(Lmqq/app/AppRuntime;Landroid/os/Parcelable;Ljava/io/Externalizable;)V");
        env->CallStaticVoidMethod(ChatActivityFacade, sendAbsStructMsg, rt, session, structMsg);
        return !env->ExceptionCheck();
    } else if (format == '{') {
        jclass c_ArkAppMessage = env->FindClass("com/tencent/mobileqq/data/ArkAppMessage");
        if (c_ArkAppMessage == nullptr)return false;
        jmethodID cid = env->GetMethodID(c_ArkAppMessage, "<init>", "()V");
        if (env->ExceptionCheck())return false;
        jobject arkMsg = env->NewObject(c_ArkAppMessage, cid);
        if (arkMsg == nullptr)return false;
        jmethodID fromAppXml = env->GetMethodID(c_ArkAppMessage, "fromAppXml",
                                                "(Ljava/lang/String;)Z");
        if (env->ExceptionCheck())return false;
        if (!env->CallBooleanMethod(arkMsg, fromAppXml, msg)) {
            return false;
        }
        jclass ChatActivityFacade = env->FindClass("io/github/qauxv/bridge/ChatActivityFacade");
        jmethodID sendArkAppMessage = env->GetStaticMethodID(ChatActivityFacade,
                                                             "sendArkAppMessage",
                                                             "(Lmqq/app/AppRuntime;Landroid/os/Parcelable;Ljava/lang/Object;)Z");
        return env->CallStaticBooleanMethod(ChatActivityFacade, sendArkAppMessage,
                                            rt, session, arkMsg);
    } else {
        return false;
    }
}

static JNINativeMethod gPrimaryPreInitMethods[] = {
        {"ntSendCardMsg", "(Lmqq/app/AppRuntime;Landroid/os/Parcelable;Ljava/lang/String;)Z", reinterpret_cast<void*>(handleSendCardMsg)}
};

REGISTER_PRIMARY_PRE_INIT_NATIVE_METHODS("cc/ioctl/hook/experimental/CardMsgSender", gPrimaryPreInitMethods);
