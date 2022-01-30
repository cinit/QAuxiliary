//
// Created by kinit on 5/6/21.
//

#include "NativeHookEntry.h"
#include "natives_utils.h"

static NativeHookHandle nativeHookHandle = {};

EXPORT NativeOnModuleLoaded native_init(const NativeAPIEntries *entries) {
    nativeHookHandle.hookFunction = entries->hookFunc;
    return handleLoadLibrary;
}

NativeHookHandle *GetOrInitNativeHookHandle() {
    if (nativeHookHandle.hookFunction != nullptr) {
        return &nativeHookHandle;
    } else {
        // TODO: carry a few native hook framework if Xposed framework doesn't have one
        return nullptr;
    }
}

NativeHookHandle *GetNativeHookHandle() {
    if (nativeHookHandle.hookFunction != nullptr) {
        return &nativeHookHandle;
    } else {
        return nullptr;
    }
}

bool IsNativeHookHandleAvailable() {
    return GetNativeHookHandle() != nullptr;
}
