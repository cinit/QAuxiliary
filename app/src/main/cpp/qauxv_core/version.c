//
// Created by cinit on 2021-05-12.
//
#include <stdio.h>
#include <unistd.h>

#if defined(__aarch64__) || defined(__x86_64__)
const char so_interp[] __attribute__((used, section(".interp"))) = "/system/bin/linker64";
#elif defined(__i386__) || defined(__arm__)
const char so_interp[] __attribute__((used, section(".interp"))) = "/system/bin/linker";
#else
#error Unknown Arch
#endif

#ifndef QAUXV_VERSION
#error Please define macro QAUXV_VERSION in CMakeList
#endif

__attribute__((used, noreturn, section(".entry_init")))
void __libqauxv_main(void) {
    printf("QAuxiliary libqauxv.so version " QAUXV_VERSION ".\n"
           "Copyright (C) 2019-2022 qwq233@qwq2333.top\n"
           "This software is distributed in the hope that it will be useful,\n"
           "but WITHOUT ANY WARRANTY; without even the implied warranty of\n"
           "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.\n");
    _exit(0);
}
