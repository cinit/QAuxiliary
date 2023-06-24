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
#include <span>
#include <optional>
#include <sys/mman.h>
#include <ucontext.h>
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
#include "utils/ElfScan.h"
#include "utils/MemoryUtils.h"
#include "qauxv_core/natives_utils.h"

namespace ntqq::hook {

using namespace qauxv;
using namespace utils;

/**
    void RecallC2cSysMsg(void * param_1, void ** param_2, void * param_3)

    a01604234 fd 7b ba a9     stp        x29,x30,[sp, #local_60]!
    a01604238 91 16 00 94     bl         _prologue_save_regs_a01609c7c
    a0160423c ff c3 0f d1     sub        sp,sp,#0x3f0
    a01604240 54 d0 3b d5     mrs        x20,tpidr_el0
    a01604244 88 16 40 f9     ldr        x8,[x20, #0x28]
    a01604248 a8 03 1f f8     stur       x8,[x29, #-0x10]=>DAT_fffffffffffffff0
    a0160424c 48 00 40 f9     ldr        x8,[x2]
    a01604250 48 52 00 b4     cbz        x8,LAB_a01604c98
                                 TAG_C2C_KEY_START
    a01604254 09 8d 40 f8     ldr        x9,[x8, #0x8]!
    a01604258 f5 03 00 aa     mov        x21,x0
    a0160425c 21 00 80 52     mov        w1,#0x1
    a01604260 f3 03 02 aa     mov        x19,x2
    a01604264 29 8d 40 f9     ldr        x9,[x9, #0x118]
                                 TAG_C2C_KEY_END
    [ 09 8d 40 f8 f5 03 00 aa 21 00 80 52 f3 03 02 aa 29 8d 40 f9 ]
 */
static constexpr uint8_t kTraitRecallC2cSysMsg[] =
        {0x09, 0x8d, 0x40, 0xf8, 0xf5, 0x03, 0x00, 0xaa, 0x21, 0x00, 0x80, 0x52, 0xf3, 0x03, 0x02, 0xaa, 0x29, 0x8d, 0x40, 0xf9};
static constexpr uint64_t kTraitOffsetRecallC2cSysMsg = 4 * 8;

/**
    void __cdecl RecallGroupSysMsg(void * param_1, void * param_2, void * param_3)

    a01605904 fd 7b ba a9     stp        x29,x30,[sp, #local_60]!
    a01605908 dd 10 00 94     bl         _prologue_save_regs_a01609c7c
    a0160590c ff 83 0d d1     sub        sp,sp,#0x360
    a01605910 bc 12 00 94     bl         FUN_a0160a400
    a01605914 c6 12 00 94     bl         FUN_a0160a42c
    a01605918 a8 03 1f f8     stur       x8,[x29, #-0x10]=>DAT_fffffffffffffff0
                                 TAG_GROUP_KEY_START
    a0160591c 28 00 40 f9     ldr        x8,[x1]
    a01605920 61 00 80 52     mov        w1,#0x3
    a01605924 09 8d 40 f8     ldr        x9,[x8, #0x8]!
    a01605928 29 8d 40 f9     ldr        x9,[x9, #0x118]
                                 TAG_GROUP_KEY_END
    a0160592c 53 12 00 94     bl         __call__FUN_a0160a278
                          msg_common::Msg::kBody
    a01605930 00 04 00 36     tbz        w0,#0x0,LAB_a016059b0

    [ 28 00 40 f9 61 00 80 52 09 8d 40 f8 29 8d 40 f9 ]
 */
static constexpr uint8_t kTraitRecallGroupSysMsg[] =
        {0x28, 0x00, 0x40, 0xf9, 0x61, 0x00, 0x80, 0x52, 0x09, 0x8d, 0x40, 0xf8, 0x29, 0x8d, 0x40, 0xf9};
static constexpr uint64_t kTraitOffsetRecallGroupSysMsg = 4 * 6;

static bool sIsHooked = false;

EXPORT extern "C" void* gLibkernelBaseAddress = nullptr;

jclass klassRevokeMsgHook = nullptr;
jmethodID handleC2cRecallMsgFromNtKernel = nullptr;

uint64_t ThunkGetInt64Property(const void* thiz, int property) {
    // vtable
    // 4160. [[this+8]+0x58]
    void* thisp8 = reinterpret_cast<void*>(reinterpret_cast<uintptr_t>(thiz) + 8);
    uintptr_t vtable = *reinterpret_cast<uintptr_t*>(thisp8);
    void* func = *reinterpret_cast<void**>(vtable + 0x58);
    return reinterpret_cast<decltype(ThunkGetInt64Property)*>(func)(thisp8, property);
}

uint32_t ThunkGetInt32Property(const void* thiz, int property) {
    // vtable
    // 4160. [[this+8]+0x38]
    void* thisp8 = reinterpret_cast<void*>(reinterpret_cast<uintptr_t>(thiz) + 8);
    uintptr_t vtable = *reinterpret_cast<uintptr_t*>(thisp8);
    void* func = *reinterpret_cast<void**>(vtable + 0x38);
    return reinterpret_cast<decltype(ThunkGetInt64Property)*>(func)(thisp8, property);
}

std::string ThunkGetStringProperty(void* thiz, int property) {
    // vtable
    // 4160. [[this+8]+0x70]
    void* thisp8 = reinterpret_cast<void*>(reinterpret_cast<uintptr_t>(thiz) + 8);
    uintptr_t vtable = *reinterpret_cast<uintptr_t*>(thisp8);
    void* func = *reinterpret_cast<void**>(vtable + 0x70);
    return reinterpret_cast<decltype(ThunkGetStringProperty)*>(func)(thisp8, property);
}

//void ThunkCallAPI(void* x0, uintptr_t api_caller_id, int x2, int x3, int& x4, std::string& x5) {
//    // 4160. 0x00cc0750
//    // "CallAPI"
//    // "!!! RegisterAPIHandler Error crash: api_caller_id is empty can not use You can use GlobalAPI or set other value to api_caller_id !!!"
//    auto func = reinterpret_cast<decltype(ThunkCallAPI)*>((uintptr_t) gLibkernelBaseAddress + 0x00cc0750);
//    func(x0, api_caller_id, x2, x3, x4, x5);
//}

class RevokeMsgInfoAccess {
public:

    struct UnknownObjectStub16 {
        void* _unk0_8;
        void* _unk8_8;
    };

};

void (* sOriginHandleGroupRecallSysMsgCallback)(void*, void*, void*) = nullptr;

void HandleGroupRecallSysMsgCallback(void* p1, void* p2, void* p3) {
    if (p3 == nullptr || *(void**) p3 == nullptr) {
        LOGE("HandleRecallGroupSysMsgCallback p3 == null, todo, wip, return");
        return;
    }
    // TODO: get group id, etc
}

void (* sOriginHandleC2cRecallSysMsgCallback)(void*, void*, void*) = nullptr;

void NotifyRecallMsgEventForC2c(const std::string& fromUid, const std::string& toUid,
                                uint64_t random64, uint64_t timeSeconds,
                                uint64_t msgUid, uint64_t msgSeq, uint32_t msgClientSeq) {
    JavaVM* vm = HostInfo::GetJavaVM();
    if (vm == nullptr) {
        LOGE("NotifyRecallMsgEventForC2c fatal vm == null");
        return;
    }
    if (klassRevokeMsgHook == nullptr) {
        LOGE("NotifyRecallMsgEventForC2c fatal klassRevokeMsgHook == null");
        return;
    }
    // check if current thread is attached to jvm
    JNIEnv* env = nullptr;
    bool isAttachedManually = false;
    jint err = vm->GetEnv((void**) &env, JNI_VERSION_1_6);
    if (err == JNI_EDETACHED) {
        if (vm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            LOGE("NotifyRecallMsgEventForC2c fatal AttachCurrentThread failed");
            return;
        }
        isAttachedManually = true;
    } else if (env == nullptr) {
        LOGE("NotifyRecallMsgEventForC2c fatal GetEnv failed, err = {}", err);
        return;
    }
    // call java method
    env->CallStaticVoidMethod(klassRevokeMsgHook, handleC2cRecallMsgFromNtKernel,
                              env->NewStringUTF(fromUid.c_str()),
                              env->NewStringUTF(toUid.c_str()),
                              (jlong) random64, (jlong) timeSeconds, (jlong) msgUid,
                              (jlong) msgSeq, (jint) msgClientSeq);
    // check if exception occurred
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
    // detach thread if attached manually
    if (isAttachedManually) {
        vm->DetachCurrentThread();
    }
}

void HandleC2cRecallSysMsgCallback(void* p1, void* p2, void* p3) {
    if (p3 == nullptr || *(void**) p3 == nullptr) {
        LOGE("HandleC2cGroupSysMsgCallback BUG !!! *p3 = null, this should not happen!!!");
        return;
    }

    std::array<void*, 3> vectorResultStub = {nullptr, nullptr, nullptr};

    void* v1 = *(void**) p3;
    void** v2 = (void**) ((uintptr_t) v1 + 8);

    // 4160. 0xf0
    auto func = reinterpret_cast<std::array<void*, 3>(*)(void*, int)>(*(void**) ((uintptr_t) *v2 + 0xf0));
    // XXX: memory leak, no dtor available
    vectorResultStub = func(v2, 1);

    static_assert(sizeof(std::vector<int>) == sizeof(std::array<void*, 3>), "libcxx vector size not match");
    const auto& objects = *reinterpret_cast<const std::vector<RevokeMsgInfoAccess::UnknownObjectStub16>*>(&vectorResultStub);

    if (!objects.empty()) {

//        void* x0 = nullptr;
//        uintptr_t x1 = 0;
//        {
//            uint8_t* param_1 = static_cast<uint8_t*>(p1) + 8;
//            uint8_t* pbVar1;
//            uint64_t uVar2;
//            uVar2 = *(uint64_t*) (param_1 + 8);
//            pbVar1 = *(uint8_t**) (param_1 + 0x10);
//            if ((*param_1 & 1) == 0) {
//                pbVar1 = param_1 + 1;
//                uVar2 = (uint64_t) (*param_1 >> 1);
//            }
//            x0 = pbVar1;
//            x1 = uVar2;
//        }
//        int tmpInt = 0x138b;
//        std::string tmpString;
//        ThunkCallAPI(x0, x1, 0x10, 1, tmpInt, tmpString);

        for (const auto& obj: objects) {
            auto fromUid = ThunkGetStringProperty(obj._unk0_8, 1);
            auto toUid = ThunkGetStringProperty(obj._unk0_8, 2);
            auto randomId = ThunkGetInt64Property(obj._unk0_8, 6);
            auto timeSeconds = ThunkGetInt64Property(obj._unk0_8, 5);
            auto msgUid = ThunkGetInt64Property(obj._unk0_8, 4);
            auto msgSeq = ThunkGetInt64Property(obj._unk0_8, 0x14);
            auto msgClientSeq = ThunkGetInt32Property(obj._unk0_8, 3);
            NotifyRecallMsgEventForC2c(fromUid, toUid, randomId, timeSeconds, msgUid, msgSeq, msgClientSeq);
        }
    }
}

// Nobody uses PaiYiPai, right?

bool InitInitNtKernelRecallMsgHook() {
    using namespace utils;
    if (sIsHooked) {
        LOGW("InitInitNtKernelRecallMsgHook failed, already hooked");
        return false;
    }
    auto fnHookProc = [](uint64_t baseAddress) {
        if (sIsHooked) {
            return false;
        }
        sIsHooked = true;
        bool hasError = false;
        gLibkernelBaseAddress = reinterpret_cast<void*>(baseAddress);
        // LOGD("baseAddress = {}", baseAddress);
        auto c2cCandidates = FindByteSequenceForLoadedImage(gLibkernelBaseAddress, kTraitRecallC2cSysMsg, true, 4);
        auto groupCandidates = FindByteSequenceForLoadedImage(gLibkernelBaseAddress, kTraitRecallGroupSysMsg, true, 4);
        uint64_t offsetC2c = 0;
        uint64_t offsetGroup = 0;
        if (c2cCandidates.size() != 1) {
            std::string logStr = "InitInitNtKernelRecallMsgHook failed, c2cCandidates.size()=" + std::to_string(c2cCandidates.size());
            logStr += ", [";
            for (auto& item: c2cCandidates) {
                logStr += std::to_string(item) + ",";
            }
            logStr += "]";
            LOGE("{}", logStr);
            hasError = true;
        } else {
            uintptr_t offset = c2cCandidates[0] - kTraitOffsetRecallC2cSysMsg;
            const uint32_t* p = reinterpret_cast<const uint32_t*>(baseAddress + offset);
            uint32_t inst = *p;
            // expect  fd 7b ba a9     stp        x29,x30,[sp, #???]!
            if ((inst & ((0b11111111u << 24) | (0b11000000u << 16u) | (0b01111111u << 8u) | 0xFF))
                    == ((0b10101001u << 24u) | (0b10000000u << 16u) | (0x7b << 8u) | 0xfd)) {
                offsetC2c = offset;
                LOGD("c2cCandidates = [{:x}], offsetC2c = {:x}", c2cCandidates[0], offsetC2c);
            } else {
                LOGE("c2c: inst = {:x} not match, expect 'stp x29,x30,[sp, #???]!'", inst);
            }
        }
        if (groupCandidates.size() != 1) {
            std::string logStr = "InitInitNtKernelRecallMsgHook failed, groupCandidates.size()=" + std::to_string(groupCandidates.size());
            logStr += ", [";
            for (auto& item: groupCandidates) {
                logStr += std::to_string(item) + ",";
            }
            logStr += "]";
            LOGE("{}", logStr);
            hasError = true;
        } else {
            uintptr_t offset = groupCandidates[0] - kTraitOffsetRecallGroupSysMsg;
            const uint32_t* p = reinterpret_cast<const uint32_t*>(baseAddress + offset);
            uint32_t inst = *p;
            // expect  fd 7b ba a9     stp        x29,x30,[sp, #???]!
            if ((inst & ((0b11111111u << 24) | (0b11000000u << 16u) | (0b01111111u << 8u) | 0xFF))
                    == ((0b10101001u << 24u) | (0b10000000u << 16u) | (0x7b << 8u) | 0xfd)) {
                offsetGroup = offset;
                LOGD("groupCandidates = [{:x}], offsetGroup = {:x}", groupCandidates[0], offsetGroup);
            } else {
                LOGE("group: inst = {:x} not match, expect 'stp x29,x30,[sp, #???]!'", inst);
            }
        }
        if (offsetC2c != 0) {
            void* c2c = (void*) (baseAddress + offsetC2c);
            if (CreateInlineHook(c2c, (void*) &HandleC2cRecallSysMsgCallback, (void**) &sOriginHandleC2cRecallSysMsgCallback) != 0) {
                LOGE("InitInitNtKernelRecallMsgHook failed, DobbyHook c2c failed");
                return false;
            }
        } else {
            LOGE("InitInitNtKernelRecallMsgHook failed, offsetC2c == 0");
        }
        if (offsetGroup != 0) {
            void* group = (void*) (baseAddress + offsetGroup);
            if (CreateInlineHook(group, (void*) &HandleGroupRecallSysMsgCallback, (void**) &sOriginHandleGroupRecallSysMsgCallback) != 0) {
                LOGE("InitInitNtKernelRecallMsgHook failed, DobbyHook group failed");
                return false;
            }
        } else {
            LOGE("InitInitNtKernelRecallMsgHook failed, offsetGroup == 0");
        }
        if (hasError) {
            LOGE("InitInitNtKernelRecallMsgHook failed, hasError");
        }
        return true;
    };
    ProcessView self;
    if (int err;(err = self.readProcess(getpid())) != 0) {
        LOGE("InitInitNtKernelRecallMsgHook failed, readProcess failed: {}", err);
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
        RegisterLoadLibraryCallback([fnHookProc](const char* name, void* handle) {
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
                    LOGE("InitInitNtKernelRecallMsgHook failed, readProcess failed: {}", err);
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
        // LOGD("libkernel.so is not loaded, register callback");
        return true;
    }
}

} // ntqq::hook

extern "C" JNIEXPORT jboolean JNICALL
Java_cc_ioctl_hook_msg_RevokeMsgHook_nativeInitNtKernelRecallMsgHook(JNIEnv* env, jobject thiz) {
    using ntqq::hook::klassRevokeMsgHook;
    using ntqq::hook::handleC2cRecallMsgFromNtKernel;
    if (klassRevokeMsgHook == nullptr) {
        jclass clazz = env->GetObjectClass(thiz);
        if (clazz == nullptr) {
            LOGE("InitInitNtKernelRecallMsgHook failed, GetObjectClass failed");
            return false;
        }
        klassRevokeMsgHook = (jclass) env->NewGlobalRef(clazz);
        handleC2cRecallMsgFromNtKernel = env->GetStaticMethodID(clazz, "handleC2cRecallMsgFromNtKernel",
                                                                "(Ljava/lang/String;Ljava/lang/String;JJJJI)V");
        if (handleC2cRecallMsgFromNtKernel == nullptr) {
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
