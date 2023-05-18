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

#ifndef QAUXV_MEMORYBUFFER_H
#define QAUXV_MEMORYBUFFER_H

#include <cstddef>

namespace utils {

class MemoryBuffer {
public:
    const void* address = nullptr;
    size_t length = 0;

    MemoryBuffer() = default;

    inline MemoryBuffer(const void* addr, size_t len) : address(addr), length(len) {}

    template<class T>
    inline T* at(size_t offset) noexcept {
        if (offset + sizeof(T) > length) {
            return nullptr;
        } else {
            return (T*) (((char*) address) + offset);
        }
    }

    template<class T>
    inline const T* at(size_t offset) const noexcept {
        if (offset + sizeof(T) > length) {
            return nullptr;
        } else {
            return (const T*) (((const char*) address) + offset);
        }
    }

    template<class T>
    inline bool access(size_t offset) const noexcept {
        if (offset + sizeof(T) > length) {
            return false;
        } else {
            return true;
        }
    }
};

}

#endif //QAUXV_MEMORYBUFFER_H
