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
#include "utils/MemoryUtils.h"
#include "qauxv_core/natives_utils.h"

namespace ntqq::hook {

using namespace qauxv;
using namespace utils;

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
                                uint64_t msgUid, uint32_t msgClientSeq) {
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
                              (jlong) random64, (jlong) timeSeconds, (jlong) msgUid, (jint) msgClientSeq);
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
            auto msgClientSeq = ThunkGetInt32Property(obj._unk0_8, 3);

            NotifyRecallMsgEventForC2c(fromUid, toUid, randomId, timeSeconds, msgUid, msgClientSeq);
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
            LOGE("InitInitNtKernelRecallMsgHook failed, unsupported version: {}", version);
            return false;
        }
    }
    if (offsetC2c != 0 && offsetGroup != 0) {
        auto fnHookProc = [offsetGroup, offsetC2c, version](uint64_t baseAddress) {
            if (sIsHooked) {
                return false;
            }
            sIsHooked = true;
            gLibkernelBaseAddress = reinterpret_cast<void*>(baseAddress);
            void* c2c = (void*) (baseAddress + offsetC2c);
            void* group = (void*) (baseAddress + offsetGroup);
            LOGI("InitInitNtKernelRecallMsgHook, start hook");
            if (CreateInlineHook(c2c, (void*) &HandleC2cRecallSysMsgCallback, (void**) &sOriginHandleC2cRecallSysMsgCallback) != 0) {
                LOGE("InitInitNtKernelRecallMsgHook failed, DobbyHook c2c failed");
                return false;
            }
            if (CreateInlineHook(group, (void*) &HandleGroupRecallSysMsgCallback, (void**) &sOriginHandleGroupRecallSysMsgCallback) != 0) {
                LOGE("InitInitNtKernelRecallMsgHook failed, DobbyHook group failed");
                return false;
            }
            LOGI("InitInitNtKernelRecallMsgHook success, version: {}", version);
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
            return true;
        }
    } else {
        LOGE("InitInitNtKernelRecallMsgHook failed, offsetC2c: {}, offsetGroup: {}", (void*) offsetC2c, (void*) offsetGroup);
        return false;
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
                                                                "(Ljava/lang/String;Ljava/lang/String;JJJI)V");
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
