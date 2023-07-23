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
// Created by sulfate on 2023-5-27.
//

#ifndef QAUXV_MEMORYUTILS_H
#define QAUXV_MEMORYUTILS_H

#include <cstdint>
#include <vector>
#include <string>

namespace utils {

struct MemoryMapEntry {
    uint64_t start;
    uint64_t end;
    uint32_t protect;
    uint32_t flags;
    uint64_t offset;
    uint32_t major;
    uint32_t minor;
    uint64_t inode;
    std::string path;
};

std::vector<MemoryMapEntry> GetProcessMemoryMaps(uint32_t pid = 0);

std::vector<uint32_t> GetMemoryProtects(const std::vector<MemoryMapEntry>& maps, const std::vector<uint64_t>& addresses);

bool CheckMemoryProtect(const std::vector<MemoryMapEntry>& maps, uint64_t ptr, size_t length, uint32_t protect);

bool IsMemoryReadable(const void* ptr, size_t length = 1);

bool IsPageReadable(const void* ptr);

bool IsMemoryReadable(const std::vector<MemoryMapEntry>& maps, uint64_t ptr, size_t length = 1);

bool IsMemoryWritable(const std::vector<MemoryMapEntry>& maps, uint64_t ptr, size_t length = 1);

bool IsMemoryExecutable(const std::vector<MemoryMapEntry>& maps, uint64_t ptr, size_t length = 1);

size_t GetPageSize();

int GetEntryIndex(const std::vector<MemoryMapEntry>& maps, uint64_t addr);

}

#endif //QAUXV_MEMORYUTILS_H
