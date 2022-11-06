#include <cerrno>
#include <dlfcn.h>
#include <jni.h>
#include <memory.h>
#include <malloc.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/prctl.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include "natives_utils.h"
#include <android/log.h>

#include <string>
#include <vector>

#include "Natives.h"
#include "NativeMainHook.h"
#include "shared_memory.h"

static bool throwIfNull(JNIEnv *env, jobject obj, const char *msg) {
    if (obj == nullptr) {
        jclass clazz = env->FindClass("java/lang/NullPointerException");
        env->ThrowNew(clazz, msg);
        return true;
    }
    return false;
}

#define requiresNonNullP(__obj, __msg) if (throwIfNull(env, __obj, __msg)) return nullptr; ((void)0)
#define requiresNonNullV(__obj, __msg) if (throwIfNull(env, __obj, __msg)) return; ((void)0)
#define requiresNonNullZ(__obj, __msg) if (throwIfNull(env, __obj, __msg)) return 0; ((void)0)

static std::string getJstringToUtf8(JNIEnv *env, jstring jstr) {
    if (jstr == nullptr) {
        return "";
    }
    int len = env->GetStringLength(jstr);
    std::string str(len, 0);
    env->GetStringUTFRegion(jstr, 0, len, &str[0]);
    return str;
}

/*
 * Class:     io_github_qauxv_util_Natives
 * Method:    mwrite
 * Signature: (JI[BI)V
 */
EXPORT void Java_io_github_qauxv_util_Natives_mwrite
        (JNIEnv *env, jclass clz, jlong ptr, jint len, jbyteArray arr, jint offset) {
    auto *bufptr = (jbyte *) ptr;
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
    auto *bufptr = (jbyte *) ptr;
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
    auto ptr = (jlong) malloc(len);
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
    auto *dat = (uint8_t *) malloc(size);
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

EXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = nullptr;
    jint retCode = MMKV_JNI_OnLoad(vm, reserved);
    if (retCode < 0) {
        return retCode;
    }
    retCode = DexKit_JNI_OnLoad(vm, reserved);
    if (retCode < 0) {
        return retCode;
    }
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }
    JNINativeMethod lMethods[1];
    jclass appInterface = env->FindClass("mqq/app/AppRuntime");
    if (appInterface == nullptr) {
        env->ExceptionClear();
        __android_log_print(ANDROID_LOG_WARN, "QAuxv", "not seem to be in host, skip native hooks");
    } else {
        jclass clazz = env->FindClass("cc/ioctl/hook/experimental/CardMsgSender");
        lMethods[0].name = "ntSendCardMsg";
        lMethods[0].signature = "(Lmqq/app/AppRuntime;Landroid/os/Parcelable;Ljava/lang/String;)Z";
        lMethods[0].fnPtr = (void *) &handleSendCardMsg;
        if (env->RegisterNatives(clazz, lMethods, 1)) {
            __android_log_print(ANDROID_LOG_INFO, "QAuxv", "register native method[1] failed!\n");
            return -1;
        }
        NativeHook_initOnce();
    }
    return retCode;
}

extern "C" JNIEXPORT jobject JNICALL
Java_io_github_qauxv_util_Natives_allocateInstanceImpl(JNIEnv *env, jclass, jclass targetClass) {
    requiresNonNullP(targetClass, "targetClass is null");
    return env->AllocObject(targetClass);
}

jobject wrapPrimitiveValue(JNIEnv *env, char type, const jvalue &jvalue) {
    switch (type) {
        case 'Z': {
            jclass kBoolean = env->FindClass("java/lang/Boolean");
            jmethodID cid = env->GetStaticMethodID(kBoolean, "valueOf", "(Z)Ljava/lang/Boolean;");
            return env->CallStaticObjectMethod(kBoolean, cid, jvalue.z);
        }
        case 'B': {
            jclass kByte = env->FindClass("java/lang/Byte");
            jmethodID cid = env->GetStaticMethodID(kByte, "valueOf", "(B)Ljava/lang/Byte;");
            return env->CallStaticObjectMethod(kByte, cid, jvalue.b);
        }
        case 'C': {
            jclass kCharacter = env->FindClass("java/lang/Character");
            jmethodID cid = env->GetStaticMethodID(kCharacter, "valueOf", "(C)Ljava/lang/Character;");
            return env->CallStaticObjectMethod(kCharacter, cid, jvalue.c);
        }
        case 'S': {
            jclass kShort = env->FindClass("java/lang/Short");
            jmethodID cid = env->GetStaticMethodID(kShort, "valueOf", "(S)Ljava/lang/Short;");
            return env->CallStaticObjectMethod(kShort, cid, jvalue.s);
        }
        case 'I': {
            jclass kInteger = env->FindClass("java/lang/Integer");
            jmethodID cid = env->GetStaticMethodID(kInteger, "valueOf", "(I)Ljava/lang/Integer;");
            return env->CallStaticObjectMethod(kInteger, cid, jvalue.i);
        }
        case 'J': {
            jclass kLong = env->FindClass("java/lang/Long");
            jmethodID cid = env->GetStaticMethodID(kLong, "valueOf", "(J)Ljava/lang/Long;");
            return env->CallStaticObjectMethod(kLong, cid, jvalue.j);
        }
        case 'F': {
            jclass kFloat = env->FindClass("java/lang/Float");
            jmethodID cid = env->GetStaticMethodID(kFloat, "valueOf", "(F)Ljava/lang/Float;");
            return env->CallStaticObjectMethod(kFloat, cid, jvalue.f);
        }
        case 'D': {
            jclass kDouble = env->FindClass("java/lang/Double");
            jmethodID cid = env->GetStaticMethodID(kDouble, "valueOf", "(D)Ljava/lang/Double;");
            return env->CallStaticObjectMethod(kDouble, cid, jvalue.d);
        }
        case 'V': {
            return nullptr;
        }
        case 'L': {
            return jvalue.l;
        }
        default: {
            env->ThrowNew(env->FindClass("java/lang/AssertionError"),
                          (std::string("unsupported primitive type: ") + std::to_string(type)).c_str());
            return nullptr;
        }
    }
}

void extractWrappedValue(JNIEnv *env, jvalue &out, char type, jobject value) {
    switch (type) {
        case 'Z': {
            jclass kBoolean = env->FindClass("java/lang/Boolean");
            jmethodID cid = env->GetMethodID(kBoolean, "booleanValue", "()Z");
            out.z = env->CallBooleanMethod(value, cid);
            break;
        }
        case 'B': {
            jclass kByte = env->FindClass("java/lang/Byte");
            jmethodID cid = env->GetMethodID(kByte, "byteValue", "()B");
            out.b = env->CallByteMethod(value, cid);
            break;
        }
        case 'C': {
            jclass kCharacter = env->FindClass("java/lang/Character");
            jmethodID cid = env->GetMethodID(kCharacter, "charValue", "()C");
            out.c = env->CallCharMethod(value, cid);
            break;
        }
        case 'S': {
            jclass kShort = env->FindClass("java/lang/Short");
            jmethodID cid = env->GetMethodID(kShort, "shortValue", "()S");
            out.s = env->CallShortMethod(value, cid);
            break;
        }
        case 'I': {
            jclass kInteger = env->FindClass("java/lang/Integer");
            jmethodID cid = env->GetMethodID(kInteger, "intValue", "()I");
            out.i = env->CallIntMethod(value, cid);
            break;
        }
        case 'J': {
            jclass kLong = env->FindClass("java/lang/Long");
            jmethodID cid = env->GetMethodID(kLong, "longValue", "()J");
            out.j = env->CallLongMethod(value, cid);
            break;
        }
        case 'F': {
            jclass kFloat = env->FindClass("java/lang/Float");
            jmethodID cid = env->GetMethodID(kFloat, "floatValue", "()F");
            out.f = env->CallFloatMethod(value, cid);
            break;
        }
        case 'D': {
            jclass kDouble = env->FindClass("java/lang/Double");
            jmethodID cid = env->GetMethodID(kDouble, "doubleValue", "()D");
            out.d = env->CallDoubleMethod(value, cid);
            break;
        }
        case 'V': {
            out.l = nullptr;
            break;
        }
        case 'L': {
            out.l = value;
            break;
        }
        default: {
            env->ThrowNew(env->FindClass("java/lang/AssertionError"),
                          (std::string("unsupported primitive type: ") + std::to_string(type)).c_str());
            break;
        }
    }
}

jobject transformArgumentsAndInvokeNonVirtual(JNIEnv *env, jmethodID method, jclass clazz,
                                              const std::vector<char> &parameterShorts,
                                              char returnTypeShort, jobject obj, jobjectArray args) {
    int argc = int(parameterShorts.size());
    auto *jargs = new jvalue[argc];
    memset(jargs, 0, sizeof(jvalue) * argc);
    for (int i = 0; i < argc; i++) {
        extractWrappedValue(env, jargs[i], parameterShorts[i], env->GetObjectArrayElement(args, i));
        if (env->ExceptionCheck()) {
            delete[] jargs;
            return nullptr;
        }
    }
    jvalue ret;
    memset(&ret, 0, sizeof(jvalue));
    switch (returnTypeShort) {
        case 'L': {
            ret.l = env->CallNonvirtualObjectMethodA(obj, clazz, method, jargs);
            break;
        }
        case 'Z': {
            ret.z = env->CallNonvirtualBooleanMethodA(obj, clazz, method, jargs);
            break;
        }
        case 'B': {
            ret.b = env->CallNonvirtualByteMethodA(obj, clazz, method, jargs);
            break;
        }
        case 'C': {
            ret.c = env->CallNonvirtualCharMethodA(obj, clazz, method, jargs);
            break;
        }
        case 'S': {
            ret.s = env->CallNonvirtualShortMethodA(obj, clazz, method, jargs);
            break;
        }
        case 'I': {
            ret.i = env->CallNonvirtualIntMethodA(obj, clazz, method, jargs);
            break;
        }
        case 'J': {
            ret.j = env->CallNonvirtualLongMethodA(obj, clazz, method, jargs);
            break;
        }
        case 'F': {
            ret.f = env->CallNonvirtualFloatMethodA(obj, clazz, method, jargs);
            break;
        }
        case 'D': {
            ret.d = env->CallNonvirtualDoubleMethodA(obj, clazz, method, jargs);
            break;
        }
        case 'V': {
            env->CallNonvirtualVoidMethodA(obj, clazz, method, jargs);
            ret.l = nullptr;
            break;
        }
        default: {
            env->ThrowNew(env->FindClass("java/lang/AssertionError"),
                          (std::string("unsupported primitive type: ") + std::to_string(returnTypeShort)).c_str());
            delete[] jargs;
            return nullptr;
        }
    }
    delete[] jargs;
    // check for exceptions
    if (env->ExceptionCheck()) {
        // wrap exception with InvocationTargetException
        jthrowable exception = env->ExceptionOccurred();
        env->ExceptionClear();
        jclass exceptionClass = env->FindClass("java/lang/reflect/InvocationTargetException");
        jmethodID exceptionConstructor = env->GetMethodID(exceptionClass, "<init>", "(Ljava/lang/Throwable;)V");
        jobject exceptionObject = env->NewObject(exceptionClass, exceptionConstructor, exception);
        env->Throw((jthrowable) exceptionObject);
        return nullptr;
    }
    // wrap return value
    return wrapPrimitiveValue(env, returnTypeShort, ret);
}

extern "C" JNIEXPORT jobject JNICALL
Java_io_github_qauxv_util_Natives_invokeNonVirtualImpl(JNIEnv *env, jclass,
                                                       jclass klass, jstring method_name, jstring method_sig,
                                                       jobject obj, jobjectArray args) {
    requiresNonNullP(klass, "declaringClass is null");
    requiresNonNullP(method_name, "method_name is null");
    requiresNonNullP(method_sig, "method_sig is null");
    requiresNonNullP(obj, "obj is null");
    std::string methodName = getJstringToUtf8(env, method_name);
    std::string methodSignature = getJstringToUtf8(env, method_sig);
    jclass targetClass = klass;
    if (targetClass == nullptr) {
        return nullptr;
    }
    jmethodID method = env->GetMethodID(targetClass, methodName.c_str(), methodSignature.c_str());
    if (method == nullptr) {
        return nullptr;
    }
    int argc = args == nullptr ? 0 : env->GetArrayLength(args);
    // parse method signature
    std::vector<char> paramShorts;
    paramShorts.reserve(argc);
    // skip first '('
    for (int i = 1; i < methodSignature.length(); i++) {
        if (methodSignature[i] == ')') {
            break;
        }
        if (methodSignature[i] == 'L') {
            paramShorts.push_back('L');
            while (methodSignature[i] != ';') {
                i++;
            }
        } else if (methodSignature[i] == '[') {
            paramShorts.push_back('L');
            // it's an array, so we just skip the '['
            while (methodSignature[i] == '[') {
                i++;
            }
            // check if it's a primitive array
            if (methodSignature[i] == 'L') {
                while (methodSignature[i] != ';') {
                    i++;
                }
            }
        } else {
            paramShorts.push_back(methodSignature[i]);
        }
    }
    // find return type, start from last ')'
    char returnTypeShort = 0;
    for (auto i = methodSignature.length() - 1; i >= 0; i--) {
        if (methodSignature[i] == ')') {
            returnTypeShort = methodSignature[i + 1];
            break;
        }
    }
    if (returnTypeShort == '[') {
        returnTypeShort = 'L';
    }
    if (paramShorts.size() != argc) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"),
                      "parameter count mismatch");
        return nullptr;
    }
    // invoke
    return transformArgumentsAndInvokeNonVirtual(env, method, targetClass, paramShorts, returnTypeShort, obj, args);
}

extern "C" JNIEXPORT jint JNICALL
Java_io_github_qauxv_util_Natives_write(JNIEnv *env, jclass clazz, jint fd, jbyteArray buf, jint offset, jint len) {
    requiresNonNullZ(buf, "buf is null");
    if (fd < 0) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"),
                      (std::string("fd is negative: ") + std::to_string(fd)).c_str());
        return 0;
    }
    int arrayLen = env->GetArrayLength(buf);
    if (offset < 0 || len < 0 || offset + len > arrayLen) {
        env->ThrowNew(env->FindClass("java/lang/IndexOutOfBoundsException"),
                      (std::string("offset or len is out of bounds: ") + std::to_string(offset) + " "
                              + std::to_string(len) + " " + std::to_string(arrayLen)).c_str());
        return 0;
    }
    jbyte *bufPtr = env->GetByteArrayElements(buf, nullptr);
    if (bufPtr == nullptr) {
        env->ThrowNew(env->FindClass("java/io/IOException"), "failed to allocate memory");
        return 0;
    }
    ssize_t written = write(fd, bufPtr + offset, len);
    int err = errno;
    env->ReleaseByteArrayElements(buf, bufPtr, JNI_ABORT);
    if (written < 0) {
        env->ThrowNew(env->FindClass("java/io/IOException"), strerror(err));
        return 0;
    }
    return (jint) written;
}

extern "C" JNIEXPORT jint JNICALL
Java_io_github_qauxv_util_Natives_read(JNIEnv *env, jclass, jint fd, jbyteArray buf, jint offset, jint len) {
    requiresNonNullZ(buf, "buf is null");
    if (fd < 0) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"),
                      (std::string("fd is negative: ") + std::to_string(fd)).c_str());
        return 0;
    }
    int arrayLen = env->GetArrayLength(buf);
    if (offset < 0 || len < 0 || offset + len > arrayLen) {
        env->ThrowNew(env->FindClass("java/lang/IndexOutOfBoundsException"),
                      (std::string("offset or len is out of bounds: ") + std::to_string(offset)
                              + " " + std::to_string(len) + " " + std::to_string(arrayLen)).c_str());
        return 0;
    }
    jbyte *bufPtr = env->GetByteArrayElements(buf, nullptr);
    if (bufPtr == nullptr) {
        env->ThrowNew(env->FindClass("java/io/IOException"), "failed to allocate memory");
        return 0;
    }
    ssize_t r = read(fd, bufPtr + offset, len);
    int err = errno;
    env->ReleaseByteArrayElements(buf, bufPtr, 0);
    if (r < 0) {
        env->ThrowNew(env->FindClass("java/io/IOException"), strerror(err));
        return 0;
    }
    return (jint) r;
}

extern "C" JNIEXPORT void JNICALL
Java_io_github_qauxv_util_Natives_close(JNIEnv *env, jclass, jint fd) {
    if (fd < 0) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"),
                      (std::string("fd is negative: ") + std::to_string(fd)).c_str());
        return;
    }
    if (close(fd) < 0) {
        env->ThrowNew(env->FindClass("java/io/IOException"), strerror(errno));
    }
}

static char sTmpDir[PATH_MAX] = {};

extern "C" JNIEXPORT jint JNICALL
Java_io_github_qauxv_util_MemoryFileUtils_nativeInitializeTmpDir(JNIEnv *env, jclass, jstring jcacheDir) {
    if (jcacheDir == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "cacheDir is null");
        return -EINVAL;
    }
    const char *cacheDir = env->GetStringUTFChars(jcacheDir, nullptr);
    if (cacheDir == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/OutOfMemoryError"), "failed to allocate memory");
        return -ENOMEM;
    }
    if (access(cacheDir, W_OK) != 0) {
        return -errno;
    }
    std::string cacheDirStr = cacheDir;
    env->ReleaseStringUTFChars(jcacheDir, cacheDir);
    std::string tmpDir = cacheDirStr + "/.tmp";
    if (access(tmpDir.c_str(), W_OK) != 0) {
        if (mkdir(tmpDir.c_str(), 0700) != 0) {
            return -errno;
        }
    }
    strncpy(sTmpDir, tmpDir.c_str(), sizeof(sTmpDir));
    return 0;
}

extern "C" JNIEXPORT jint JNICALL
Java_io_github_qauxv_util_MemoryFileUtils_nativeCreateMemoryFile0(JNIEnv *env, jclass, jstring name, jint size) {
    if (strlen(sTmpDir) == 0) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), "tmpDir is not initialized");
        return -EINVAL;
    }
    if (name == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/NullPointerException"), "name is null");
        return -1;
    }
    if (size < 0) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"),
                      (std::string("size is negative: ") + std::to_string(size)).c_str());
        return -1;
    }
    const char *namePtr = env->GetStringUTFChars(name, nullptr);
    if (namePtr == nullptr) {
        env->ThrowNew(env->FindClass("java/io/IOException"), "failed to allocate memory");
        return -1;
    }
    int fd = create_in_memory_file(sTmpDir, namePtr, (size_t) size);
    env->ReleaseStringUTFChars(name, namePtr);
    if (fd < 0) {
        int err = -fd;
        env->ThrowNew(env->FindClass("java/io/IOException"), strerror(err));
        return -1;
    } else {
        return fd;
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_io_github_qauxv_util_Natives_getProcessDumpableState(JNIEnv *env, jclass) {
    int dumpable = prctl(PR_GET_DUMPABLE);
    if (dumpable < 0) {
        int err = errno;
        const char *msg = strerror(err);
        env->ThrowNew(env->FindClass("java/io/IOException"), msg);
        return -1;
    }
    return dumpable;
}

extern "C" JNIEXPORT void JNICALL
Java_io_github_qauxv_util_Natives_setProcessDumpableState(JNIEnv *env, jclass, jint dumpable) {
    if (dumpable < 0 || dumpable > 2) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"),
                      (std::string("dumpable is out of bounds: ") + std::to_string(dumpable)).c_str());
        return;
    }
    if (prctl(PR_SET_DUMPABLE, dumpable, 0, 0, 0) < 0) {
        int err = errno;
        const char *msg = strerror(err);
        env->ThrowNew(env->FindClass("java/io/IOException"), msg);
    }
}

extern "C" JNIEXPORT jlong JNICALL
Java_io_github_qauxv_util_Natives_lseek(JNIEnv *env, jclass, jint fd, jlong offset, jint whence) {
    if (fd < 0) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"),
                      (std::string("fd is negative: ") + std::to_string(fd)).c_str());
        return -1;
    }
    if (whence < 0 || whence > 2) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"),
                      (std::string("whence is invalid: ") + std::to_string(whence)).c_str());
        return -1;
    }
    off_t result = lseek(fd, (int64_t) offset, whence);
    if (result < 0) {
        int err = errno;
        env->ThrowNew(env->FindClass("java/io/IOException"), strerror(err));
        return -1;
    }
    return (jlong) result;
}

extern "C" JNIEXPORT jint JNICALL
Java_cc_ioctl_util_JunkCodeUtils_getJunkCode(JNIEnv *, jclass, jint jtc) {
    auto tc = uint32_t(jtc);
    const char *magic = "mIplOkwgxe3bzGc6g9K1BNJlXbuNmM+kYGWuoFGDOAZD1vHBEROCj+AN2TmBKXc0wEDLXgE+XgxL";
    auto magic_len = strlen(magic);
    uint32_t a32 = update_adler32(tc, reinterpret_cast<const uint8_t *>(magic), magic_len);
    uint32_t o = a32 & 0xf;
    // code should be in the range [0, 999999]
    uint32_t code = (a32 >> o) % 1000000;
    return jint(code);
}
