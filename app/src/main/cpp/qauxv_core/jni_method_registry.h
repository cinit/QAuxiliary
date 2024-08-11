//
// Created by sulfate on 2024-08-10.
//

#ifndef QAUXV_JNI_METHOD_REGISTRY_H
#define QAUXV_JNI_METHOD_REGISTRY_H

#include <cstdint>
#include <array>
#include <string>
#include <vector>

#include <jni.h>

namespace qauxv::jniutil {

enum class JniMethodInitType {
    kNone = 0,
    kPrimaryPreInit = 1,
    kPrimaryFullInit = 2,
    kSecondaryFullInit = 4,
};

struct JniMethodInitMethodList {
    JniMethodInitType type;
    std::string declare_class;
    std::vector<JNINativeMethod> methods;
};

const std::vector<JniMethodInitMethodList>& GetPrimaryPreInitMethods();
const std::vector<JniMethodInitMethodList>& GetPrimaryFullInitMethods();
const std::vector<JniMethodInitMethodList>& GetSecondaryFullInitMethods();

void RegisterJniLateInitMethod(JniMethodInitType type, const std::string& declare_class, const std::vector<JNINativeMethod>& methods);

template<int Size>
void RegisterJniLateInitMethod(JniMethodInitType type, const char* declare_class, JNINativeMethod (& methods)[Size]) {
    RegisterJniLateInitMethod(type, declare_class, std::vector<JNINativeMethod>(methods, methods + Size));
}

/**
 * Register JNI methods to class loader. If any action failed, it will throw a runtime exception.
 * @param env  JNI environment.
 * @param type  the type of the JNI methods.
 * @param class_loader  the class loader object.
 */
void RegisterJniLateInitMethodsToClassLoader(JNIEnv* env, JniMethodInitType type, jobject class_loader);

void RegisterJniMethodsCommon(JNIEnv* env, jobject class_loader, std::string_view klass, const std::vector<JNINativeMethod>& methods);

} // qauxv::jniutil

#define REGISTER_PRIMARY_PRE_INIT_NATIVE_METHODS(DECLARE_CLASS, METHOD_ARRAY) \
__attribute__((constructor)) \
static void _LocalRegisterPrimaryPreInitNativeMethods() { \
    qauxv::jniutil::RegisterJniLateInitMethod(qauxv::jniutil::JniMethodInitType::kPrimaryPreInit, DECLARE_CLASS, METHOD_ARRAY); \
} static_assert(true, "")                                                            \

#define REGISTER_PRIMARY_FULL_INIT_NATIVE_METHODS(DECLARE_CLASS, METHOD_ARRAY) \
__attribute__((constructor))                                                   \
static void _LocalRegisterPrimaryFullInitNativeMethods() {                     \
    qauxv::jniutil::RegisterJniLateInitMethod(qauxv::jniutil::JniMethodInitType::kPrimaryFullInit, DECLARE_CLASS, METHOD_ARRAY); \
} static_assert(true, "")                                                       \

#define REGISTER_SECONDARY_FULL_INIT_NATIVE_METHODS(DECLARE_CLASS, METHOD_ARRAY) \
__attribute__((constructor))                                                     \
static void _LocalRegisterSecondaryFullInitNativeMethods() {                     \
    qauxv::jniutil::RegisterJniLateInitMethod(qauxv::jniutil::JniMethodInitType::kSecondaryFullInit, DECLARE_CLASS, METHOD_ARRAY); \
} static_assert(true, "")                                                        \

#endif //QAUXV_JNI_METHOD_REGISTRY_H
