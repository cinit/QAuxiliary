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
#include "qauxv_core/natives_utils.h"

#ifndef STACK_GUARD
// for debug purpose only
#define STACK_GUARD ((void) 0)
#endif

namespace ntqq::hook {

using namespace qauxv;
using namespace utils;

static bool sIsHooked = false;

EXPORT extern "C" void* gLibkernelBaseAddress = nullptr;

jclass klassRevokeMsgHook = nullptr;
jobject gInstanceRevokeMsgHook = nullptr;
jmethodID handleRecallSysMsgFromNtKernel = nullptr;

uintptr_t gOffsetGetDecoderSp = 0;

uintptr_t gOffsetForTmpRev5048 = 0;

NOINLINE
uint64_t ThunkGetInt64Property(const void* thiz, int property) {
    // vtable
    // 4160. [[this+8]+0x58]
    void* thisp8 = reinterpret_cast<void*>(reinterpret_cast<uintptr_t>(thiz) + 8);
    uintptr_t vtable = *reinterpret_cast<uintptr_t*>(thisp8);
    void* func = *reinterpret_cast<void**>(vtable + 0x58);
    return reinterpret_cast<decltype(ThunkGetInt64Property)*>(func)(thisp8, property);
}

NOINLINE
uint32_t ThunkGetInt32Property(const void* thiz, int property) {
    // vtable
    // 4160. [[this+8]+0x38]
    void* thisp8 = reinterpret_cast<void*>(reinterpret_cast<uintptr_t>(thiz) + 8);
    uintptr_t vtable = *reinterpret_cast<uintptr_t*>(thisp8);
    void* func = *reinterpret_cast<void**>(vtable + 0x38);
    return reinterpret_cast<decltype(ThunkGetInt32Property)*>(func)(thisp8, property);
}

NOINLINE
std::string ThunkGetStringProperty(void* thiz, int property) {
    // vtable
    // 4160. [[this+8]+0x70]
    void* thisp8 = reinterpret_cast<void*>(reinterpret_cast<uintptr_t>(thiz) + 8);
    uintptr_t vtable = *reinterpret_cast<uintptr_t*>(thisp8);
    void* func = *reinterpret_cast<void**>(vtable + 0x70);
    return reinterpret_cast<decltype(ThunkGetStringProperty)*>(func)(thisp8, property);
}

template<typename ReturnType, uintptr_t vtableOffset, uintptr_t thizOffset, typename... ArgTypes>
requires((std::is_same_v<ReturnType, void> || std::is_integral_v<ReturnType> || std::is_pointer_v<ReturnType>)
        && ((std::is_integral_v<ArgTypes> || std::is_pointer_v<ArgTypes>) && ...))
NOINLINE
ReturnType vcall(void* thiz, ArgTypes... args) {
    // vtable
    // [[this+thizOff]+offsetVT]
    void* thisp8 = reinterpret_cast<void*>(reinterpret_cast<uintptr_t>(thiz) + thizOffset);
    uintptr_t vtable = *reinterpret_cast<uintptr_t*>(thisp8);
    void* func = *reinterpret_cast<void**>(vtable + vtableOffset);
    if constexpr (std::is_same_v<ReturnType, void>) {
        reinterpret_cast<ReturnType(*)(void*, ArgTypes...)>(func)(thisp8, args...);
        return;
    } else {
        return reinterpret_cast<ReturnType(*)(void*, ArgTypes...)>(func)(thisp8, args...);
    }
}

template<typename... ArgTypes>
requires(((std::is_integral_v<ArgTypes> || std::is_pointer_v<ArgTypes>) && ...))
NOINLINE
void vcall_x8_v2(void* thiz, uintptr_t vtableOffset, uintptr_t thizOffset, void* x8, ArgTypes... args) {
    // vtable
    // [[this+thizOff]+offsetVT]
    void* thisp8 = reinterpret_cast<void*>(reinterpret_cast<uintptr_t>(thiz) + thizOffset);
    uintptr_t vtable = *reinterpret_cast<uintptr_t*>(thisp8);
    void* func = *reinterpret_cast<void**>(vtable + vtableOffset);
    static_assert(sizeof...(args) <= 3);
    std::array<void*, 3> argArray = {reinterpret_cast<void*>(args)...};
    call_func_with_x8(func, x8, thisp8, argArray[0], argArray[1], argArray[2]);
}

// helper for uintptr_t as this
template<typename ReturnType, uintptr_t vtableOffset, uintptr_t thizOffset, typename... ArgTypes>
requires((std::is_same_v<ReturnType, void> || std::is_integral_v<ReturnType> || std::is_pointer_v<ReturnType>)
        && ((std::is_integral_v<ArgTypes> || std::is_pointer_v<ArgTypes>) && ...))
static inline ReturnType vcall(uintptr_t thiz, ArgTypes... args) {
    if constexpr (std::is_same_v<ReturnType, void>) {
        vcall<vtableOffset, thizOffset, ArgTypes...>(reinterpret_cast<void*>(thiz), args...);
        return;
    } else {
        return vcall<vtableOffset, thizOffset, ArgTypes...>(reinterpret_cast<void*>(thiz), args...);
    }
}

template<typename... ArgTypes> requires((std::is_integral_v<ArgTypes> || std::is_pointer_v<ArgTypes>) && ...)
static inline void vcall_x8_v2(uintptr_t thiz, uintptr_t vtableOffset, uintptr_t thizOffset, void* x8, ArgTypes... args) {
    vcall_x8_v2<ArgTypes...>(reinterpret_cast<void*>(thiz), vtableOffset, thizOffset, x8, args...);
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


void NotifyRecallSysMsgEvent(int chatType, const std::string& peerUid, const std::string& recallOpUid, const std::string& msgAuthorUid,
                             const std::string& toUid, uint64_t random64, uint64_t timeSeconds, uint64_t msgUid, uint64_t msgSeq, uint32_t msgClientSeq) {
    JavaVM* vm = HostInfo::GetJavaVM();
    if (vm == nullptr) {
        LOGE("NotifyRecallSysMsgEvent fatal vm == null");
        return;
    }
    if (klassRevokeMsgHook == nullptr) {
        LOGE("NotifyRecallSysMsgEvent fatal klassRevokeMsgHook == null");
        return;
    }
    // check if current thread is attached to jvm
    JNIEnv* env = nullptr;
    bool isAttachedManually = false;
    jint err = vm->GetEnv((void**) &env, JNI_VERSION_1_6);
    if (err == JNI_EDETACHED) {
        if (vm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            LOGE("NotifyRecallSysMsgEvent fatal AttachCurrentThread failed");
            return;
        }
        isAttachedManually = true;
    } else if (env == nullptr) {
        LOGE("NotifyRecallSysMsgEvent fatal GetEnv failed, err = {}", err);
        return;
    }
    // call java method
    env->CallStaticVoidMethod(klassRevokeMsgHook, handleRecallSysMsgFromNtKernel,
                              jint(chatType), env->NewStringUTF(peerUid.c_str()), env->NewStringUTF(recallOpUid.c_str()),
                              env->NewStringUTF(msgAuthorUid.c_str()), env->NewStringUTF(toUid.c_str()),
                              jlong(random64), jlong(timeSeconds), jlong(msgUid), jlong(msgSeq), jint(msgClientSeq));
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

void NotifyRecallMsgEventForC2c(const std::string& fromUid, const std::string& toUid,
                                uint64_t random64, uint64_t timeSeconds,
                                uint64_t msgUid, uint64_t msgSeq, uint32_t msgClientSeq) {
    NotifyRecallSysMsgEvent(1, fromUid, fromUid, fromUid, toUid, random64, timeSeconds, msgUid, msgSeq, msgClientSeq);
}

void NotifyRecallMsgEventForGroup(const std::string& peerUid, const std::string& recallOpUid, const std::string& msgAuthorUid,
                                  uint64_t random64, uint64_t timeSeconds, uint64_t msgSeq) {
    NotifyRecallSysMsgEvent(2, peerUid, recallOpUid, msgAuthorUid, peerUid, random64, timeSeconds, 0, msgSeq, 0);
}


void (* sOriginHandleGroupRecallSysMsgCallback)(void*, void*, void*) = nullptr;

void HandleGroupRecallSysMsgCallback([[maybe_unused]] void* x0, void* x1, [[maybe_unused]] void* x2, [[maybe_unused]] int x3) {
    // LOGD("HandleGroupRecallSysMsgCallback start p1={:p}, p2={:p}, p3={:p}", x0, x1, x2);
    // we can still do it... hitherto p3 == null... we need to decode the message manually...
    uintptr_t base = (uintptr_t) gLibkernelBaseAddress;
    void* pVar1 = *(void**) x1;
    if ((vcall<int, 0x118, 8, int>(pVar1, 3) & 1) == 0) {
        LOGE("msg_recall: HandleRecallSysMsg: on recall group sys msg! hasn't msg_common::Msg::kBody");
        return;
    }
    void* pVar2 = *(void**) x1;
    STACK_GUARD;
    std::array<uint8_t, 0x100> objVar1 = {}; // actual size unknown, maybe 0x90
    STACK_GUARD;
    vcall_x8_v2<int>(pVar2, gOffsetForTmpRev5048, 0, &objVar1, 3);
    auto pVar3 = *(void**) (objVar1.data());
    if (pVar3 == nullptr) {
        LOGE("msg_recall: HandleRecallSysMsg: on recall group sys msg! msg_common::Msg::kBody = null");
        return;
    }
    if ((vcall<int, 0x118, 8, int>(pVar3, 2) & 1) == 0) {
        LOGE("msg_recall: HandleRecallSysMsg: on recall group sys msg! hasn't im_msg_body::MsgBody::kBytesMsgContent");
        return;
    }
    STACK_GUARD;
    std::vector<uint8_t> msgContentBytes;
    STACK_GUARD;
    vcall_x8_v2<int>(pVar3, 0x78, 8, &msgContentBytes, 2);
    if (msgContentBytes.size() < 8) {
        LOGE("msg_recall: HandleRecallSysMsg: on recall group sys msg! im_msg_body::MsgBody::kBytesMsgContent is error");
        return;
    }
    std::vector<uint8_t> content = {msgContentBytes.begin() + 7, msgContentBytes.end()};
    STACK_GUARD;
    std::array<void*, 2> objVar3 = {}; // actual size 0x10, maybe shared_ptr, but we don't have dtor
    STACK_GUARD;
    call_func_with_x8((void*) (base + gOffsetGetDecoderSp), &objVar3, 0, 0, 0, 0);
    void* notifyMsgBody = objVar3[0];
    if ((vcall<int, 0x100, 8, std::vector<uint8_t>*>(notifyMsgBody, &content) & 1) == 0) {
        LOGE("on recall group sys msg! decode kBytesMsgContent fail");
        return;
    }
    uint64_t groupCode = vcall<uint64_t, 0x58, 8, int>(notifyMsgBody, 4);
    if (groupCode == 0) {
        LOGE("on recall group sys msg! group code is 0");
        return;
    }
    uint64_t opType = vcall<uint64_t, 0x58, 8, int>(notifyMsgBody, 1);
    if (opType != 7) {
        LOGW("HandleGroupRecallSysMsgCallback: on recall group sys msg! no Prompt_MsgRecallReminder op_type:{}", opType);
        return;
    }
    if ((vcall<uint64_t, 0x118, 8, int>(notifyMsgBody, 0xb) & 1) == 0) {
        LOGE("HandleGroupRecallSysMsgCallback: on recall group sys msg! no NotifyMsgBody::opt_msg_recall");
        return;
    }
    STACK_GUARD;
    std::array<void*, 2> optMsgRecall = {}; // actual size 0x10, maybe shared_ptr, but we don't have dtor
    STACK_GUARD;
    vcall_x8_v2<int>(notifyMsgBody, gOffsetForTmpRev5048, 0, &optMsgRecall, 0xb);
    if (optMsgRecall[0] == nullptr) {
        LOGE("HandleGroupRecallSysMsgCallback: on recall group sys msg! NotifyMsgBody::opt_msg_recall == null");
        return;
    }
    if ((vcall<int, 0x118, 8, int>(optMsgRecall[0], 3) & 1) == 0) {
        LOGE("HandleGroupRecallSysMsgCallback: on recall group sys msg! on recall group sys msg! no msg_infos");
        return;
    }
    std::array<void*, 3> vectorResultStub = {nullptr, nullptr, nullptr};
    vcall_x8_v2<int>(optMsgRecall[0], 0xf0, 8, &vectorResultStub, 3);
    std::string recallOpUid = ThunkGetStringProperty(optMsgRecall[0], 1);
    const auto& msgInfoList = *reinterpret_cast<const std::vector<RevokeMsgInfoAccess::UnknownObjectStub16>*>(&vectorResultStub);
    if (msgInfoList.empty()) {
        LOGE("HandleGroupRecallSysMsgCallback: on recall group sys msg! no any msg info");
        return;
    }
    std::string peerUid = fmt::format("{}", groupCode);
    for (const auto& msgInfo: msgInfoList) {
        uint32_t msgSeq = ThunkGetInt32Property(msgInfo._unk0_8, 1);
        uint32_t random = ThunkGetInt32Property(msgInfo._unk0_8, 3);
        uint64_t time = ThunkGetInt64Property(msgInfo._unk0_8, 2);
        std::string msgAuthorUid = ThunkGetStringProperty(msgInfo._unk0_8, 6);

        // Unfortunately, I didn't find a way to find the origMsgSenderUid.
        // The only thing we can do is to get message by msgSeq, and get senderUid from it, iff we have the message.

        NotifyRecallMsgEventForGroup(peerUid, recallOpUid, msgAuthorUid, random, time, msgSeq);
    }
}

void (* sOriginHandleC2cRecallSysMsgCallback)(void*, void*, void*) = nullptr;

void HandleC2cRecallSysMsgCallback([[maybe_unused]] void* p1, [[maybe_unused]] void* p2, void* p3, [[maybe_unused]] int x3) {
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
        gLibkernelBaseAddress = reinterpret_cast<void*>(baseAddress);
        // RecallC2cSysMsg 09 8d 40 f8 f5 03 00 aa 21 00 80 52 f3 03 02 aa 29 8d 40 f9
        auto targetRecallC2cSysMsg = AobScanTarget()
                .WithName("RecallC2cSysMsg")
                .WithSequence({0x09, 0x8d, 0x40, 0xf8, 0xf5, 0x03, 0x00, 0xaa, 0x21, 0x00, 0x80, 0x52, 0xf3, 0x03, 0x02, 0xaa, 0x29, 0x8d, 0x40, 0xf9})
                .WithStep(4)
                .WithExecMemOnly(true)
                .WithOffsetsForResult({-0x20, -0x24, -0x28})
                .WithResultValidator(CommonAobScanValidator::kArm64StpX29X30SpImm);

        // RecallGroupSysMsg 28 00 40 f9 61 00 80 52 09 8d 40 f8 29 8d 40 f9
        auto targetRecallGroupSysMsg = AobScanTarget()
                .WithName("RecallGroupSysMsg")
                .WithSequence({0x28, 0x00, 0x40, 0xf9, 0x61, 0x00, 0x80, 0x52, 0x09, 0x8d, 0x40, 0xf8, 0x29, 0x8d, 0x40, 0xf9})
                .WithStep(4)
                .WithExecMemOnly(true)
                .WithOffsetsForResult({-0x18, -0x24, -0x28})
                .WithResultValidator(CommonAobScanValidator::kArm64StpX29X30SpImm);

        // GetDecoder 3f 8d 01 f8 f4 03 00 aa 1f 10 00 f9
        auto targetGetDecoder = AobScanTarget()
                .WithName("GetDecoder")
                .WithSequence({0x3f, 0x8d, 0x01, 0xf8, 0xf4, 0x03, 0x00, 0xaa, 0x1f, 0x10, 0x00, 0xf9})
                .WithStep(4)
                .WithExecMemOnly(true)
                .WithOffsetsForResult({-0x78})
                .WithResultValidator(CommonAobScanValidator::kArm64StpX29X30SpImm);

        //@formatter:off
        //OffsetForTmpRev5048
        //61 01 80 52     mov        w1,#0xb
        //e0 03 ?? aa     mov        x0,x?
        //?? 10 00 94     bl         FUN_?
        //?? ?? 00 36     tbz        w0,#0x0,LAB_?
        //?? 02 40 f9     ldr        x8,[x??]
        //61 01 80 52     mov        w1,#0xb
        //e0 03 ?? aa     mov        x0,x??
        //09 !! 40 f9     ldr        x9,[x8, #0x!!] <-- we need to find this
        //e8 ?? ?? 91     add        x8,sp,#0x??
        //20 01 3f d6     blr        x9
        auto targetInstructionOffsetForTmpRev5048 = AobScanTarget()
                .WithName("InstructionOffsetForTmpRev5048")
                        //     0x61  0x01  0x80  0x52  0xe0  0x03  0x??  0xaa
                        //     0x??  0x10  0x00  0x94  0x??  0x??  0x00  0x36
                        //     0x??  0x02  0x40  0xf9  0x61  0x01  0x80  0x52
                        //     0xe0  0x03  0x??  0xaa  0x09  0x??  0x40  0xf9
                        //     0xe8  0x??  0x??  0x91  0x20  0x01  0x3f  0xd6
                .WithSequence({0x61, 0x01, 0x80, 0x52, 0xe0, 0x03, 0x00, 0xaa,
                               0x00, 0x10, 0x00, 0x94, 0x00, 0x00, 0x00, 0x36,
                               0x00, 0x02, 0x40, 0xf9, 0x61, 0x01, 0x80, 0x52,
                               0xe0, 0x03, 0x00, 0xaa, 0x09, 0x00, 0x40, 0xf9,
                               0xe8, 0x00, 0x00, 0x91, 0x20, 0x01, 0x3f, 0xd6})
                .WithMask({    0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x00, 0xff,
                               0x00, 0xff, 0xff, 0xff, 0x00, 0x00, 0xff, 0xff,
                               0x00, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
                               0xff, 0xff, 0x00, 0xff, 0xff, 0x00, 0xff, 0xff,
                               0xff, 0x00, 0x00, 0xff, 0xff, 0xff, 0xff, 0xff})
                .WithStep(4)
                .WithExecMemOnly(true)
                .WithOffsetsForResult({7 * 4});
        //@formatter:on

        std::vector<std::string> errorMsgList;
        // auto start = std::chrono::steady_clock::now();
        if (!SearchForAllAobScanTargets({&targetRecallC2cSysMsg, &targetRecallGroupSysMsg, &targetGetDecoder,
                                         &targetInstructionOffsetForTmpRev5048},
                                        gLibkernelBaseAddress, true, errorMsgList)) {
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

        if (auto pkg = HostInfo::GetPackageName(); pkg != "com.tencent.mobileqq") {
            TraceErrorF(nullptr, gInstanceRevokeMsgHook, "InitInitNtKernelRecallMsgHook failed, unexpected package name: {}", pkg);
            return false;
        }

        uint64_t offsetC2c = targetRecallC2cSysMsg.GetResultOffset();
        uint64_t offsetGroup = targetRecallGroupSysMsg.GetResultOffset();
        uint64_t offsetGetDecoder = targetGetDecoder.GetResultOffset();
        uint64_t offsetInstForTmpRev5048 = targetInstructionOffsetForTmpRev5048.GetResultOffset();

        uint32_t instructionForTmpRev5048 = *reinterpret_cast<uint32_t*>(
                reinterpret_cast<uintptr_t>(gLibkernelBaseAddress) + offsetInstForTmpRev5048);
        {
            // LOGD("instructionForTmpRev5048={:08x}", instructionForTmpRev5048);
            // 09 !! 40 f9     ldr  x9,[x8, #0x!!]
            uint32_t imm12 = ((instructionForTmpRev5048 >> 10u) & 0xfffu) << 3u;
            // LOGD("imm12={:x}", imm12);
            gOffsetForTmpRev5048 = imm12;
        }

        LOGD("offsetC2c={:x}, offsetGroup={:x}, offsetGetDecoder={:x}, offsetInstForTmpRev5048={:x}, gOffsetForTmpRev5048={:x}",
             offsetC2c, offsetGroup, offsetGetDecoder, offsetInstForTmpRev5048, gOffsetForTmpRev5048);

        gOffsetGetDecoderSp = offsetGetDecoder;
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
    };
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
Java_cc_ioctl_hook_msg_RevokeMsgHook_nativeInitNtKernelRecallMsgHook(JNIEnv* env, jobject thiz) {
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
