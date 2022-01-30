#include <cerrno>
#include <dlfcn.h>
#include <jni.h>
#include <memory.h>
#include <malloc.h>
#include <unistd.h>
#include <sys/mman.h>
#include "natives_utils.h"
#include <android/log.h>

#include "Natives.h"
#include "NativeMainHook.h"

/*
 * Class:     io_github_qauxv_util_Natives
 * Method:    mwrite
 * Signature: (JI[BI)V
 */
EXPORT void Java_io_github_qauxv_util_Natives_mwrite
        (JNIEnv *env, jclass clz, jlong ptr, jint len, jbyteArray arr, jint offset) {
    jbyte *bufptr = (jbyte *) ptr;
    int blen = env->GetArrayLength(arr);
    if (offset < 0) {
        env->ThrowNew(env->FindClass("java/lang/IndexOutOfBoundsException"), "offset < 0");
        return;
    }
    if (len < 0) {
        env->ThrowNew(env->FindClass("java/lang/IndexOutOfBoundsException"), "len < 0");
        return;
    }
    if (blen - len < 0) {
        env->ThrowNew(env->FindClass("java/lang/IndexOutOfBoundsException"), "length < offset");
        return;
    }
    env->GetByteArrayRegion(arr, offset, len, bufptr);
}

/*
 * Class:     io_github_qauxv_util_Natives
 * Method:    mread
 * Signature: (JI[BI)V
 */
EXPORT void Java_io_github_qauxv_util_Natives_mread
        (JNIEnv *env, jclass, jlong ptr, jint len, jbyteArray arr, jint offset) {
    jbyte *bufptr = (jbyte *) ptr;
    int blen = env->GetArrayLength(arr);
    if (offset < 0) {
        env->ThrowNew(env->FindClass("java/lang/IndexOutOfBoundsException"), "offset < 0");
        return;
    }
    if (len < 0) {
        env->ThrowNew(env->FindClass("java/lang/IndexOutOfBoundsException"), "len < 0");
        return;
    }
    if (blen - len < 0) {
        env->ThrowNew(env->FindClass("java/lang/IndexOutOfBoundsException"), "length < offset");
        return;
    }
    env->SetByteArrayRegion(arr, offset, len, bufptr);
}

/*
 * Class:     io_github_qauxv_util_Natives
 * Method:    malloc
 * Signature: (I)J
 */
EXPORT jlong Java_io_github_qauxv_util_Natives_malloc
        (JNIEnv *env, jclass, jint len) {
    jlong ptr = (jlong) malloc(len);
    return ptr;
}

/*
 * Class:     io_github_qauxv_util_Natives
 * Method:    free
 * Signature: (J)V
 */
EXPORT void Java_io_github_qauxv_util_Natives_free(JNIEnv *, jclass, jlong ptr) {
    if (ptr != 0L) {
        free((void *) ptr);
    }
}

/*
 * Class:     io_github_qauxv_util_Natives
 * Method:    memcpy
 * Signature: (JJI)V
 */
EXPORT void
Java_io_github_qauxv_util_Natives_memcpy(JNIEnv *, jclass, jlong dest, jlong src, jint n) {
    memcpy((void *) dest, (void *) src, n);
}

/*
 * Class:     io_github_qauxv_util_Natives
 * Method:    memset
 * Signature: (JII)V
 */
EXPORT void Java_io_github_qauxv_util_Natives_memset
        (JNIEnv *, jclass, jlong addr, jint c, jint num) {
    memset((void *) addr, c, num);
}

/*
 * Class:     io_github_qauxv_util_Natives
 * Method:    mprotect
 * Signature: (JII)I
 */
EXPORT jint
Java_io_github_qauxv_util_Natives_mprotect(JNIEnv *, jclass, jlong addr, jint len, jint prot) {
    if (mprotect((void *) addr, len, prot)) {
        return errno;
    } else {
        return 0;
    }
}

/*
 * Class:     io_github_qauxv_util_Natives
 * Method:    dlsym
 * Signature: (JLjava/lang/String;)J
 */
EXPORT jlong
Java_io_github_qauxv_util_Natives_dlsym(JNIEnv *env, jclass, jlong h, jstring name) {
    const char *p;
    jboolean copy;
    p = env->GetStringUTFChars(name, &copy);
    if (!p)return 0;
    void *ret = dlsym((void *) h, p);
    env->ReleaseStringUTFChars(name, p);
    return (jlong) ret;
}

/*
 * Class:     io_github_qauxv_util_Natives
 * Method:    dlopen
 * Signature: (Ljava/lang/String;I)J
 */
EXPORT jlong
Java_io_github_qauxv_util_Natives_dlopen(JNIEnv *env, jclass, jstring name, jint flag) {
    const char *p;
    jboolean copy;
    p = env->GetStringUTFChars(name, &copy);
    if (!p)return 0;
    void *ret = dlopen(p, flag);
    env->ReleaseStringUTFChars(name, p);
    return (jlong) ret;
}

/*
 * Class:     io_github_qauxv_util_Natives
 * Method:    dlclose
 * Signature: (J)I
 */
EXPORT jint Java_io_github_qauxv_util_Natives_dlclose(JNIEnv *, jclass, jlong h) {
    return (jint) dlclose((void *) h);
}

/*
 * Class:     io_github_qauxv_util_Natives
 * Method:    dlerror
 * Signature: ()Ljava/lang/String;
 */
EXPORT jstring Java_io_github_qauxv_util_Natives_dlerror
        (JNIEnv *env, jclass) {
    const char *str = dlerror();
    if (str == nullptr) {
        return nullptr;
    }
    return env->NewStringUTF(str);
}

/*
 * Class:     io_github_qauxv_util_Natives
 * Method:    sizeofptr
 * Signature: ()I
 */
EXPORT jint Java_io_github_qauxv_util_Natives_sizeofptr(JNIEnv *, jclass) {
    return sizeof(void *);
}

/*
 * Class:     io_github_qauxv_util_Natives
 * Method:    getpagesize
 * Signature: ()I
 */
EXPORT jint Java_io_github_qauxv_util_Natives_getpagesize(JNIEnv *, jclass) {
    return getpagesize();
}

/*
 * Class:     io_github_qauxv_util_Natives
 * Method:    call
 * Signature: (J)J
 */
EXPORT jlong Java_io_github_qauxv_util_Natives_call__J(JNIEnv *env, jclass, jlong addr) {
    void *(*fun)();
    fun = (void *(*)()) (addr);
    if (fun == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/NullPointerException"),
                      "address == null");
        return 0L;
    }
    void *ret = fun();
    return (jlong) ret;
}

/*
 * Class:     io_github_qauxv_util_Natives
 * Method:    call
 * Signature: (JJ)J
 */
EXPORT jlong Java_io_github_qauxv_util_Natives_call__JJ
        (JNIEnv *env, jclass, jlong addr, jlong arg) {
    void *(*fun)(void *);
    fun = (void *(*)(void *)) (addr);
    if (fun == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/NullPointerException"),
                      "address == null");
        return 0L;
    }
    void *ret = fun((void *) arg);
    return (jlong) ret;
}

uint32_t update_adler32(unsigned adler, const uint8_t *data, uint32_t len) {
    unsigned s1 = adler & 0xffffu;
    unsigned s2 = (adler >> 16u) & 0xffffu;

    while (len > 0) {
        /*at least 5550 sums can be done before the sums overflow, saving a lot of module divisions*/
        unsigned amount = len > 5550 ? 5550 : len;
        len -= amount;
        while (amount > 0) {
            s1 += (*data++);
            s2 += s1;
            --amount;
        }
        s1 %= 65521;
        s2 %= 65521;
    }
    return (s2 << 16u) | s1;
}

uint8_t *extractPayload(uint8_t *dex, int dexLength, int *outLength) {
    int chunkROff = readLe32(dex, dexLength - 4);
    if (chunkROff > dexLength) {
        *outLength = 0;
        return nullptr;
    }
    int base = dexLength - chunkROff;
    int size = readLe32(dex, base);
    if (size > dexLength) {
        *outLength = 0;
        return nullptr;
    }
    uint32_t flags = readLe32(dex, base + 4);
    uint32_t a32_got = readLe32(dex, base + 8);
    uint32_t extra = readLe32(dex, base + 12);
    if (flags != 0) {
        *outLength = 0;
        return nullptr;
    }
    uint32_t key = extra & 0xFFu;
    uint8_t *dat = (uint8_t *) malloc(size);
    if (key == 0) {
        memcpy(dat, dex + base + 16, size);
    } else {
        for (int i = 0; i < size; i++) {
            dat[i] = (uint8_t) (key ^ dex[base + 16 + i]);
        }
    }
    uint32_t a32 = update_adler32(1, dat, size);
    if (a32 != a32_got) {
        free(dat);
        *outLength = 0;
        return nullptr;
    }
    return dat;
}

static int64_t sBuildTimestamp = -2;

static const int DEX_MAX_SIZE = 12 * 1024 * 1024;


jboolean handleSendBatchMessages(JNIEnv *env, jclass clazz, jobject rt,
                                 jobject ctx, jstring msg, jintArray _type, jlongArray _uin) {
    if (rt == nullptr || ctx == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/NullPointerException"), "appInterface/ctx == null");
        return false;
    }
    if (msg == nullptr || _type == nullptr || _uin == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/NullPointerException"), "msg/uin == null");
        return false;
    }
    bool success = true;
    int len = min(env->GetArrayLength(_type), env->GetArrayLength(_uin));
    if (len == 0)return true;
    int *types = static_cast<int *>(malloc(4 * len));
    int64_t *uins = static_cast<int64_t *>(malloc(8 * len));
    env->GetIntArrayRegion(_type, 0, len, types);
    env->GetLongArrayRegion(_uin, 0, len, uins);
    jclass cl_SessionInfoImpl = env->FindClass("io/github/qauxv/bridge/SessionInfoImpl");
    jmethodID createSessionInfo = env->GetStaticMethodID(cl_SessionInfoImpl, "createSessionInfo",
                                                         "(Ljava/lang/String;I)Landroid/os/Parcelable;");
    jclass cl_Str = env->FindClass("java/lang/String");
    jmethodID strValOf = env->GetStaticMethodID(cl_Str, "valueOf", "(J)Ljava/lang/String;");
    jclass cl_Facade = env->FindClass("io/github/qauxv/bridge/ChatActivityFacade");
    jmethodID send = env->GetStaticMethodID(cl_Facade, "sendMessage",
                                            "(Lmqq/app/AppRuntime;Landroid/content/Context;Landroid/os/Parcelable;Ljava/lang/String;)[J");
    for (int i = 0; i < len; i++) {
        jstring struin = (jstring) (env->CallStaticObjectMethod(cl_Str, strValOf, uins[i]));
        jobject session = env->CallStaticObjectMethod(cl_SessionInfoImpl, createSessionInfo, struin,
                                                      types[i]);
        if (session == nullptr) {
            __android_log_print(ANDROID_LOG_ERROR, "QAuxv",
                                "SessionInfoImpl/E createSessionInfo failed");
            success = false;
            break;
        }
        jlongArray msgUid = (jlongArray) env->CallStaticObjectMethod(cl_Facade, send, rt, ctx,
                                                                     session, msg);
        if (msgUid == nullptr) {
            __android_log_print(ANDROID_LOG_ERROR, "QAuxv",
                                "handleSendBatchMessages/E sendMsg failed");
            success = false;
            break;
        }
    }
    free(types);
    free(uins);
    return success;
}

jboolean handleSendCardMsg(JNIEnv *env, jclass clazz, jobject rt, jobject session, jstring msg) {
    if (rt == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/NullPointerException"), "appInterface== null");
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
    jmethodID getInstance = env->GetStaticMethodID(cardMsgListClass,"checkCardMsg", "(Ljava/lang/String;)Lme/singleneuron/data/CardMsgCheckResult;");
    jobject result = env->CallStaticObjectMethod(cardMsgListClass,getInstance,msg);
    jclass cardMsgCheckResultClass = env->FindClass("me/singleneuron/data/CardMsgCheckResult");
    jmethodID toString = env->GetMethodID(cardMsgCheckResultClass,"toString", "()Ljava/lang/String;");
    jmethodID getAccepted = env->GetMethodID(cardMsgCheckResultClass,"getAccept", "()Z");
    auto resultString = (jstring) env->CallObjectMethod(result,toString);
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
        jclass DexKit = env->FindClass("io/github/qauxv/util/DexKit");
        jmethodID cid = env->GetStaticMethodID(DexKit, "doFindClass", "(I)Ljava/lang/Class;");
        jclass TestStructMsg = (jclass) env->CallStaticObjectMethod(DexKit, cid, 18);
        if (TestStructMsg == nullptr) {
            env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "404: TestStructMsg");
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

EXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = nullptr;
    jint retCode = MMKV_JNI_OnLoad(vm, reserved);
    if (retCode < 0) {
        return retCode;
    }
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }
    jclass clazz = env->FindClass("io/github/qauxv/util/Utils");
    if (!clazz) {
        __android_log_print(ANDROID_LOG_ERROR, "QAuxv","cannot get class: Utils");
        return -1;
    }
    JNINativeMethod lMethods[1];
    jclass appInterface = env->FindClass("mqq/app/AppRuntime");
    if (appInterface == nullptr) {
        env->ExceptionClear();
        __android_log_print(ANDROID_LOG_WARN, "QAuxv", "not seem to be in host, skip native hooks");
    } else {
        clazz = env->FindClass("cc/ioctl/hook/CardMsgHook");
        lMethods[0].name = "ntSendCardMsg";
        lMethods[0].signature = "(Lmqq/app/AppRuntime;Landroid/os/Parcelable;Ljava/lang/String;)Z";
        lMethods[0].fnPtr = (void *) &handleSendCardMsg;
        if (env->RegisterNatives(clazz, lMethods, 1)) {
            __android_log_print(ANDROID_LOG_INFO, "QAuxv", "register native method[1] failed!\n");
            return -1;
        }
        clazz = env->FindClass("cc/ioctl/util/SendBatchMsg");
        lMethods[0].name = "ntSendBatchMessages";
        lMethods[0].signature = "(Lmqq/app/AppRuntime;Landroid/content/Context;Ljava/lang/String;[I[J)Z";
        lMethods[0].fnPtr = (void *) &handleSendBatchMessages;
        if (env->RegisterNatives(clazz, lMethods, 1)) {
            __android_log_print(ANDROID_LOG_INFO, "QAuxv", "register native method[2] failed!\n");
            return -1;
        }
        NativeHook_initOnce();
    }
    return retCode;
}
