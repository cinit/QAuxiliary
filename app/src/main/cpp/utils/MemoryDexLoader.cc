//
// Created by sulfate on 2023-07-21.
//

#include "MemoryDexLoader.h"

#include <cstdint>
#include <vector>
#include <mutex>
#include <string>
#include <jni.h>
#include <unistd.h>
#include <sys/mman.h>
#include <cerrno>
#include <mutex>

#include <android/api-level.h>

#include <fmt/format.h>
#include <dobby.h>

#include "qauxv_core/NativeCoreBridge.h"
#include "qauxv_core/jni_method_registry.h"
#include "utils/art_symbol_resolver.h"

#include "utils/Log.h"

namespace qauxv {

namespace art {
struct MemMap;
struct OatFile;
struct OatDexFile;
}

const art::DexFile* art::DexFile::OpenMemory(const uint8_t* dex_file, size_t size, std::string location, std::string* error_msg) {
    // Android 5.0 API 21
    //  static const DexFile* OpenMemory(const byte* dex_file,
    //                                   size_t size,
    //                                   const std::string& location,
    //                                   uint32_t location_checksum,
    //                                   MemMap* mem_map,
    //                                   std::string* error_msg);
    using OpenMemory21_t = const art::DexFile* (*)(const uint8_t*, size_t, const std::string&, uint32_t, MemMap*, std::string*);
    // Android 5.1 API 22
    //  static const DexFile* OpenMemory(const byte* dex_file,
    //                                   size_t size,
    //                                   const std::string& location,
    //                                   uint32_t location_checksum,
    //                                   MemMap* mem_map,
    //                                   const OatFile* oat_file,
    //                                   std::string* error_msg);
    using OpenMemory22_t = const art::DexFile* (*)(const uint8_t*, size_t, const std::string&, uint32_t, MemMap*, const OatFile*, std::string*);
    // Android 6.0-7.1 API 23-25
    //  static std::unique_ptr<const DexFile> OpenMemory(const uint8_t* dex_file,
    //                                                   size_t size,
    //                                                   const std::string& location,
    //                                                   uint32_t location_checksum,
    //                                                   MemMap* mem_map,
    //                                                   const OatDexFile* oat_dex_file,
    //                                                   std::string* error_msg);
    using OpenMemory23_t = std::unique_ptr<const art::DexFile>(*)(const uint8_t*,
                                                                  size_t,
                                                                  const std::string&,
                                                                  uint32_t,
                                                                  MemMap*,
                                                                  const OatDexFile*,
                                                                  std::string*);
    // Android 8.0-8.1 API 26-27
    //   static std::unique_ptr<const DexFile> OpenMemory(const uint8_t* dex_file,
    //                                                   size_t size,
    //                                                   const std::string& location,
    //                                                   uint32_t location_checksum,
    //                                                   std::unique_ptr<MemMap> mem_map,
    //                                                   const OatDexFile* oat_dex_file,
    //                                                   std::string* error_msg);
    using OpenMemory26_t = std::unique_ptr<const art::DexFile>(*)(const uint8_t*,
                                                                  size_t,
                                                                  const std::string&,
                                                                  uint32_t,
                                                                  std::unique_ptr<MemMap>,
                                                                  const OatDexFile*,
                                                                  std::string*);
    static OpenMemory21_t OpenMemory21 = nullptr;
    static OpenMemory22_t OpenMemory22 = nullptr;
    static OpenMemory23_t OpenMemory23 = nullptr;
    // We don't need to this on SDK 26+, because that can be created by DexFile java constructor with ByteBuffer.
    if (OpenMemory21 == nullptr && OpenMemory22 == nullptr && OpenMemory23 == nullptr) {
        // find symbol
        auto libart = GetModuleSymbolResolver("libart.so");
        if (libart == nullptr) {
            if (error_msg) {
                *error_msg = "libart.so not found";
            }
            return nullptr;
        }
        // try SDK 23 first
        OpenMemory23 = reinterpret_cast<OpenMemory23_t>(libart->GetSymbol(
                "_ZN3art7DexFile10OpenMemoryEPKhjRKNSt3__112basic_stringIcNS3_11char_traitsIcEENS3_9allocatorIcEEEEjPNS_6MemMapEPKNS_10OatDexFileEPS9_"
        ));
        if (OpenMemory23 == nullptr) {
            OpenMemory23 = reinterpret_cast<OpenMemory23_t>(libart->GetSymbol(
                    "_ZN3art7DexFile10OpenMemoryEPKhmRKNSt3__112basic_stringIcNS3_11char_traitsIcEENS3_9allocatorIcEEEEjPNS_6MemMapEPKNS_10OatDexFileEPS9_"
            ));
        }
        // try SDK 22
        OpenMemory22 = reinterpret_cast<OpenMemory22_t>(libart->GetSymbol(
                "_ZN3art7DexFile10OpenMemoryEPKhjRKNSt3__112basic_stringIcNS3_11char_traitsIcEENS3_9allocatorIcEEEEjPNS_6MemMapEPKNS_7OatFileEPS9_"
        ));
        if (OpenMemory22 == nullptr) {
            OpenMemory22 = reinterpret_cast<OpenMemory22_t>(libart->GetSymbol(
                    "_ZN3art7DexFile10OpenMemoryEPKhmRKNSt3__112basic_stringIcNS3_11char_traitsIcEENS3_9allocatorIcEEEEjPNS_6MemMapEPKNS_7OatFileEPS9_"
            ));
        }
        // try SDK 21
        OpenMemory21 = reinterpret_cast<OpenMemory21_t>(libart->GetSymbol(
                "_ZN3art7DexFile10OpenMemoryEPKhjRKNSt3__112basic_stringIcNS3_11char_traitsIcEENS3_9allocatorIcEEEEjPNS_6MemMapEPS9_"
        ));
        if (OpenMemory21 == nullptr) {
            OpenMemory21 = reinterpret_cast<OpenMemory21_t>(libart->GetSymbol(
                    "_ZN3art7DexFile10OpenMemoryEPKhmRKNSt3__112basic_stringIcNS3_11char_traitsIcEENS3_9allocatorIcEEEEjPNS_6MemMapEPS9_"
            ));
        }
    }
    if (OpenMemory21 == nullptr && OpenMemory22 == nullptr && OpenMemory23 == nullptr) {
        if (error_msg) {
            *error_msg = fmt::format("DexFile::OpenMemory symbol not found, api level: {}", android_get_device_api_level());
        }
        return nullptr;
    }
    if (dex_file == nullptr || size <= 64) {
        if (error_msg) {
            *error_msg = "dex_file is invalid";
        }
        return nullptr;
    }
    // call the function
    if (location.empty()) {
        static int anonymous_dex_counter = 1;
        location = "anonymous-dex-" + std::to_string(anonymous_dex_counter++);
    }
    struct DexHeader {
        uint8_t magic_[8];
        uint32_t checksum_;
    };
    uint32_t checksum = reinterpret_cast<const DexHeader*>(dex_file)->checksum_;
    std::string dummy_error_msg;
    if (error_msg == nullptr) {
        // ART expects a valid string
        error_msg = &dummy_error_msg;
    }
    if (OpenMemory23 != nullptr) {
        MemMap* mem_map = nullptr;
        const OatDexFile* oat_dex_file = nullptr;
        auto dex = OpenMemory23(dex_file, size, location, checksum, mem_map, oat_dex_file, error_msg);
        return dex.release();
    }
    if (OpenMemory22 != nullptr) {
        MemMap* mem_map = nullptr;
        const OatFile* oat_file = nullptr;
        auto dex = OpenMemory22(dex_file, size, location, checksum, mem_map, oat_file, error_msg);
        return dex;
    }
    if (OpenMemory21 != nullptr) {
        MemMap* mem_map = nullptr;
        auto dex = OpenMemory21(dex_file, size, location, checksum, mem_map, error_msg);
        return dex;
    }
    *error_msg = "cannot find OpenMemory function";
    return nullptr;
}

jobject art::DexFile::ToJavaDexFile(JNIEnv* env) const {
    if (this == nullptr || env == nullptr) {
        return nullptr;
    }
    static std::once_flag once;
    static int sdk_int = android_get_device_api_level();
    static jclass dex_file_class = nullptr;
    static jfieldID cookie_field_j = nullptr; // below Android 6.0
    static jfieldID cookie_field_l = nullptr; // Android 6.0+
    static jfieldID internal_cookie_field = nullptr; // Android 7.0+
    static jfieldID file_name_field = nullptr;
    // see https://cs.android.com/android/platform/superproject/+/android-7.0.0_r1:art/runtime/native/dalvik_system_DexFile.cc;l=77
    static int kDexFileIndexStart = -1;
    std::call_once(once, [env] {
        jclass klass = env->FindClass("dalvik/system/DexFile");
        dex_file_class = reinterpret_cast<jclass>(env->NewGlobalRef(klass));
        if (sdk_int >= __ANDROID_API_M__) {
            cookie_field_l = env->GetFieldID(dex_file_class, "mCookie", "Ljava/lang/Object;");
            kDexFileIndexStart = 0;
        } else {
            cookie_field_j = env->GetFieldID(dex_file_class, "mCookie", "J");
            // kDexFileIndexStart not used
        }
        if (sdk_int >= __ANDROID_API_N__) {
            internal_cookie_field = env->GetFieldID(dex_file_class, "mInternalCookie", "Ljava/lang/Object;");
            kDexFileIndexStart = 1;
        }
        file_name_field = env->GetFieldID(dex_file_class, "mFileName", "Ljava/lang/String;");
        env->DeleteLocalRef(klass);
    });
    jobject dex_file = env->AllocObject(dex_file_class);
    if (sdk_int >= __ANDROID_API_M__) {
        // cookie is an long array
        std::vector<jlong> cookie_vector(kDexFileIndexStart + 1);
        if (kDexFileIndexStart == 1) {
            // oat = 0
            cookie_vector[0] = 0;
        }
        cookie_vector[kDexFileIndexStart] = reinterpret_cast<jlong>(this);
        // set the cookie
        jlongArray cookie = env->NewLongArray(cookie_vector.size());
        env->SetLongArrayRegion(cookie, 0, cookie_vector.size(), cookie_vector.data());
        env->SetObjectField(dex_file, cookie_field_l, cookie);
        if (internal_cookie_field) {
            env->SetObjectField(dex_file, internal_cookie_field, cookie);
        }
    } else {
        auto cookie = static_cast<jlong>(reinterpret_cast<uintptr_t>(new std::vector{this}));
        env->SetLongField(dex_file, cookie_field_j, cookie);
    }
    env->SetObjectField(dex_file, file_name_field, env->NewStringUTF(""));
    return dex_file;
}

}

extern "C" JNIEXPORT jobject JNICALL
Java_io_github_qauxv_util_dyn_MemoryDexLoader_nativeCreateClassLoaderWithDexBelowOreo(JNIEnv* env, jclass clazz, jbyteArray dex_file, jobject parent) {
    using namespace qauxv;
    // This method is only used for Android 8.0 and below.
    if (dex_file == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/NullPointerException"), "dex_file is null");
        return nullptr;
    }
    if (!InitLibArtElfView()) {
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "libart symbol resolver init failed");
        return nullptr;
    }
    jclass kPathClassLoader = env->FindClass("dalvik/system/PathClassLoader");
    jclass kBaseDexClassLoader = env->FindClass("dalvik/system/BaseDexClassLoader");
    jfieldID pathListField = env->GetFieldID(kBaseDexClassLoader, "pathList", "Ldalvik/system/DexPathList;");
    if (env->ExceptionCheck()) {
        return nullptr; // exception thrown
    }
    jclass kDexPathList = env->FindClass("dalvik/system/DexPathList");
    jfieldID dexElementsField = env->GetFieldID(kDexPathList, "dexElements", "[Ldalvik/system/DexPathList$Element;");
    if (env->ExceptionCheck()) {
        return nullptr; // exception thrown
    }
    jclass kElement = env->FindClass("dalvik/system/DexPathList$Element");
    if (env->ExceptionCheck()) {
        return nullptr; // exception thrown
    }
    jfieldID dexFileField = env->GetFieldID(kElement, "dexFile", "Ldalvik/system/DexFile;");
    if (env->ExceptionCheck()) {
        return nullptr; // exception thrown
    }
    jmethodID ctorPathClassLoader = env->GetMethodID(kPathClassLoader, "<init>", "(Ljava/lang/String;Ljava/lang/ClassLoader;)V");
    jclass kDexFile = env->FindClass("dalvik/system/DexFile");
    jclass kFile = env->FindClass("java/io/File");
    // public Element(File dir, boolean isDirectory, File zip, DexFile dexFile)
    jmethodID ctorElement = env->GetMethodID(kElement, "<init>", "(Ljava/io/File;ZLjava/io/File;Ldalvik/system/DexFile;)V");
    jmethodID ctorFile = env->GetMethodID(kFile, "<init>", "(Ljava/lang/String;)V");
    size_t dex_file_size = env->GetArrayLength(dex_file);
    if (dex_file_size == 0) {
        env->ThrowNew(env->FindClass("java/lang/IllegalAccessException"), "dex_file is empty");
        return nullptr;
    }
    std::vector<uint8_t> dex_file_data(dex_file_size);
    env->GetByteArrayRegion(dex_file, 0, dex_file_size, reinterpret_cast<jbyte*>(dex_file_data.data()));
    void* dex_file_ptr = mmap(nullptr, dex_file_size, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
    if (dex_file_ptr == MAP_FAILED) {
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), fmt::format("mmap failed: {}", strerror(errno)).c_str());
        return nullptr;
    }
    memcpy(dex_file_ptr, dex_file_data.data(), dex_file_size);
    // set read-only
    mprotect(dex_file_ptr, dex_file_size, PROT_READ);
    std::string err_msg;
    const auto* dex = qauxv::art::DexFile::OpenMemory(static_cast<const uint8_t*>(dex_file_ptr), dex_file_size, "qauxv-stub", &err_msg);
    if (!dex) {
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), fmt::format("DexFile::OpenMemory failed: {}", err_msg).c_str());
        return nullptr;
    }
    jstring emptyString = env->NewStringUTF("");
    auto java_dex_file = dex->ToJavaDexFile(env);
    jobject classloader = env->NewObject(kPathClassLoader, ctorPathClassLoader, emptyString, parent);
    jobject path_list = env->GetObjectField(classloader, pathListField);
    jobjectArray dex_elements = reinterpret_cast<jobjectArray>(env->GetObjectField(path_list, dexElementsField));
    jobject emptyFile = env->NewObject(kFile, ctorFile, emptyString);
    // Element(File(""), false, null, dexFile)
    jobject element = env->NewObject(kElement, ctorElement, emptyFile, false, nullptr, java_dex_file);
    if (env->ExceptionCheck()) {
        return nullptr;
    }
    // create a new element array with one element
    jobjectArray new_dex_elements = env->NewObjectArray(1, kElement, element);
    // set the array back to the dexElements field
    env->SetObjectField(path_list, dexElementsField, new_dex_elements);
    return classloader;
}

extern "C" JNIEXPORT jobject JNICALL
Java_io_github_qauxv_util_dyn_MemoryDexLoader_nativeCreateDexFileFormBytesBelowOreo(JNIEnv* env,
                                                                                    jclass clazz,
                                                                                    jbyteArray dex_bytes,
                                                                                    jobject defining_context,
                                                                                    jstring jstr_name) {
    using namespace qauxv;
    // This method is only used for Android 8.0 and below.
    if (dex_bytes == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/NullPointerException"), "dex_file is null");
        return nullptr;
    }
    if (!InitLibArtElfView()) {
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "libart symbol resolver init failed");
        return nullptr;
    }
    size_t dex_file_size = env->GetArrayLength(dex_bytes);
    if (dex_file_size == 0) {
        env->ThrowNew(env->FindClass("java/lang/IllegalAccessException"), "dex_file is empty");
        return nullptr;
    }
    std::vector<uint8_t> dex_file_data(dex_file_size);
    env->GetByteArrayRegion(dex_bytes, 0, (int) dex_file_size, reinterpret_cast<jbyte*>(dex_file_data.data()));
    void* dex_file_ptr = mmap(nullptr, dex_file_size, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
    if (dex_file_ptr == MAP_FAILED) {
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), fmt::format("mmap failed: {}", strerror(errno)).c_str());
        return nullptr;
    }
    memcpy(dex_file_ptr, dex_file_data.data(), dex_file_size);
    // set read-only
    mprotect(dex_file_ptr, dex_file_size, PROT_READ);
    std::string err_msg;
    const auto* dex = qauxv::art::DexFile::OpenMemory(static_cast<const uint8_t*>(dex_file_ptr), dex_file_size, "qauxv-stub", &err_msg);
    if (!dex) {
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), fmt::format("DexFile::OpenMemory failed: {}", err_msg).c_str());
        return nullptr;
    }
    auto java_dex_file = dex->ToJavaDexFile(env);
    if (env->ExceptionCheck()) {
        return nullptr; // exception thrown
    }
    return java_dex_file;
}

//@formatter:off
static JNINativeMethod gMethods[] = {
        {"nativeCreateClassLoaderWithDexBelowOreo", "([BLjava/lang/ClassLoader;)Ljava/lang/ClassLoader;", reinterpret_cast<void*>(Java_io_github_qauxv_util_dyn_MemoryDexLoader_nativeCreateClassLoaderWithDexBelowOreo)},
        {"nativeCreateDexFileFormBytesBelowOreo", "([BLjava/lang/ClassLoader;Ljava/lang/String;)Ldalvik/system/DexFile;", reinterpret_cast<void*>(Java_io_github_qauxv_util_dyn_MemoryDexLoader_nativeCreateDexFileFormBytesBelowOreo)},
};
//@formatter:on
REGISTER_PRIMARY_PRE_INIT_NATIVE_METHODS("io/github/qauxv/util/dyn/MemoryDexLoader", gMethods);
