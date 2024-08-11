//
// Created by sulfate on 2024-08-10.
//

#include "jni_method_registry.h"

#include <mutex>

#include <fmt/format.h>

#include "utils/JniUtils.h"

namespace qauxv::jniutil {

static std::vector<JniMethodInitMethodList> gPrimaryPreInitMethods;
static std::vector<JniMethodInitMethodList> gPrimaryFullInitMethods;
static std::vector<JniMethodInitMethodList> gSecondaryFullInitMethods;

const std::vector<JniMethodInitMethodList>& GetPrimaryPreInitMethods() {
    return gPrimaryPreInitMethods;
}

const std::vector<JniMethodInitMethodList>& GetPrimaryFullInitMethods() {
    return gPrimaryFullInitMethods;
}

const std::vector<JniMethodInitMethodList>& GetSecondaryFullInitMethods() {
    return gSecondaryFullInitMethods;
}

void RegisterJniLateInitMethod(JniMethodInitType type, const std::string& declare_class, const std::vector<JNINativeMethod>& methods) {
    switch (type) {
        case JniMethodInitType::kPrimaryPreInit:
            gPrimaryPreInitMethods.push_back({type, declare_class, methods});
            break;
        case JniMethodInitType::kPrimaryFullInit:
            gPrimaryFullInitMethods.push_back({type, declare_class, methods});
            break;
        case JniMethodInitType::kSecondaryFullInit:
            gSecondaryFullInitMethods.push_back({type, declare_class, methods});
            break;
        default:
            break;
    }
}

std::string NormalizeClassName(std::string_view name) {
    // replace / with .
    std::string result(name);
    for (char& c: result) {
        if (c == '/') {
            c = '.';
        }
    }
    return result;
}

/**
 * Register JNI methods to class loader. If any action failed, it will throw a runtime exception.
 * @param env  JNI environment.
 * @param type  the type of the JNI methods.
 * @param class_loader  the class loader object.
 */
void RegisterJniLateInitMethodsToClassLoader(JNIEnv* env, JniMethodInitType type, jobject class_loader) {
    using qauxv::ThrowIfNoPendingException;
    std::vector<JniMethodInitMethodList>* methods = nullptr;
    switch (type) {
        case JniMethodInitType::kPrimaryPreInit:
            methods = &gPrimaryPreInitMethods;
            break;
        case JniMethodInitType::kPrimaryFullInit:
            methods = &gPrimaryFullInitMethods;
            break;
        case JniMethodInitType::kSecondaryFullInit:
            methods = &gSecondaryFullInitMethods;
            break;
        default:
            break;
    }
    if (methods == nullptr) {
        return;
    }
    jclass kClassLoader = env->FindClass("java/lang/ClassLoader");
    // check if the class loader is an instance of ClassLoader
    if (!env->IsInstanceOf(class_loader, kClassLoader)) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "class_loader is not an instance of ClassLoader");
        return;
    }
    jmethodID kLoadClass = env->GetMethodID(kClassLoader, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
    if (kLoadClass == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/NoSuchMethodError"), "loadClass method not found");
        return;
    }
    // register the JNI methods
    for (const auto& method_list: *methods) {
        jstring class_name = env->NewStringUTF(NormalizeClassName(method_list.declare_class).c_str());
        auto klass = static_cast<jclass>(env->CallObjectMethod(class_loader, kLoadClass, class_name));
        if (env->ExceptionCheck() || klass == nullptr) {
            ThrowIfNoPendingException(env, "java/lang/NullPointerException", fmt::format("class {} not found", method_list.declare_class));
            return; // with exception
        }
        const JNINativeMethod* method_array = method_list.methods.data();
        if (env->RegisterNatives(klass, method_array, (jint) method_list.methods.size()) != JNI_OK) {
            ThrowIfNoPendingException(env, "java/lang/RuntimeException",
                                      fmt::format("RegisterNatives failed for class {}", method_list.declare_class));
            return; // with exception
        }
        env->DeleteLocalRef(class_name);
        env->DeleteLocalRef(klass);
    }
}

void RegisterJniMethodsCommon(JNIEnv* env, jobject class_loader, std::string_view klass, const std::vector<JNINativeMethod>& methods) {
    jclass kClassLoader = env->FindClass("java/lang/ClassLoader");
    // check if the class loader is an instance of ClassLoader
    if (!env->IsInstanceOf(class_loader, kClassLoader)) {
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "class_loader is not an instance of ClassLoader");
        return;
    }
    jmethodID kLoadClass = env->GetMethodID(kClassLoader, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
    if (kLoadClass == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/NoSuchMethodError"), "loadClass method not found");
        return;
    }
    jstring class_name = env->NewStringUTF(NormalizeClassName(klass).c_str());
    auto klass_obj = static_cast<jclass>(env->CallObjectMethod(class_loader, kLoadClass, class_name));
    if (env->ExceptionCheck() || klass_obj == nullptr) {
        ThrowIfNoPendingException(env, "java/lang/NullPointerException", fmt::format("class {} not found", klass));
        return; // with exception
    }
    const JNINativeMethod* method_array = methods.data();
    if (env->RegisterNatives(klass_obj, method_array, (jint) methods.size()) != JNI_OK) {
        ThrowIfNoPendingException(env, "java/lang/RuntimeException", fmt::format("RegisterNatives failed for class {}", klass));
        return; // with exception
    }
    env->DeleteLocalRef(class_name);
    env->DeleteLocalRef(klass_obj);
}

}
