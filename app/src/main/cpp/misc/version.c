//
// Created by cinit on 2021-05-12.
//
#include <stdio.h>
#include <unistd.h>

#if defined(__LP64__)
const char so_interp[] __attribute__((used, section(".interp"), visibility("default"))) = "/system/bin/linker64";
_Static_assert(sizeof(void *) == 8, "sizeof(void *) != 8");
#else
const char so_interp[] __attribute__((used, section(".interp"), visibility("default"))) = "/system/bin/linker";
_Static_assert(sizeof(void *) == 4, "sizeof(void *) != 4");
#endif

#ifndef QAUXV_VERSION
#error Please define macro QAUXV_VERSION in CMakeList
#endif

__attribute__((used, noreturn, section(".entry_init")))
void __libqauxv_main(void) {
    printf("QAuxiliary libqauxv-core0.so version " QAUXV_VERSION ".\n"
           "Copyright (C) 2019-2023 QAuxiliary developers\n"
           "This software is distributed in the hope that it will be useful,\n"
           "but WITHOUT ANY WARRANTY; without even the implied warranty of\n"
           "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.\n");
    _exit(0);
}
