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
    if (helper == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/NullPointerException"), "helper is null");
        return nullptr;
    }
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


static std::vector<std::string> getJavaStringArray(JNIEnv *env, jobjectArray strings) {
    std::vector<std::string> results;
    if (strings == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/NullPointerException"), "jstringKeyArray is null");
        return {};
    }
    if (env->ExceptionCheck()) {
        return {};
    }
    for (int i = 0; i < env->GetArrayLength(strings); i++) {
        auto string = (jstring) env->GetObjectArrayElement(strings, i);
        if (string == nullptr) {
            env->ThrowNew(env->FindClass("java/lang/NullPointerException"),
                          ("string at index " + std::to_string(i) + " is null").c_str());
            return {};
        }
        const char *findStr = env->GetStringUTFChars(string, nullptr);
        results.emplace_back(findStr);
        env->ReleaseStringUTFChars(string, findStr);
        env->DeleteLocalRef(string);
    }
    return results;
}

static jobjectArray stringArrayToJavaArray(JNIEnv *env, const std::vector<std::string> &strings) {
    jobjectArray result = env->NewObjectArray((int) strings.size(), env->FindClass("java/lang/String"), nullptr);
    if (result == nullptr) {
        return nullptr;
    }
    for (int i = 0; i < strings.size(); i++) {
        auto str = env->NewStringUTF(strings[i].c_str());
        if (str == nullptr) {
            return nullptr;
        }
        env->SetObjectArrayElement(result, i, str);
        env->DeleteLocalRef(str);
    }
    return result;
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_me_teble_DexKitHelper_batchFindMethodUsedString(JNIEnv *env,
                                                     jobject thiz,
                                                     jobjectArray jdesignatorArray,
                                                     jobjectArray jstringKeyArray) {
    auto *helper = reinterpret_cast<dexkit::DexKit *>(env->GetLongField(thiz, token_field));
    if (helper == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/NullPointerException"), "helper is null");
        return nullptr;
    }
    std::vector<std::string> designatorArray = getJavaStringArray(env, jdesignatorArray);
    if (env->ExceptionCheck()) {
        return nullptr;
    }
    std::vector<std::string> stringKeyArray = getJavaStringArray(env, jstringKeyArray);
    if (env->ExceptionCheck()) {
        return nullptr;
    }
    if (designatorArray.size() != stringKeyArray.size()) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"),
                      "designatorArray and stringKeyArray size not equal");
        return nullptr;
    }
    auto inputMap = std::map<std::string, std::set<std::string >>();
    for (int i = 0; i < designatorArray.size(); i++) {
        inputMap[designatorArray[i]].insert(stringKeyArray[i]);
    }
    auto results = helper->LocationMethods(inputMap, false);
    jobjectArray resultArray2 = env->NewObjectArray((int) designatorArray.size(), env->FindClass("[Ljava/lang/String;"), nullptr);
    if (resultArray2 == nullptr) {
        return nullptr;
    }
    for (int i = 0; i < designatorArray.size(); i++) {
        auto result = stringArrayToJavaArray(env, results[designatorArray[i]]);
        if (result == nullptr) {
            return nullptr;
        }
        env->SetObjectArrayElement(resultArray2, i, result);
        env->DeleteLocalRef(result);
    }
    return resultArray2;
}
