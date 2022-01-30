//
// Created by kinit on 5/6/21.
//
#include <cstdlib>
#include <android/log.h>

#include "NativeMainHook.h"
#include "NativeHookEntry.h"
#include "ConcurrentHashSet.h"

#include "dlfcn.h"
#include "natives_utils.h"

static bool nt_h_inited = false;

static ConcurrentHashSet<int> *gOpenFdSelfMaps = nullptr;
static ConcurrentHashSet<int> *gOpenFdSelfSmaps = nullptr;

bool NativeHook_initOnce() {
    if (nt_h_inited) {
        return true;
    }
    NativeHookHandle *h = GetOrInitNativeHookHandle();
    if (h == nullptr) {
        __android_log_print(ANDROID_LOG_DEBUG, "QAuxv",
                            "GetOrInitNativeHookHandle() is null\n");
        return false;
    }
    __android_log_print(ANDROID_LOG_DEBUG, "QAuxv",
                        "hookProc=%p\n", h->hookFunction);
    // init native hook start
    gOpenFdSelfMaps = new ConcurrentHashSet<int>;
    gOpenFdSelfSmaps = new ConcurrentHashSet<int>;
    // TODO: hook open(/proc/self/maps)
    // TODO: hook stat
    nt_h_inited = true;
    return true;
}


bool NativeHook_isInited() {
    return nt_h_inited;
}

void handleLoadLibrary(const char *name, void *handle) {
    if (!nt_h_inited) {
        return;
    }
    // TODO: implement this func
}

static int libc_hook = false;

EXPORT int hook_libc_open() {
    if (!nt_h_inited) {
        return 3;
    }
    if (libc_hook) {
        return 0x10;
    }
    void *libc = dlopen("libc.so", RTLD_NOLOAD);
    if (libc == nullptr) {
        return 1;
    }
    void *popen = dlsym(libc, "open");
    void *pread = dlsym(libc, "read");
    void *pclose = dlsym(libc, "close");
    if (!popen && !pread && !pclose) {
        return 3;
    }

    libc_hook = true;
    return 0;
}
