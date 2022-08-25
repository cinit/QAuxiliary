#include <jni.h>
#include "../dex_kit/include/dex_kit.h"
#include <android/log.h>

#define LOG_TAG "QAuxv"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL, LOG_TAG ,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG ,__VA_ARGS__)
#define PLOGE(fmt, args...)                                                    \
  LOGE(fmt " failed with %d: %s", ##args, errno, strerror(errno))

jfieldID token_field;

extern "C"
JNIEXPORT void JNICALL
Java_me_teble_DexKitHelper_close(JNIEnv *env,
                                 jobject thiz) {
    auto *helper = reinterpret_cast<dexkit::DexKit *>(env->GetLongField(thiz, token_field));
    delete reinterpret_cast<dexkit::DexKit *>(helper);
    env->SetLongField(thiz, token_field, jlong(0));
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_teble_DexKitHelper_initDexKit(JNIEnv *env,
                                      jobject thiz,
                                      jobject class_loader) {
    if (!class_loader) {
        return -1;
    }
    jclass cClassloader = env->FindClass("java/lang/ClassLoader");
    jmethodID mGetResource = env->GetMethodID(cClassloader, "findResource",
                                              "(Ljava/lang/String;)Ljava/net/URL;");
    jstring manifestPath = env->NewStringUTF("AndroidManifest.xml");
    jobject url = env->CallObjectMethod(class_loader, mGetResource, manifestPath);
    jclass cURL = env->FindClass("java/net/URL");
    jmethodID mGetPath = env->GetMethodID(cURL, "getPath", "()Ljava/lang/String;");
    auto file = (jstring) env->CallObjectMethod(url, mGetPath);
    const char *cStr = env->GetStringUTFChars(file, nullptr);
    std::string filePathStr(cStr);
    std::string hostApkPath = filePathStr.substr(5, filePathStr.size() - 26);
    LOGD("host apk path -> %s", hostApkPath.c_str());
    auto dexKitHelper = new dexkit::DexKit(hostApkPath);
    env->SetLongField(thiz, token_field, jlong(dexKitHelper));
    return 0;
}

extern "C" JNIEXPORT jint JNICALL DexKit_JNI_OnLoad(JavaVM *vm, void *) {
    JNIEnv *env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }
    jclass helper = env->FindClass("me/teble/DexKitHelper");
    token_field = env->GetFieldID(helper, "token", "J");
    return JNI_VERSION_1_4;
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_me_teble_DexKitHelper_findMethodUsedString(JNIEnv *env,
                                                jobject thiz,
                                                jstring string) {
    auto *helper = reinterpret_cast<dexkit::DexKit *>(env->GetLongField(thiz, token_field));
    const char *findStr = env->GetStringUTFChars(string, nullptr);
    auto res = helper->FindMethodUsedString(findStr, {}, {},
                                            {}, {}, {},
                                            true, false);
    jobjectArray result = env->NewObjectArray((int) res.size(), env->FindClass("java/lang/String"),
                                              env->NewStringUTF(""));
    for (int i = 0; i < res.size(); i++) {
        env->SetObjectArrayElement(result, i, env->NewStringUTF(res[i].c_str()));
    }
    env->ReleaseStringUTFChars(string, findStr);
    return result;
}
