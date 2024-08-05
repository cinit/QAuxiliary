//
// Created by sulfate on 2024-08-05.
//

#ifndef QAUXV_DEBUG_UTILS_H
#define QAUXV_DEBUG_UTILS_H

namespace debugutil {

inline void DebugBreak() {
    __builtin_trap();
}

bool IsDebuggerPresent();

void DebugBreakIfDebuggerPresent();

}

#endif //QAUXV_DEBUG_UTILS_H
