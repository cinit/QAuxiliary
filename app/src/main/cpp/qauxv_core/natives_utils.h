#ifndef NATIVES_NATIVES_UTILS_H
#define NATIVES_NATIVES_UTILS_H

#include <jni.h>
#include <stdint.h>

#define EXPORT __attribute__((visibility("default")))

#define NOINLINE __attribute__((noinline))

typedef unsigned char uchar;

//Android is little endian, use pointer
inline uint32_t readLe32(uint8_t *buf, int index) {
    return *((uint32_t * )(buf + index));
}

inline uint32_t readLe16(uint8_t *buf, int off) {
    return *((uint16_t * )(buf + off));
}

inline int min(int a, int b) {
    return a > b ? b : a;
}

inline int max(int a, int b) {
    return a < b ? b : a;
}

extern "C" jint MMKV_JNI_OnLoad(JavaVM *vm, void *reserved);

#endif //NATIVES_NATIVES_UTILS_H
