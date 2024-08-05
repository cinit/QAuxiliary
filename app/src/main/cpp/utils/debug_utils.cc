//
// Created by sulfate on 2024-08-05.
//

#include "debug_utils.h"

#include <string_view>
#include <string>
#include <cstdint>
#include <vector>
#include <array>

#include <fmt/format.h>

#include <unistd.h>
#include <fcntl.h>
#include <cstdio>

namespace debugutil {

void DebugBreakIfDebuggerPresent() {
    if (IsDebuggerPresent()) {
        DebugBreak();
    }
}

bool IsDebuggerPresent() {
    // find TracerPid
    std::string statusPath = fmt::format("/proc/{}/status", getpid());
    FILE* statusFile = fopen(statusPath.c_str(), "r");
    if (!statusFile) {
        return false;
    }
    std::array<char, 1024> line = {};
    while (fgets(line.data(), line.size(), statusFile)) {
        if (std::string_view(line.data(), 9) == "TracerPid") {
            int tracerPid = 0;
            sscanf(line.data(), "TracerPid: %d", &tracerPid);
            if (tracerPid != 0) {
                fclose(statusFile);
                return true;
            }
            break;
        }
    }
    fclose(statusFile);
    return false;
}

}
