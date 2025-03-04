// QAuxiliary - An Xposed module for QQ/TIM
// Copyright (C) 2019-2023 QAuxiliary developers
// https://github.com/cinit/QAuxiliary
//
// This software is non-free but opensource software: you can redistribute it
// and/or modify it under the terms of the GNU Affero General Public License
// as published by the Free Software Foundation; either
// version 3 of the License, or any later version and our eula as published
// by QAuxiliary contributors.
//
// This software is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// and eula along with this software.  If not, see
// <https://www.gnu.org/licenses/>
// <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.

//
// Created by sulfate on 2023-05-18.
//

#ifndef QAUXV_NATIVECOREBRIDGE_H
#define QAUXV_NATIVECOREBRIDGE_H

#include <cstdint>
#include <functional>
#include <string_view>
#include <jni.h>
#include <fmt/format.h>

#include "lsp_native_api.h"

namespace qauxv {

// keep in sync with NativeLoader.java
enum class NativeLibraryInitMode {
    kNone = 0,
    kPrimaryOnly = 1,
    kSecondaryOnly = 2,
    kBothPrimaryAndSecondary = 3,
};


using LoadLibraryCallback = std::function<void(const char* name, void* handle)>;

int RegisterLoadLibraryCallback(const LoadLibraryCallback& callback);

// void UnregisterLoadLibraryCallback(const LoadLibraryCallback& callback);

int CreateInlineHook(void* func, void* replace, void** backup);

int DestroyInlineHook(void* func);

void InitializeNativeHookApi(bool allowHookLinker);

bool IsNativeHookApiInitialized();

NativeLibraryInitMode GetCurrentNativeLibraryInitMode();

void SetCurrentNativeLibraryInitMode(NativeLibraryInitMode mode);

/**
 * Add some error message to the error list.
 * @param env  JNI environment, optional, may be nullptr.
 * @param thiz  the ITraceableDynamicHook object which owns the error list.
 * @param errMsg  error message.
 */
void TraceError(JNIEnv* env, jobject thiz, std::string_view errMsg);

/**
 * Add some error message to the error list.
 * @param env  JNI environment, optional, may be nullptr.
 * @param thiz  the ITraceableDynamicHook object which owns the error list.
 * @param fmt  error message format.
 * @param args  error message format arguments.
 */
template<typename... T>
static inline void TraceErrorF(JNIEnv* env, jobject thiz, ::fmt::format_string<T...> fmt, T&& ... args) {
    TraceError(env, thiz, ::fmt::format(fmt, std::forward<T>(args)...));
}

}

extern "C" void ChainLoaderCallTargetNativeInit(NativeInit func);

#endif //QAUXV_NATIVECOREBRIDGE_H
