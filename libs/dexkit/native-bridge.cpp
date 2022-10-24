#include <jni.h>
#include <android/log.h>
#include <dex_kit.h>
#include "dex_kit_jni_helper.h"

#define TAG "DexKit"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL, TAG ,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG ,__VA_ARGS__)

static jclass dex_kit_class = nullptr;
static JavaVM *g_currentJVM = nullptr;

static jfieldID path_list_field = nullptr;
static jfieldID element_field = nullptr;
static jfieldID dex_file_field = nullptr;
static jfieldID cookie_field = nullptr;
static jfieldID file_name_field = nullptr;
static bool is_initialized = false;

static int registerNativeMethods(JNIEnv *env, jclass cls);

extern "C" JNIEXPORT JNICALL jint DexKit_JNI_OnLoad(JavaVM *vm, void *reserved) {
    g_currentJVM = vm;
    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }
    dex_kit_class = env->FindClass("io/luckypray/dexkit/DexKitBridge");
    int ret = registerNativeMethods(env, dex_kit_class);
    if (ret != 0) {
        return -3;
    }
    return JNI_VERSION_1_6;
}

namespace DexKit {

struct DexFile {
  const void *begin_{};
  size_t size_{};

  virtual ~DexFile() = default;
};

static bool IsCompactDexFile(const void *image) {
    const auto *header = reinterpret_cast<const struct dex::Header *>(image);
    if (header->magic[0] == 'c' && header->magic[1] == 'd' &&
            header->magic[2] == 'e' && header->magic[3] == 'x') {
        return true;
    }
    return false;
}

static bool CheckPoint(void *addr) {
    int nullfd = open("/dev/random", O_WRONLY);
    bool valid = true;
    if (write(nullfd, (void *) addr, sizeof(addr)) < 0) {
        valid = false;
    }
    close(nullfd);
    return valid;
}

void init(JNIEnv *env) {
    if (is_initialized) {
        return;
    }
    auto dex_class_loader = env->FindClass("dalvik/system/BaseDexClassLoader");
    path_list_field = env->GetFieldID(dex_class_loader, "pathList",
                                      "Ldalvik/system/DexPathList;");
    auto dex_path_list = env->FindClass("dalvik/system/DexPathList");
    element_field = env->GetFieldID(dex_path_list, "dexElements",
                                    "[Ldalvik/system/DexPathList$Element;");
    auto element = env->FindClass("dalvik/system/DexPathList$Element");
    dex_file_field =
            env->GetFieldID(element, "dexFile", "Ldalvik/system/DexFile;");
    auto dex_file = env->FindClass("dalvik/system/DexFile");
    cookie_field = env->GetFieldID(dex_file, "mCookie", "Ljava/lang/Object;");
    file_name_field = env->GetFieldID(dex_file, "mFileName", "Ljava/lang/String;");

    is_initialized = true;
}

#define DEXKIT_JNI extern "C" JNIEXPORT JNICALL

DEXKIT_JNI jlong
nativeInitDexKit(JNIEnv *env, jclass clazz,
                 jstring apk_path) {
    if (!apk_path) {
        return 0;
    }
    const char *cStr = env->GetStringUTFChars(apk_path, nullptr);
    LOGI("apkPath -> %s", cStr);
    std::string filePathStr(cStr);
    auto dexkit = new dexkit::DexKit(filePathStr);
    env->ReleaseStringUTFChars(apk_path, cStr);
    return (jlong) dexkit;
}

DEXKIT_JNI jlong
nativeInitDexKitByClassLoader(JNIEnv *env, jclass clazz,
                              jobject class_loader) {
    if (!class_loader) {
        return 0;
    }
    init(env);
    auto path_list = env->GetObjectField(class_loader, path_list_field);
    if (!path_list)
        return 0;
    auto elements = (jobjectArray) env->GetObjectField(path_list, element_field);
    if (!elements)
        return 0;
    LOGD("elements size -> %d", env->GetArrayLength(elements));
    auto dexkit = new dexkit::DexKit();
    for (auto i = 0, len = env->GetArrayLength(elements); i < len; ++i) {
        auto element = env->GetObjectArrayElement(elements, i);
        if (!element) continue;
        auto java_dex_file = env->GetObjectField(element, dex_file_field);
        if (!java_dex_file) continue;
        auto cookie = (jlongArray) env->GetObjectField(java_dex_file, cookie_field);
        if (!cookie) continue;
        auto dex_file_length = env->GetArrayLength(cookie);
        const auto *dex_files = reinterpret_cast<const DexFile **>(
                env->GetLongArrayElements(cookie, nullptr));
        LOGI("dex_file_length -> %d", dex_file_length);
        std::vector<const DexFile *> dex_images;
        for (int j = 0; j < dex_file_length; ++j) {
            const auto *dex_file = dex_files[j];
            if (!CheckPoint((void *) dex_file) ||
                    !CheckPoint((void *) dex_file->begin_) ||
                    dex_file->size_ < sizeof(dex::Header)) {
                LOGD("dex_file %d is invalid", j);
                continue;
            }
            if (IsCompactDexFile(dex_file->begin_)) {
                LOGD("skip compact dex");
                dex_images.clear();
                break;
            } else {
                LOGD("push standard dex file %d, image size: %zu", j, dex_file->size_);
                dex_images.emplace_back(dex_file);
            }
        }
        if (dex_images.empty()) {
            auto file_name_obj = (jstring) env->GetObjectField(java_dex_file, file_name_field);
            if (!file_name_obj) continue;
            auto file_name = env->GetStringUTFChars(file_name_obj, nullptr);
            LOGD("contains compact dex, use path load: %s", file_name);
            dexkit->AddPath(file_name);
        } else {
            std::vector<std::unique_ptr<dexkit::MemMap>> images;
            for (auto &image: dex_images) {
                auto mmap = dexkit::MemMap(image->size_);
                memcpy(mmap.addr(), image->begin_, image->size_);
                images.emplace_back(std::make_unique<dexkit::MemMap>(std::move(mmap)));
            }
            dexkit->AddImages(std::move(images));
        }
    }
    return (jlong) dexkit;
}

DEXKIT_JNI void
nativeSetThreadNum(JNIEnv *env, jclass clazz,
                   jlong native_ptr, jint thread_num) {
    if (!native_ptr) {
        return;
    }
    SetThreadNum(env, native_ptr, thread_num);
}

DEXKIT_JNI jint
nativeGetDexNum(JNIEnv *env, jclass clazz,
                jlong native_ptr) {
    if (!native_ptr) {
        return 0;
    }
    return GetDexNum(env, native_ptr);
}

DEXKIT_JNI void
nativeRelease(JNIEnv *env, jclass clazz,
              jlong native_ptr) {
    if (!native_ptr) {
        return;
    }
    ReleaseDexKitInstance(env, native_ptr);
}

DEXKIT_JNI jobject
nativeBatchFindClassesUsingStrings(JNIEnv *env,
                                   jclass clazz,
                                   jlong native_ptr,
                                   jobject map,
                                   jboolean advanced_match,
                                   jintArray dex_priority) {
    if (!native_ptr) {
        return CMap2JMap(env, std::map<std::string, std::vector<std::string>>());
    }
    return BatchFindClassesUsingStrings(env, native_ptr, map, advanced_match, dex_priority);
}

DEXKIT_JNI jobject
nativeBatchFindMethodsUsingStrings(JNIEnv *env,
                                   jclass clazz,
                                   jlong native_ptr,
                                   jobject map,
                                   jboolean advanced_match,
                                   jintArray dex_priority) {
    if (!native_ptr) {
        return CMap2JMap(env, std::map<std::string, std::vector<std::string>>());
    }
    return BatchFindMethodsUsingStrings(env, native_ptr, map, advanced_match, dex_priority);
}

DEXKIT_JNI jobjectArray
nativeFindMethodCaller(JNIEnv *env, jclass clazz,
                       jlong native_ptr,
                       jstring method_descriptor,
                       jstring method_declare_class,
                       jstring method_declare_name,
                       jstring method_return_type,
                       jobjectArray method_param_types,
                       jstring caller_method_declare_class,
                       jstring caller_method_declare_name,
                       jstring caller_method_return_type,
                       jobjectArray caller_method_param_types,
                       jintArray dex_priority) {
    if (!native_ptr) {
        return StrVec2JStrArr(env, std::vector<std::string>());
    }
    return FindMethodCaller(env, native_ptr, method_descriptor, method_declare_class,
                            method_declare_name, method_return_type, method_param_types,
                            caller_method_declare_class, caller_method_declare_name,
                            caller_method_return_type, caller_method_param_types,
                            dex_priority);
}

DEXKIT_JNI jobject
nativeFindMethodInvoking(JNIEnv *env, jclass clazz,
                         jlong native_ptr,
                         jstring method_descriptor,
                         jstring method_declare_class,
                         jstring method_declare_name,
                         jstring method_return_type,
                         jobjectArray method_param_types,
                         jstring be_called_method_declare_class,
                         jstring be_called_method_declare_name,
                         jstring be_called_method_return_type,
                         jobjectArray be_called_method_param_types,
                         jintArray dex_priority) {
    if (!native_ptr) {
        return CMap2JMap(env, std::map<std::string, std::vector<std::string>>());
    }
    return FindMethodInvoking(env, native_ptr, method_descriptor, method_declare_class,
                              method_declare_name, method_return_type, method_param_types,
                              be_called_method_declare_class, be_called_method_declare_name,
                              be_called_method_return_type, be_called_method_param_types,
                              dex_priority);
}

DEXKIT_JNI jobject
nativeFindMethodUsingField(JNIEnv *env, jclass clazz,
                           jlong native_ptr,
                           jstring field_descriptor,
                           jstring field_declare_class,
                           jstring field_name,
                           jstring field_type,
                           jint used_flags,
                           jstring caller_method_declare_class,
                           jstring caller_method_name,
                           jstring caller_method_return_type,
                           jobjectArray caller_method_param_types,
                           jintArray dex_priority) {
    if (!native_ptr) {
        return StrVec2JStrArr(env, std::vector<std::string>());
    }
    return FindMethodUsingField(env, native_ptr, field_descriptor, field_declare_class,
                                field_name,
                                field_type, used_flags, caller_method_declare_class,
                                caller_method_name, caller_method_return_type,
                                caller_method_param_types, dex_priority);
}

DEXKIT_JNI jobjectArray
nativeFindMethodUsingString(JNIEnv *env, jclass clazz,
                            jlong native_ptr,
                            jstring used_string,
                            jboolean advanced_match,
                            jstring method_declare_class,
                            jstring method_name,
                            jstring method_return_type,
                            jobjectArray method_param_types,
                            jintArray dex_priority) {
    if (!native_ptr) {
        return StrVec2JStrArr(env, std::vector<std::string>());
    }
    return FindMethodUsingString(env, native_ptr, used_string, advanced_match,
                                 method_declare_class,
                                 method_name, method_return_type, method_param_types,
                                 dex_priority);
}

DEXKIT_JNI jobjectArray
nativeFindMethod(JNIEnv *env, jclass clazz,
                 jlong native_ptr,
                 jstring method_declare_class,
                 jstring method_name,
                 jstring method_return_type,
                 jobjectArray method_param_types,
                 jintArray dex_priority) {
    if (!native_ptr) {
        return StrVec2JStrArr(env, std::vector<std::string>());
    }
    return FindMethod(env, native_ptr, method_declare_class, method_name, method_return_type,
                      method_param_types, dex_priority);
}

DEXKIT_JNI jobjectArray
nativeFindSubClasses(JNIEnv *env, jclass clazz,
                     jlong native_ptr,
                     jstring parent_class,
                     jintArray dex_priority) {
    if (!native_ptr) {
        return StrVec2JStrArr(env, std::vector<std::string>());
    }
    return FindSubClasses(env, native_ptr, parent_class, dex_priority);
}

DEXKIT_JNI jobjectArray
nativeFindMethodOpPrefixSeq(JNIEnv *env, jclass clazz,
                            jlong native_ptr,
                            jintArray op_prefix_seq,
                            jstring method_declare_class,
                            jstring method_name,
                            jstring method_return_type,
                            jobjectArray method_param_types,
                            jintArray dex_priority) {
    if (!native_ptr) {
        return StrVec2JStrArr(env, std::vector<std::string>());
    }
    return FindMethodOpPrefixSeq(env, native_ptr, op_prefix_seq, method_declare_class,
                                 method_name,
                                 method_return_type, method_param_types, dex_priority);
}

DEXKIT_JNI jobjectArray
nativeFindMethodUsingOpCodeSeq(JNIEnv *env, jclass clazz,
                               jlong native_ptr,
                               jintArray op_code_seq,
                               jstring method_declare_class,
                               jstring method_name,
                               jstring method_return_type,
                               jobjectArray method_param_types,
                               jintArray dex_priority) {
    if (!native_ptr) {
        return StrVec2JStrArr(env, std::vector<std::string>());
    }
    return FindMethodUsingOpCodeSeq(env, native_ptr, op_code_seq, method_declare_class, method_name,
                                    method_return_type, method_param_types, dex_priority);
}

DEXKIT_JNI jobject
nativeGetMethodOpCodeSeq(JNIEnv *env, jclass clazz,
                         jlong native_ptr,
                         jstring method_descriptor,
                         jstring method_declare_class,
                         jstring method_name,
                         jstring method_return_type,
                         jobjectArray method_param_types,
                         jintArray dex_priority) {
    if (!native_ptr) {
        return EmptyJMap(env);
    }
    return GetMethodOpCodeSeq(env, native_ptr, method_descriptor, method_declare_class, method_name,
                              method_return_type, method_param_types, dex_priority);
}

} // namespace

static JNINativeMethod g_methods[]{
        {"nativeInitDexKit", "(Ljava/lang/String;)J", (void *) DexKit::nativeInitDexKit},
        {"nativeInitDexKitByClassLoader", "(Ljava/lang/ClassLoader;)J", (void *) DexKit::nativeInitDexKitByClassLoader},
        {"nativeSetThreadNum", "(JI)V", (void *) DexKit::nativeSetThreadNum},
        {"nativeGetDexNum", "(J)I", (void *) DexKit::nativeGetDexNum},
        {"nativeRelease", "(J)V", (void *) DexKit::nativeRelease},
        {"nativeBatchFindClassesUsingStrings", "(JLjava/util/Map;Z[I)Ljava/util/Map;", (void *) DexKit::nativeBatchFindClassesUsingStrings},
        {"nativeBatchFindMethodsUsingStrings", "(JLjava/util/Map;Z[I)Ljava/util/Map;", (void *) DexKit::nativeBatchFindMethodsUsingStrings},
        {"nativeFindMethodCaller",
         "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[I)[Ljava/lang/String;",
         (void *) DexKit::nativeFindMethodCaller},
        {"nativeFindMethodInvoking",
         "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[I)Ljava/util/Map;",
         (void *) DexKit::nativeFindMethodInvoking},
        {"nativeFindMethodUsingField",
         "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[I)Ljava/util/Map;",
         (void *) DexKit::nativeFindMethodUsingField},
        {"nativeFindMethodUsingString", "(JLjava/lang/String;ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[I)[Ljava/lang/String;",
         (void *) DexKit::nativeFindMethodUsingString},
        {"nativeFindMethod", "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[I)[Ljava/lang/String;",
         (void *) DexKit::nativeFindMethod},
        {"nativeFindSubClasses", "(JLjava/lang/String;[I)[Ljava/lang/String;", (void *) DexKit::nativeFindSubClasses},
        {"nativeFindMethodOpPrefixSeq", "(J[ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[I)[Ljava/lang/String;",
         (void *) DexKit::nativeFindMethodOpPrefixSeq},
        {"nativeFindMethodUsingOpCodeSeq", "(J[ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[I)[Ljava/lang/String;",
         (void *) DexKit::nativeFindMethodUsingOpCodeSeq},
        {"nativeGetMethodOpCodeSeq", "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[I)Ljava/util/Map;",
         (void *) DexKit::nativeGetMethodOpCodeSeq},
};

static int registerNativeMethods(JNIEnv *env, jclass cls) {
    return env->RegisterNatives(cls, g_methods, sizeof(g_methods) / sizeof(g_methods[0]));
}
