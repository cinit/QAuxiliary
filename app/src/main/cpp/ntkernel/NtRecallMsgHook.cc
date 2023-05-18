//
// Created by sulfate on 2023-05-17.
//

#include "NtRecallMsgHook.h"

#include <optional>
#include <jni.h>
#include <unistd.h>
#include <cstring>

#include "qauxv_core/NativeCoreBridge.h"
#include "utils/Log.h"
#include "qauxv_core/HostInfo.h"
#include "utils/ProcessView.h"

namespace ntqq::hook {

using namespace qauxv;

static bool sIsHooked = false;

void (* gRecallGroupSysMsgCallback)(void*, void*, void*) = nullptr;
void (* gC2cGroupSysMsgCallback)(void*, void*, void*) = nullptr;

void HandleRecallGroupSysMsgCallback(void* p1, void* p2, void* p3) {
    LOGD("HandleRecallGroupSysMsgCallback, p1: %p, p2: %p, p3: %p", p1, p2, p3);
}

void HandleC2cGroupSysMsgCallback(void* p1, void* p2, void* p3) {
    LOGD("HandleC2cGroupSysMsgCallback, p1: %p, p2: %p, p3: %p", p1, p2, p3);
}

bool InitInitNtKernelRecallMsgHook() {
    using namespace utils;
    if (sIsHooked) {
        LOGW("InitInitNtKernelRecallMsgHook failed, already hooked");
        return false;
    }
    auto version = HostInfo::GetVersionCode32();
    uint64_t offsetGroup = 0;
    uint64_t offsetC2c = 0;
    switch (version) {
        case 4160: {
            offsetGroup = 0x1605904;
            offsetC2c = 0x1604234;
            break;
        }
        default: {
            LOGE("InitInitNtKernelRecallMsgHook failed, unsupported version: %u", version);
            return false;
        }
    }
    if (offsetC2c != 0 && offsetGroup != 0) {
        auto fnHookProc = [offsetGroup, offsetC2c, version](uint64_t baseAddress) {
            if (sIsHooked) {
                return false;
            }
            sIsHooked = true;
            void* c2c = (void*) (baseAddress + offsetC2c);
            void* group = (void*) (baseAddress + offsetGroup);
            if (CreateInlineHook(c2c, (void*) &HandleC2cGroupSysMsgCallback, (void**) &gC2cGroupSysMsgCallback) != 0) {
                LOGE("InitInitNtKernelRecallMsgHook failed, DobbyHook c2c failed");
                return false;
            }
            if (CreateInlineHook(group, (void*) &HandleRecallGroupSysMsgCallback, (void**) &gRecallGroupSysMsgCallback) != 0) {
                LOGE("InitInitNtKernelRecallMsgHook failed, DobbyHook group failed");
                return false;
            }
            LOGI("InitInitNtKernelRecallMsgHook success, version: %u", version);
            return true;
        };
        ProcessView self;
        if (int err;(err = self.readProcess(getpid())) != 0) {
            LOGE("InitInitNtKernelRecallMsgHook failed, readProcess failed: %d", err);
            return false;
        }
        std::optional<ProcessView::Module> libkernel;
        for (const auto& m: self.getModules()) {
            if (m.name == "libkernel.so") {
                libkernel = m;
                break;
            }
        }
        if (libkernel.has_value()) {
            // hook now
            return fnHookProc(libkernel->baseAddress);
        } else {
            RegisterLoadLibraryCallback([fnHookProc](const char* name, void* handle) {
                if (name == nullptr) {
                    return;
                }
                std::string soname;
                // get suffix
                auto suffix = strrchr(name, '/');
                if (suffix == nullptr) {
                    soname = name;
                } else {
                    soname = suffix + 1;
                }
                if (soname == "libkernel.so") {
                    // get base address
                    ProcessView self2;
                    if (int err;(err = self2.readProcess(getpid())) != 0) {
                        LOGE("InitInitNtKernelRecallMsgHook failed, readProcess failed: %d", err);
                        return;
                    }
                    std::optional<ProcessView::Module> libkernel2;
                    for (const auto& m: self2.getModules()) {
                        if (m.name == "libkernel.so") {
                            libkernel2 = m;
                            break;
                        }
                    }
                    if (libkernel2.has_value()) {
                        // hook now
                        if (!fnHookProc(libkernel2->baseAddress)) {
                            LOGE("InitInitNtKernelRecallMsgHook failed, fnHookProc failed");
                        }
                    } else {
                        LOGE("InitInitNtKernelRecallMsgHook failed, but it was loaded");
                    }
                }
            });
            return false;
        }
    } else {
        LOGE("InitInitNtKernelRecallMsgHook failed, offsetC2c: 0x%p, offsetGroup: 0x%p", (void*) offsetC2c, (void*) offsetGroup);
        return false;
    }
}

} // ntqq::hook

extern "C" JNIEXPORT jboolean JNICALL
Java_cc_ioctl_hook_msg_RevokeMsgHook_nativeInitNtKernelRecallMsgHook(JNIEnv* env, jobject thiz) {
    return ntqq::hook::InitInitNtKernelRecallMsgHook();
}
