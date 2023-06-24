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

#include "MemoryUtils.h"

#include <fcntl.h>
#include <sys/mman.h>
#include <sys/eventfd.h>
#include <unistd.h>
#include <sys/uio.h>

#include <algorithm>
#include <cinttypes>
#include <string>
#include <cerrno>
#include <regex>
#include <array>

#include "TextUtils.h"

namespace utils {

using namespace std;

size_t GetPageSize() {
    return static_cast<size_t>(sysconf(_SC_PAGESIZE));
}

std::vector<MemoryMapEntry> GetProcessMemoryMaps(uint32_t pid) {
    int fd;
    if (pid == 0) {
        fd = open("/proc/self/maps", O_RDONLY);
    } else {
        std::array<char, 64> path = {};
        snprintf(path.data(), path.size(), "/proc/%d/maps", pid);
        fd = open(path.data(), O_RDONLY);
    }
    if (fd == -1) {
        return {};
    }
    // read fully
    std::string content;
    std::array<char, 4096> buf = {};
    while (true) {
        auto n = read(fd, buf.data(), buf.size());
        if (n == -1) {
            close(fd);
            return {};
        }
        if (n == 0) {
            close(fd);
            break;
        }
        content.append(buf.data(), n);
    }
    // split
    vector<string> lines = SplitString(content, "\n");
    std::vector<MemoryMapEntry> maps;
    for (const auto& line: lines) {
        if (line.size() < 3) {
            continue;
        }
        MemoryMapEntry entry;
        // "5594a1416000-5594a1438000 r--p 00000000 103:06 584979298                 path
        // regex "^([0-9a-f]+)-([0-9a-f]+) ([rwxp-]{4}) ([0-9a-f]+) ([0-9a-f]+):([0-9a-f]+) ([0-9]+)\s*(.*)$"
        static const std::regex re(R"(([\da-f]+)-([\da-f]+) ([rwxp-]{4}) ([\da-f]+) ([\da-f]+):([\da-f]+) ([\d]+)\s*(.*))");
        std::smatch match;
        if (!std::regex_match(line, match, re)) {
            continue;
        }
        entry.start = static_cast<uint64_t>(std::stoull(match[1].str(), nullptr, 16));
        entry.end = static_cast<uint64_t>(std::stoull(match[2].str(), nullptr, 16));
        entry.protect = 0;
        if (match[3].str().find('r') != std::string::npos) {
            entry.protect |= PROT_READ;
        }
        if (match[3].str().find('w') != std::string::npos) {
            entry.protect |= PROT_WRITE;
        }
        if (match[3].str().find('x') != std::string::npos) {
            entry.protect |= PROT_EXEC;
        }
        entry.flags = 0;
        if (match[3].str().find('p') != std::string::npos) {
            entry.flags |= MAP_PRIVATE;
        }
        if (match[3].str().find('s') != std::string::npos) {
            entry.flags |= MAP_SHARED;
        }
        entry.offset = static_cast<uint64_t>(std::stoull(match[4].str(), nullptr, 16));
        entry.major = static_cast<uint32_t>(std::stoul(match[5].str(), nullptr, 16));
        entry.minor = static_cast<uint32_t>(std::stoul(match[6].str(), nullptr, 16));
        entry.inode = static_cast<uint32_t>(std::stoul(match[7].str(), nullptr, 10));
        entry.path = match[8];
        maps.push_back(entry);
    }
    return maps;
}

std::vector<uint32_t> GetMemoryProtects(const std::vector<MemoryMapEntry>& maps, const std::vector<uint64_t>& addresses) {
    auto pageSize = GetPageSize();
    // maps are ordered by start address
    std::vector<uint32_t> protects;
    for (auto address: addresses) {
        uint64_t alignedAddr = static_cast<uint64_t>(address) & ~(pageSize - 1);
        auto it = std::upper_bound(maps.begin(), maps.end(), alignedAddr,
                                   [](uint64_t addr, const MemoryMapEntry& entry) {
                                       return addr < entry.start;
                                   });
        if (it == maps.begin()) {
            protects.push_back(0);
            continue;
        }
        --it;
        if (alignedAddr >= it->end) {
            protects.push_back(0);
            continue;
        }
        protects.push_back(it->protect);
    }
    return protects;
}

bool CheckMemoryProtect(const std::vector<MemoryMapEntry>& maps, uint64_t ptr, size_t length, uint32_t protect) {
    auto pageSize = GetPageSize();
    auto startPage = reinterpret_cast<uint64_t>(ptr) & ~(pageSize - 1);
    auto endPage = (reinterpret_cast<uint64_t>(ptr) + length) & ~(pageSize - 1);
    auto pageCount = (endPage - startPage) / pageSize + 1u;
    std::vector<uint64_t> addresses;
    addresses.reserve(pageCount);
    for (uint64_t i = 0; i < pageCount; ++i) {
        addresses.push_back(startPage + i * pageSize);
    }
    auto protects = GetMemoryProtects(maps, addresses);
    return std::all_of(protects.begin(), protects.end(), [protect](uint32_t p) {
        return p & protect;
    });
}

bool IsMemoryReadable(const void* ptr, size_t length) {
    // transform into pages
    auto pageSize = GetPageSize();
    auto startPage = reinterpret_cast<uint64_t>(ptr) & ~(pageSize - 1);
    auto endPage = (reinterpret_cast<uint64_t>(ptr) + length) & ~(pageSize - 1);
    auto pageCount = (endPage - startPage) / pageSize + 1u;
    std::vector<uint64_t> addresses;
    addresses.reserve(pageCount);
    for (uint64_t i = 0; i < pageCount; ++i) {
        addresses.push_back(startPage + i * pageSize);
    }
    // use IsPageReadable to check each page
    return std::all_of(addresses.begin(), addresses.end(), [](uint64_t addr) {
        return IsPageReadable(reinterpret_cast<void*>(addr));
    });
}

bool IsPageReadable(const void* ptr) {
    uint64_t alignedAddr = reinterpret_cast<uint64_t>(ptr) & ~(GetPageSize() - 1);
    // use process_vm_readv to try first word of each page
    uint64_t buffer;
    iovec local;
    iovec remote;
    local.iov_base = &buffer;
    local.iov_len = sizeof(uint64_t);
    remote.iov_base = reinterpret_cast<void*>(alignedAddr);
    remote.iov_len = sizeof(uint64_t);
    errno = 0;
    auto size = process_vm_readv(getpid(), &local, 1, &remote, 1, 0);
    return size == sizeof(uint64_t);
}

bool IsMemoryReadable(const std::vector<MemoryMapEntry>& maps, uint64_t ptr, size_t length) {
    return CheckMemoryProtect(maps, ptr, length, PROT_READ);
}

bool IsMemoryWritable(const std::vector<MemoryMapEntry>& maps, uint64_t ptr, size_t length) {
    return CheckMemoryProtect(maps, ptr, length, PROT_WRITE);
}

bool IsMemoryExecutable(const std::vector<MemoryMapEntry>& maps, uint64_t ptr, size_t length) {
    return CheckMemoryProtect(maps, ptr, length, PROT_EXEC);
}

int GetEntryIndex(const std::vector<MemoryMapEntry>& maps, uint64_t addr) {
    for (int i = 0; i < maps.size(); i++) {
        if (maps[i].start <= addr && addr < maps[i].end) {
            return i;
        }
    }
    return -1;
}

}
