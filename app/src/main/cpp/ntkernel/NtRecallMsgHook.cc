//
// Created by sulfate on 2023-05-17.
//

#include "NtRecallMsgHook.h"

#include <optional>
#include <cstdint>
#include <cinttypes>
#include <array>
#include <jni.h>
#include <unistd.h>
#include <cstring>
#include <string>
#include <span>
#include <optional>
#include <sys/mman.h>
#include <ucontext.h>
#include <dlfcn.h>
#include <type_traits>
//#include <chrono>
#include <unordered_map>
#include <memory>
#include <unordered_set>
#include <fmt/format.h>

#include "qauxv_core/NativeCoreBridge.h"
#include "utils/Log.h"
#include "qauxv_core/HostInfo.h"
#include "utils/ProcessView.h"
#include "utils/ThreadUtils.h"
#include "utils/TextUtils.h"
#include "utils/AobScanUtils.h"
#include "utils/MemoryUtils.h"
#include "utils/arch_utils.h"
#include "utils/endian.h"
#include "qauxv_core/natives_utils.h"
#include "qauxv_core/linker_utils.h"
#include "qauxv_core/jni_method_registry.h"

#ifndef STACK_GUARD
// for debug purpose only
#define STACK_GUARD ((void) 0)
#endif

namespace ntqq::hook {

using namespace qauxv;
using namespace ::utils;

static bool sIsHooked = false;

EXPORT extern "C" void* gLibkernelBaseAddress = nullptr;

jclass klassRevokeMsgHook = nullptr;
jobject gInstanceRevokeMsgHook = nullptr;
jmethodID handleRecallSysMsgFromNtKernel = nullptr;

void (* sOriginHandleGroupRecallSysMsgCallback)(void*, void*, void*) = nullptr;

void HandleGroupRecallSysMsgCallback([[maybe_unused]] void* x0, void* x1, [[maybe_unused]] void* x2, [[maybe_unused]] int x3) {
    // LOGD("HandleGroupRecallSysMsgCallback start p1={:p}, p2={:p}, p3={:p}", x0, x1, x2);
}

void (* sOriginHandleC2cRecallSysMsgCallback)(void*, void*, void*) = nullptr;

void HandleC2cRecallSysMsgCallback([[maybe_unused]] void* p1, [[maybe_unused]] void* p2, void* p3, [[maybe_unused]] int x3) {
    if (p3 == nullptr || *(void**) p3 == nullptr) {
        LOGE("HandleC2cGroupSysMsgCallback BUG !!! *p3 = null, this should not happen!!!");
        return;
    }
    // LOGD("HandleC2cRecallSysMsgCallback start p1={:p}, p2={:p}, p3={:p}", p1, p2, p3);
}

// Nobody uses PaiYiPai, right?
bool PerformNtRecallMsgHook(uint64_t baseAddress) {
    if (sIsHooked) {
        return false;
    }
    sIsHooked = true;
    gLibkernelBaseAddress = reinterpret_cast<void*>(baseAddress);

    //@formatter:off
    // RecallC2cSysMsg 09 8d 40 f8 ?? 03 00 aa 21 00 80 52 f3 03 02 aa 29 ?? 40 f9
    auto targetRecallC2cSysMsg = AobScanTarget()
            .WithName("RecallC2cSysMsg")
            .WithSequence({0x09, 0x8d, 0x40, 0xf8, 0xf6, 0x03, 0x00, 0xaa, 0x21, 0x00, 0x80, 0x52, 0xf3, 0x03, 0x02, 0xaa, 0x29, 0x00, 0x40, 0xf9})
            .WithMask(    {0xff, 0xff, 0xff, 0xff, 0x00, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x00, 0xff, 0xff})
            .WithStep(4)
            .WithExecMemOnly(true)
            .WithOffsetsForResult({-0x20, -0x24, -0x28})
            .WithResultValidator(CommonAobScanValidator::kArm64StpX29X30SpImm);

    // RecallGroupSysMsg 28 00 40 f9 61 00 80 52 09 8d 40 f8 29 !! 40 f9
    auto targetRecallGroupSysMsg = AobScanTarget()
            .WithName("RecallGroupSysMsg")
            .WithSequence({0x28, 0x00, 0x40, 0xf9, 0x61, 0x00, 0x80, 0x52, 0x09, 0x8d, 0x40, 0xf8, 0x29, 0x00, 0x40, 0xf9})
            .WithMask(    {0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x00, 0xff, 0xff})
            .WithStep(4)
            .WithExecMemOnly(true)
            .WithOffsetsForResult({-0x18, -0x24, -0x28})
            .WithResultValidator(CommonAobScanValidator::kArm64StpX29X30SpImm);

    //@formatter:on

    std::vector<std::string> errorMsgList;
    // auto start = std::chrono::steady_clock::now();
    if (!SearchForAllAobScanTargets({&targetRecallC2cSysMsg, &targetRecallGroupSysMsg}, gLibkernelBaseAddress, true, errorMsgList)) {
        LOGE("InitInitNtKernelRecallMsgHook SearchForAllAobScanTargets failed");
        // sth went wrong
        for (const auto& msg: errorMsgList) {
            // report error to UI somehow
            TraceError(nullptr, gInstanceRevokeMsgHook, msg);
        }
        return false;
    }

    // auto end = std::chrono::steady_clock::now();
    // LOGD("InitInitNtKernelRecallMsgHook AobScan elapsed: {}us", std::chrono::duration_cast<std::chrono::microseconds>(end - start).count());

    uint64_t offsetC2c = targetRecallC2cSysMsg.GetResultOffset();
    uint64_t offsetGroup = targetRecallGroupSysMsg.GetResultOffset();

    // LOGD("offsetC2c={:x}, offsetGroup={:x}", offsetC2c, offsetGroup);

    if (offsetC2c != 0) {
        void* c2c = (void*) (baseAddress + offsetC2c);
        if (CreateInlineHook(c2c, (void*) &HandleC2cRecallSysMsgCallback, (void**) &sOriginHandleC2cRecallSysMsgCallback) != 0) {
            TraceErrorF(nullptr, gInstanceRevokeMsgHook,
                        "InitInitNtKernelRecallMsgHook failed, DobbyHook c2c failed, c2c={:p}({:x}+{:x})",
                        c2c, baseAddress, offsetC2c);
            return false;
        }
    } else {
        TraceErrorF(nullptr, gInstanceRevokeMsgHook, "InitInitNtKernelRecallMsgHook failed, offsetC2c == 0");
    }
    if (offsetGroup != 0) {
        void* group = (void*) (baseAddress + offsetGroup);
        if (CreateInlineHook(group, (void*) &HandleGroupRecallSysMsgCallback, (void**) &sOriginHandleGroupRecallSysMsgCallback) != 0) {
            TraceErrorF(nullptr, gInstanceRevokeMsgHook,
                        "InitInitNtKernelRecallMsgHook failed, DobbyHook group failed, group={:p}({:x}+{:x})",
                        group, baseAddress, offsetGroup);
            return false;
        }
    } else {
        TraceErrorF(nullptr, gInstanceRevokeMsgHook, "InitInitNtKernelRecallMsgHook failed, offsetGroup == 0");
    }
    return true;
}


bool InitInitNtKernelRecallMsgHook() {
    using namespace ::utils;
    if (sIsHooked) {
        LOGW("InitInitNtKernelRecallMsgHook failed, already hooked");
        return false;
    }
    auto fnHookProc = &PerformNtRecallMsgHook;
    ProcessView self;
    if (int err;(err = self.readProcess(getpid())) != 0) {
        TraceErrorF(nullptr, gInstanceRevokeMsgHook, "InitInitNtKernelRecallMsgHook failed, readProcess failed: {}", err);
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
        // LOGD("libkernel.so is already loaded");
        // hook now
        return fnHookProc(libkernel->baseAddress);
    } else {
        int rc = RegisterLoadLibraryCallback([fnHookProc](const char* name, void* handle) {
            if (name == nullptr) {
                return;
            }
            std::string soname;
            // LOGD("dl_dlopen: {}", name);
            // get suffix
            auto suffix = strrchr(name, '/');
            if (suffix == nullptr) {
                soname = name;
            } else {
                soname = suffix + 1;
            }
            if (soname == "libkernel.so") {
                // LOGD("dl_dlopen: libkernel.so is loaded, start hook");
                // get base address
                ProcessView self2;
                if (int err;(err = self2.readProcess(getpid())) != 0) {
                    TraceErrorF(nullptr, gInstanceRevokeMsgHook, "InitInitNtKernelRecallMsgHook failed, readProcess failed: {}", err);
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
                        TraceErrorF(nullptr, gInstanceRevokeMsgHook, "InitInitNtKernelRecallMsgHook failed, fnHookProc failed");
                    }
                } else {
                    TraceErrorF(nullptr, gInstanceRevokeMsgHook, "InitInitNtKernelRecallMsgHook failed, but it was loaded");
                }
            }
        });
        if (rc < 0) {
            // it's better to report this error somehow
            TraceErrorF(nullptr, gInstanceRevokeMsgHook, "InitInitNtKernelRecallMsgHook failed, RegisterLoadLibraryCallback failed: {}", rc);
            return false;
        }
        // LOGD("libkernel.so is not loaded, register callback");
        return true;
    }
}

} // ntqq::hook

extern "C" JNIEXPORT jboolean JNICALL
Java_cc_ioctl_hook_msg_RevokeMsgHook_nativeInitNtKernelRecallMsgHookV1p2(JNIEnv* env, jobject thiz) {
    using ntqq::hook::klassRevokeMsgHook;
    using ntqq::hook::gInstanceRevokeMsgHook;
    using ntqq::hook::handleRecallSysMsgFromNtKernel;
    if (klassRevokeMsgHook == nullptr) {
        jclass clazz = env->GetObjectClass(thiz);
        if (clazz == nullptr) {
            LOGE("InitInitNtKernelRecallMsgHook failed, GetObjectClass failed");
            return false;
        }
        klassRevokeMsgHook = (jclass) env->NewGlobalRef(clazz);
        gInstanceRevokeMsgHook = env->NewGlobalRef(thiz);
        handleRecallSysMsgFromNtKernel = env->GetStaticMethodID(clazz, "handleRecallSysMsgFromNtKernel",
                                                                "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JJJJI)V");
        if (handleRecallSysMsgFromNtKernel == nullptr) {
            LOGE("InitInitNtKernelRecallMsgHook failed, GetStaticMethodID failed");
            return false;
        }
    }
    auto ret = ntqq::hook::InitInitNtKernelRecallMsgHook();
    if (!ret) {
        LOGE("InitInitNtKernelRecallMsgHook failed");
    }
    return ret;
}

//@formatter:off
static JNINativeMethod gMethods[] = {
        {"nativeInitNtKernelRecallMsgHookV1p2", "()Z", reinterpret_cast<void*>(Java_cc_ioctl_hook_msg_RevokeMsgHook_nativeInitNtKernelRecallMsgHookV1p2)},

};
//@formatter:on
REGISTER_SECONDARY_FULL_INIT_NATIVE_METHODS("cc/ioctl/hook/msg/RevokeMsgHook", gMethods);
