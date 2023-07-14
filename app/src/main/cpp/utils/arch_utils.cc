//
// Created by sulfate on 2023-07-14.
//

#include "arch_utils.h"

#include <cstdlib>

#ifdef __aarch64__

extern "C" __attribute__((naked, visibility("default"))) void* call_func_with_x8(const void* func, void* x8, void* x0, void* x1, void* x2, void* x3) {
    __asm volatile (
            "mov x16, x0\n"
            "mov x8, x1\n"
            "mov x0, x2\n"
            "mov x1, x3\n"
            "mov x2, x4\n"
            "mov x3, x5\n"
            "br x16\n"
            );
}

#else // not __aarch64__

extern "C" void* call_func_with_x8(const void* func, void* x8, void* x0, void* x1, void* x2, void* x3) {
    // not implemented
    abort();
}

#endif // __aarch64__
