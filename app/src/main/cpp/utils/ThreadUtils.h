//
// Created by sulfate on 2023-05-19.
//

#ifndef QAUXILIARY_THREADUTILS_H
#define QAUXILIARY_THREADUTILS_H

#include <cstdint>
#include <unwindstack/AndroidUnwinder.h>

#include "utils/Log.h"

namespace utils {

void DumpCurrentThreadStackTraceToLogcat(android_LogPriority priority);

void DumpThreadStackTraceToLogcat(uint32_t tid, android_LogPriority priority);

void DumpCurrentProcessWithUContext(void* uctx, android_LogPriority priority);

} // utils

#endif //QAUXILIARY_THREADUTILS_H
