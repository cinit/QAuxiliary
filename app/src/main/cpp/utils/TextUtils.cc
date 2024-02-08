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

#include "TextUtils.h"

#include <fmt/format.h>

namespace utils {

std::vector<std::string> SplitString(std::string_view str, std::string_view delimiter) {
    std::vector<std::string> result;
    size_t pos = 0;
    while ((pos = str.find(delimiter)) != std::string::npos) {
        result.emplace_back(str.substr(0, pos));
        str.remove_prefix(pos + delimiter.length());
    }
    result.emplace_back(str);
    return result;
}

inline bool IsPrintableAscii(char c) noexcept {
    return c >= 0x20 && c <= 0x7E;
}

std::string LastPartOf(std::string_view str, std::string_view delimiter) {
    auto parts = SplitString(str, delimiter);
    return parts[parts.size() - 1];
}

std::string bytes2hex(std::span<const uint8_t> bytes) {
    if (bytes.empty()) {
        return {};
    }
    std::string result;
    result.reserve(bytes.size() * 2);
    for (uint8_t byte: bytes) {
        result += fmt::format("{:02x}", byte);
    }
    return result;
}

}
