//
// Created by sulfate on 2023-05-19.
//

#include "ThreadUtils.h"

#include <unistd.h>

using namespace unwindstack;

namespace utils {

void DumpThreadStackTraceToLogcat(uint32_t tid, android_LogPriority priority) {
    AndroidUnwinderData data;
    AndroidLocalUnwinder unwinder;
    bool result = unwinder.Unwind(tid, data);
    if (!result) {
        LOGE("Unwind failed for tid: %d, error: %s", tid, data.GetErrorString().c_str());
        return;
    }
    for (const auto& frame: data.frames) {
        __android_log_print(priority, "QAuxv", "%s", unwinder.FormatFrame(frame).c_str());
    }
}

void DumpCurrentThreadStackTraceToLogcat(android_LogPriority priority) {
    DumpThreadStackTraceToLogcat(gettid(), priority);
}

} // utils
