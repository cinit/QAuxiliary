//
// Created by sulfate on 2023-07-21.
//

#include <cstdint>
#include <vector>
#include <mutex>
#include <string>
#include <jni.h>
#include <unistd.h>
#include <sys/mman.h>
#include <cerrno>

#include <dobby.h>
#include <fmt/format.h>

#include "qauxv_core/NativeCoreBridge.h"
#include "utils/FileMemMap.h"
#include "utils/ProcessView.h"
#include "utils/ElfView.h"

#include "art/runtime/dex_file.hpp"

// undefine macros from logging.hpp
#undef LOGD
#undef LOGI
#undef LOGW
#undef LOGE
#undef LOG_TAG

#include "utils/Log.h"

namespace qauxv {

static FileMemMap sLibArtFileMap;
static utils::ElfView sLibArtElfView;
static std::mutex sLibArtViewInitMutex;
static void* sLibArtBaseAddress = nullptr;

std::string GetLibArtPath() {
    utils::ProcessView processView;
    if (processView.readProcess(getpid()) != 0) {
        return {};
    }
    for (const auto& m: processView.getModules()) {
        if (m.name == "libart.so") {
            sLibArtBaseAddress = reinterpret_cast<void*>(m.baseAddress);
            return m.path;
        }
    }
    return {};
}

bool InitLibArtElfView() {
    if (sLibArtElfView.IsValid()) {
        return true;
    }
    std::lock_guard<std::mutex> lock(sLibArtViewInitMutex);
    auto path = GetLibArtPath();
    if (path.empty()) {
        return false;
    }
    if (sLibArtFileMap.mapFilePath(path.c_str()) != 0) {
        return false;
    }
    sLibArtElfView.AttachFileMemMapping(sLibArtFileMap.getAddress(), sLibArtFileMap.getLength());
    return sLibArtElfView.IsValid();
}

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
                    auto offset = sLibArtElfView.GetSymbolOffset(symbol);
                    void* result;
                    if (offset != 0) {
                        result = reinterpret_cast<void*>(reinterpret_cast<uintptr_t>(sLibArtBaseAddress) + offset);
                    } else {
                        result = nullptr;
                        LOGD("art symbol exact '{}' not found", symbol);
                    }
                    return result;
                },
                .art_symbol_prefix_resolver = [](auto symbol) {
                    auto offset = sLibArtElfView.GetFirstSymbolOffsetWithPrefix(symbol);
                    void* result;
                    if (offset != 0) {
                        result = reinterpret_cast<void*>(reinterpret_cast<uintptr_t>(sLibArtBaseAddress) + offset);
                    } else {
                        result = nullptr;
                        LOGD("art symbol prefix '{}' not found", symbol);
                    }
                    return result;
                }
        };
        return ::lsplant::Init(env, sLSPlantInitInfo);
    };
    static bool sLSPlantInitializeResult = initProc();
    return sLSPlantInitializeResult;
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
    if (!InitLSPlantImpl(env)) {
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "lsplant init failed");
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
    const auto* dex = lsplant::art::DexFile::OpenMemory(dex_file_ptr, dex_file_size, "qauxv-stub", &err_msg);
    if (!dex) {
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), fmt::format("DexFile::OpenMemory failed: {}", err_msg).c_str());
        return nullptr;
    }
    jstring emptyString = env->NewStringUTF("");
    auto java_dex_file = lsplant::WrapScope(env, dex->ToJavaDexFile(env));
    jobject classloader = env->NewObject(kPathClassLoader, ctorPathClassLoader, emptyString, parent);
    jobject path_list = env->GetObjectField(classloader, pathListField);
    jobjectArray dex_elements = reinterpret_cast<jobjectArray>(env->GetObjectField(path_list, dexElementsField));
    jobject emptyFile = env->NewObject(kFile, ctorFile, emptyString);
    // Element(File(""), false, null, dexFile)
    jobject element = env->NewObject(kElement, ctorElement, emptyFile, false, nullptr, java_dex_file.get());
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
    if (!InitLSPlantImpl(env)) {
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "lsplant init failed");
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
    const auto* dex = lsplant::art::DexFile::OpenMemory(dex_file_ptr, dex_file_size, "qauxv-stub", &err_msg);
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
