//
// Created by cinit on 2021-05-12.
//
#include <stdio.h>
#include <unistd.h>

#include "version.h"

#if defined(__LP64__)
const char so_interp[] __attribute__((used, section(".interp"), visibility("default"))) = "/system/bin/linker64";
_Static_assert(sizeof(void*) == 8, "sizeof(void *) != 8");
#else
const char so_interp[] __attribute__((used, section(".interp"), visibility("default"))) = "/system/bin/linker";
_Static_assert(sizeof(void*) == 4, "sizeof(void *) != 4");
#endif

__attribute__((used, noreturn, section(".entry_init")))
void __libqauxv_main(void) {
    puts(QAUXV_VERSION_STRING);
    _exit(0);
}
