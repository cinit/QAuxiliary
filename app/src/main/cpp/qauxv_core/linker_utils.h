//
// Created by sulfate on 2024-07-11.
//

#ifndef QAUXV_LINKER_UTILS_H
#define QAUXV_LINKER_UTILS_H

#include <android/dlext.h>

namespace qauxv {

void* loader_android_dlopen_ext(const char* filename,
                                int flag,
                                const android_dlextinfo* extinfo,
                                const void* caller_addr);

void* loader_dlopen(const char* filename, int flag, const void* caller_addr);

}

#endif //QAUXV_LINKER_UTILS_H
