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

#ifndef QAUXV_JNIUTILS_H
#define QAUXV_JNIUTILS_H

#include <cstdint>
#include <string>
#include <string_view>
#include <optional>
#include <jni.h>


namespace qauxv {

namespace ExceptionNames {

constexpr auto kRuntimeException = "java/lang/RuntimeException";
constexpr auto kIllegalArgumentException = "java/lang/IllegalArgumentException";
constexpr auto kIllegalStateException = "java/lang/IllegalStateException";
constexpr auto kNullPointerException = "java/lang/NullPointerException";

}

std::optional<std::string> JstringToString(JNIEnv* env, jstring jstr);

void ThrowIfNoPendingException(JNIEnv* env, const char* klass, std::string_view msg);

}

#endif //QAUXV_JNIUTILS_H
