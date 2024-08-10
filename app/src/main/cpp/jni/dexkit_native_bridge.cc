//
// Created by sulfate on 2024-08-10.
//

#include <jni.h>

#include "qauxv_core/jni_method_registry.h"

JNIEXPORT extern "C" jbyteArray Java_org_luckypray_dexkit_DexKitBridge_nativeBatchFindClassUsingStrings(JNIEnv* env, jclass klass, jlong j0, jbyteArray j1);
JNIEXPORT extern "C" jbyteArray Java_org_luckypray_dexkit_DexKitBridge_nativeBatchFindMethodUsingStrings(JNIEnv* env, jclass klass, jlong j0, jbyteArray j1);
JNIEXPORT extern "C" void Java_org_luckypray_dexkit_DexKitBridge_nativeExportDexFile(JNIEnv* env, jclass klass, jlong j0, jstring j1);
JNIEXPORT extern "C" jbyteArray Java_org_luckypray_dexkit_DexKitBridge_nativeFieldGetMethods(JNIEnv* env, jclass klass, jlong j0, jlong j1);
JNIEXPORT extern "C" jbyteArray Java_org_luckypray_dexkit_DexKitBridge_nativeFieldPutMethods(JNIEnv* env, jclass klass, jlong j0, jlong j1);
JNIEXPORT extern "C" jbyteArray Java_org_luckypray_dexkit_DexKitBridge_nativeFindClass(JNIEnv* env, jclass klass, jlong j0, jbyteArray j1);
JNIEXPORT extern "C" jbyteArray Java_org_luckypray_dexkit_DexKitBridge_nativeFindField(JNIEnv* env, jclass klass, jlong j0, jbyteArray j1);
JNIEXPORT extern "C" jbyteArray Java_org_luckypray_dexkit_DexKitBridge_nativeFindMethod(JNIEnv* env, jclass klass, jlong j0, jbyteArray j1);
JNIEXPORT extern "C" jbyteArray Java_org_luckypray_dexkit_DexKitBridge_nativeGetCallMethods(JNIEnv* env, jclass klass, jlong j0, jlong j1);
JNIEXPORT extern "C" jbyteArray Java_org_luckypray_dexkit_DexKitBridge_nativeGetClassAnnotations(JNIEnv* env, jclass klass, jlong j0, jlong j1);
JNIEXPORT extern "C" jbyteArray Java_org_luckypray_dexkit_DexKitBridge_nativeGetClassByIds(JNIEnv* env, jclass klass, jlong j0, jlongArray j1);
JNIEXPORT extern "C" jbyteArray Java_org_luckypray_dexkit_DexKitBridge_nativeGetClassData(JNIEnv* env, jclass klass, jlong j0, jstring j1);
JNIEXPORT extern "C" jint Java_org_luckypray_dexkit_DexKitBridge_nativeGetDexNum(JNIEnv* env, jclass klass, jlong j0);
JNIEXPORT extern "C" jbyteArray Java_org_luckypray_dexkit_DexKitBridge_nativeGetFieldAnnotations(JNIEnv* env, jclass klass, jlong j0, jlong j1);
JNIEXPORT extern "C" jbyteArray Java_org_luckypray_dexkit_DexKitBridge_nativeGetFieldByIds(JNIEnv* env, jclass klass, jlong j0, jlongArray j1);
JNIEXPORT extern "C" jbyteArray Java_org_luckypray_dexkit_DexKitBridge_nativeGetFieldData(JNIEnv* env, jclass klass, jlong j0, jstring j1);
JNIEXPORT extern "C" jbyteArray Java_org_luckypray_dexkit_DexKitBridge_nativeGetInvokeMethods(JNIEnv* env, jclass klass, jlong j0, jlong j1);
JNIEXPORT extern "C" jbyteArray Java_org_luckypray_dexkit_DexKitBridge_nativeGetMethodAnnotations(JNIEnv* env, jclass klass, jlong j0, jlong j1);
JNIEXPORT extern "C" jbyteArray Java_org_luckypray_dexkit_DexKitBridge_nativeGetMethodByIds(JNIEnv* env, jclass klass, jlong j0, jlongArray j1);
JNIEXPORT extern "C" jbyteArray Java_org_luckypray_dexkit_DexKitBridge_nativeGetMethodData(JNIEnv* env, jclass klass, jlong j0, jstring j1);
JNIEXPORT extern "C" jintArray Java_org_luckypray_dexkit_DexKitBridge_nativeGetMethodOpCodes(JNIEnv* env, jclass klass, jlong j0, jlong j1);
JNIEXPORT extern "C" jbyteArray Java_org_luckypray_dexkit_DexKitBridge_nativeGetMethodUsingFields(JNIEnv* env, jclass klass, jlong j0, jlong j1);
JNIEXPORT extern "C" jobjectArray Java_org_luckypray_dexkit_DexKitBridge_nativeGetMethodUsingStrings(JNIEnv* env, jclass klass, jlong j0, jlong j1);
JNIEXPORT extern "C" jbyteArray Java_org_luckypray_dexkit_DexKitBridge_nativeGetParameterAnnotations(JNIEnv* env, jclass klass, jlong j0, jlong j1);
JNIEXPORT extern "C" jobjectArray Java_org_luckypray_dexkit_DexKitBridge_nativeGetParameterNames(JNIEnv* env, jclass klass, jlong j0, jlong j1);
JNIEXPORT extern "C" jlong Java_org_luckypray_dexkit_DexKitBridge_nativeInitDexKit(JNIEnv* env, jclass klass, jstring j0);
JNIEXPORT extern "C" jlong Java_org_luckypray_dexkit_DexKitBridge_nativeInitDexKitByBytesArray(JNIEnv* env, jclass klass, jobjectArray j0);
JNIEXPORT extern "C" jlong Java_org_luckypray_dexkit_DexKitBridge_nativeInitDexKitByClassLoader(JNIEnv* env, jclass klass, jobject j0, jboolean j1);
JNIEXPORT extern "C" void Java_org_luckypray_dexkit_DexKitBridge_nativeInitFullCache(JNIEnv* env, jclass klass, jlong j0);
JNIEXPORT extern "C" void Java_org_luckypray_dexkit_DexKitBridge_nativeRelease(JNIEnv* env, jclass klass, jlong j0);
JNIEXPORT extern "C" void Java_org_luckypray_dexkit_DexKitBridge_nativeSetThreadNum(JNIEnv* env, jclass klass, jlong j0, jint j1);

//@formatter:off
static JNINativeMethod gMethods[] = {
        {"nativeBatchFindClassUsingStrings", "(J[B)[B", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeBatchFindClassUsingStrings)},
        {"nativeBatchFindMethodUsingStrings", "(J[B)[B", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeBatchFindMethodUsingStrings)},
        {"nativeExportDexFile", "(JLjava/lang/String;)V", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeExportDexFile)},
        {"nativeFieldGetMethods", "(JJ)[B", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeFieldGetMethods)},
        {"nativeFieldPutMethods", "(JJ)[B", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeFieldPutMethods)},
        {"nativeFindClass", "(J[B)[B", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeFindClass)},
        {"nativeFindField", "(J[B)[B", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeFindField)},
        {"nativeFindMethod", "(J[B)[B", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeFindMethod)},
        {"nativeGetCallMethods", "(JJ)[B", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeGetCallMethods)},
        {"nativeGetClassAnnotations", "(JJ)[B", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeGetClassAnnotations)},
        {"nativeGetClassByIds", "(J[J)[B", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeGetClassByIds)},
        {"nativeGetClassData", "(JLjava/lang/String;)[B", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeGetClassData)},
        {"nativeGetDexNum", "(J)I", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeGetDexNum)},
        {"nativeGetFieldAnnotations", "(JJ)[B", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeGetFieldAnnotations)},
        {"nativeGetFieldByIds", "(J[J)[B", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeGetFieldByIds)},
        {"nativeGetFieldData", "(JLjava/lang/String;)[B", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeGetFieldData)},
        {"nativeGetInvokeMethods", "(JJ)[B", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeGetInvokeMethods)},
        {"nativeGetMethodAnnotations", "(JJ)[B", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeGetMethodAnnotations)},
        {"nativeGetMethodByIds", "(J[J)[B", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeGetMethodByIds)},
        {"nativeGetMethodData", "(JLjava/lang/String;)[B", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeGetMethodData)},
        {"nativeGetMethodOpCodes", "(JJ)[I", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeGetMethodOpCodes)},
        {"nativeGetMethodUsingFields", "(JJ)[B", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeGetMethodUsingFields)},
        {"nativeGetMethodUsingStrings", "(JJ)[Ljava/lang/String;", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeGetMethodUsingStrings)},
        {"nativeGetParameterAnnotations", "(JJ)[B", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeGetParameterAnnotations)},
        {"nativeGetParameterNames", "(JJ)[Ljava/lang/String;", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeGetParameterNames)},
        {"nativeInitDexKit", "(Ljava/lang/String;)J", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeInitDexKit)},
        {"nativeInitDexKitByBytesArray", "([[B)J", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeInitDexKitByBytesArray)},
        {"nativeInitDexKitByClassLoader", "(Ljava/lang/ClassLoader;Z)J", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeInitDexKitByClassLoader)},
        {"nativeInitFullCache", "(J)V", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeInitFullCache)},
        {"nativeRelease", "(J)V", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeRelease)},
        {"nativeSetThreadNum", "(JI)V", reinterpret_cast<void*>(Java_org_luckypray_dexkit_DexKitBridge_nativeSetThreadNum)},

};
//@formatter:on
REGISTER_PRIMARY_PRE_INIT_NATIVE_METHODS("org/luckypray/dexkit/DexKitBridge", gMethods);
