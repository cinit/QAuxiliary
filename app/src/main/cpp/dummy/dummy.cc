//
// Created by sulfate on 2024-08-16.
//

#include <jni.h>

extern "C"
JNIEXPORT jint JNI_OnLoad([[maybe_unused]] JavaVM* vm, [[maybe_unused]] void* reserved) {
    // placeholder symbol to make this shared exists
    return JNI_VERSION_1_6;
}
