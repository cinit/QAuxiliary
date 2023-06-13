//
// Created by sulfate on 2023-05-19.
//

#include "ThreadUtils.h"

#include <unistd.h>
#include <ucontext.h>
#include <cinttypes>
#include <sstream>
#include <android-base/stringprintf.h>

using namespace unwindstack;
using namespace android::base;

namespace utils {

void DumpThreadStackTraceToLogcat(uint32_t tid, android_LogPriority priority) {
    AndroidUnwinderData data;
    AndroidLocalUnwinder unwinder;
    bool result = unwinder.Unwind(tid, data);
    if (!result) {
        LOGE("Unwind failed for tid: {}, error: {}", tid, data.GetErrorString());
        return;
    }
    for (const auto& frame: data.frames) {
        __android_log_write(priority, "QAuxv", unwinder.FormatFrame(frame).c_str());
    }
}

void DumpCurrentThreadStackTraceToLogcat(android_LogPriority priority) {
    DumpThreadStackTraceToLogcat(gettid(), priority);
}

// @formatter:off

struct UContext {
  explicit UContext(void* raw_context)
      : context(reinterpret_cast<ucontext_t*>(raw_context)->uc_mcontext) {}

  void Dump(std::ostream& os) const;

  void DumpRegister32(std::ostream& os, const char* name, uint32_t value) const;
  void DumpRegister64(std::ostream& os, const char* name, uint64_t value) const;

  void DumpX86Flags(std::ostream& os, uint32_t flags) const;
  // Print some of the information from the status register (CPSR on ARMv7, PSTATE on ARMv8).
  template <typename RegisterType>
  void DumpArmStatusRegister(std::ostream& os, RegisterType status_register) const;

  mcontext_t& context;
};

void UContext::Dump(std::ostream& os) const {
#if defined(__APPLE__) && defined(__i386__)
  DumpRegister32(os, "eax", context->__ss.__eax);
  DumpRegister32(os, "ebx", context->__ss.__ebx);
  DumpRegister32(os, "ecx", context->__ss.__ecx);
  DumpRegister32(os, "edx", context->__ss.__edx);
  os << '\n';

  DumpRegister32(os, "edi", context->__ss.__edi);
  DumpRegister32(os, "esi", context->__ss.__esi);
  DumpRegister32(os, "ebp", context->__ss.__ebp);
  DumpRegister32(os, "esp", context->__ss.__esp);
  os << '\n';

  DumpRegister32(os, "eip", context->__ss.__eip);
  os << "                   ";
  DumpRegister32(os, "eflags", context->__ss.__eflags);
  DumpX86Flags(os, context->__ss.__eflags);
  os << '\n';

  DumpRegister32(os, "cs",  context->__ss.__cs);
  DumpRegister32(os, "ds",  context->__ss.__ds);
  DumpRegister32(os, "es",  context->__ss.__es);
  DumpRegister32(os, "fs",  context->__ss.__fs);
  os << '\n';
  DumpRegister32(os, "gs",  context->__ss.__gs);
  DumpRegister32(os, "ss",  context->__ss.__ss);
#elif defined(__linux__) && defined(__i386__)
  DumpRegister32(os, "eax", context.gregs[REG_EAX]);
  DumpRegister32(os, "ebx", context.gregs[REG_EBX]);
  DumpRegister32(os, "ecx", context.gregs[REG_ECX]);
  DumpRegister32(os, "edx", context.gregs[REG_EDX]);
  os << '\n';

  DumpRegister32(os, "edi", context.gregs[REG_EDI]);
  DumpRegister32(os, "esi", context.gregs[REG_ESI]);
  DumpRegister32(os, "ebp", context.gregs[REG_EBP]);
  DumpRegister32(os, "esp", context.gregs[REG_ESP]);
  os << '\n';

  DumpRegister32(os, "eip", context.gregs[REG_EIP]);
  os << "                   ";
  DumpRegister32(os, "eflags", context.gregs[REG_EFL]);
  DumpX86Flags(os, context.gregs[REG_EFL]);
  os << '\n';

  DumpRegister32(os, "cs",  context.gregs[REG_CS]);
  DumpRegister32(os, "ds",  context.gregs[REG_DS]);
  DumpRegister32(os, "es",  context.gregs[REG_ES]);
  DumpRegister32(os, "fs",  context.gregs[REG_FS]);
  os << '\n';
  DumpRegister32(os, "gs",  context.gregs[REG_GS]);
  DumpRegister32(os, "ss",  context.gregs[REG_SS]);
#elif defined(__linux__) && defined(__x86_64__)
  DumpRegister64(os, "rax", context.gregs[REG_RAX]);
  DumpRegister64(os, "rbx", context.gregs[REG_RBX]);
  DumpRegister64(os, "rcx", context.gregs[REG_RCX]);
  DumpRegister64(os, "rdx", context.gregs[REG_RDX]);
  os << '\n';

  DumpRegister64(os, "rdi", context.gregs[REG_RDI]);
  DumpRegister64(os, "rsi", context.gregs[REG_RSI]);
  DumpRegister64(os, "rbp", context.gregs[REG_RBP]);
  DumpRegister64(os, "rsp", context.gregs[REG_RSP]);
  os << '\n';

  DumpRegister64(os, "r8 ", context.gregs[REG_R8]);
  DumpRegister64(os, "r9 ", context.gregs[REG_R9]);
  DumpRegister64(os, "r10", context.gregs[REG_R10]);
  DumpRegister64(os, "r11", context.gregs[REG_R11]);
  os << '\n';

  DumpRegister64(os, "r12", context.gregs[REG_R12]);
  DumpRegister64(os, "r13", context.gregs[REG_R13]);
  DumpRegister64(os, "r14", context.gregs[REG_R14]);
  DumpRegister64(os, "r15", context.gregs[REG_R15]);
  os << '\n';

  DumpRegister64(os, "rip", context.gregs[REG_RIP]);
  os << "   ";
  DumpRegister32(os, "eflags", context.gregs[REG_EFL]);
  DumpX86Flags(os, context.gregs[REG_EFL]);
  os << '\n';

  DumpRegister32(os, "cs",  (context.gregs[REG_CSGSFS]) & 0x0FFFF);
  DumpRegister32(os, "gs",  (context.gregs[REG_CSGSFS] >> 16) & 0x0FFFF);
  DumpRegister32(os, "fs",  (context.gregs[REG_CSGSFS] >> 32) & 0x0FFFF);
  os << '\n';
#elif defined(__linux__) && defined(__arm__)
  DumpRegister32(os, "r0", context.arm_r0);
  DumpRegister32(os, "r1", context.arm_r1);
  DumpRegister32(os, "r2", context.arm_r2);
  DumpRegister32(os, "r3", context.arm_r3);
  os << '\n';

  DumpRegister32(os, "r4", context.arm_r4);
  DumpRegister32(os, "r5", context.arm_r5);
  DumpRegister32(os, "r6", context.arm_r6);
  DumpRegister32(os, "r7", context.arm_r7);
  os << '\n';

  DumpRegister32(os, "r8", context.arm_r8);
  DumpRegister32(os, "r9", context.arm_r9);
  DumpRegister32(os, "r10", context.arm_r10);
  DumpRegister32(os, "fp", context.arm_fp);
  os << '\n';

  DumpRegister32(os, "ip", context.arm_ip);
  DumpRegister32(os, "sp", context.arm_sp);
  DumpRegister32(os, "lr", context.arm_lr);
  DumpRegister32(os, "pc", context.arm_pc);
  os << '\n';

  DumpRegister32(os, "cpsr", context.arm_cpsr);
  DumpArmStatusRegister(os, context.arm_cpsr);
  os << '\n';
#elif defined(__linux__) && defined(__aarch64__)
  for (size_t i = 0; i <= 30; ++i) {
    std::string reg_name = "x" + std::to_string(i);
    DumpRegister64(os, reg_name.c_str(), context.regs[i]);
    if (i % 4 == 3) {
      os << '\n';
    }
  }
  os << '\n';

  DumpRegister64(os, "sp", context.sp);
  DumpRegister64(os, "pc", context.pc);
  os << '\n';

  DumpRegister64(os, "pstate", context.pstate);
  DumpArmStatusRegister(os, context.pstate);
  os << '\n';
#else
  os << "Unknown architecture/word size/OS in ucontext dump";
#endif
}

void UContext::DumpRegister32(std::ostream& os, const char* name, uint32_t value) const {
  os << StringPrintf(" %6s: 0x%08x", name, value);
}

void UContext::DumpRegister64(std::ostream& os, const char* name, uint64_t value) const {
  os << StringPrintf(" %6s: 0x%016" PRIx64, name, value);
}

void UContext::DumpX86Flags(std::ostream& os, uint32_t flags) const {
  os << " [";
  if ((flags & (1 << 0)) != 0) {
    os << " CF";
  }
  if ((flags & (1 << 2)) != 0) {
    os << " PF";
  }
  if ((flags & (1 << 4)) != 0) {
    os << " AF";
  }
  if ((flags & (1 << 6)) != 0) {
    os << " ZF";
  }
  if ((flags & (1 << 7)) != 0) {
    os << " SF";
  }
  if ((flags & (1 << 8)) != 0) {
    os << " TF";
  }
  if ((flags & (1 << 9)) != 0) {
    os << " IF";
  }
  if ((flags & (1 << 10)) != 0) {
    os << " DF";
  }
  if ((flags & (1 << 11)) != 0) {
    os << " OF";
  }
  os << " ]";
}

template <typename RegisterType>
void UContext::DumpArmStatusRegister(std::ostream& os, RegisterType status_register) const {
  // Condition flags.
  constexpr RegisterType kFlagV = 1U << 28;
  constexpr RegisterType kFlagC = 1U << 29;
  constexpr RegisterType kFlagZ = 1U << 30;
  constexpr RegisterType kFlagN = 1U << 31;

  os << " [";
  if ((status_register & kFlagN) != 0) {
    os << " N";
  }
  if ((status_register & kFlagZ) != 0) {
    os << " Z";
  }
  if ((status_register & kFlagC) != 0) {
    os << " C";
  }
  if ((status_register & kFlagV) != 0) {
    os << " V";
  }
  os << " ]";
}

// @formatter:on

void DumpCurrentProcessWithUContext(void* uctx, android_LogPriority priority) {
    AndroidUnwinderData data;
    AndroidLocalUnwinder unwinder;
    bool result = unwinder.Unwind(const_cast<void*>(uctx), data);
    if (!result) {
        LOGE("Unwind failed for uctx: {}, error: {}", uctx, data.GetErrorString());
        return;
    }
    // dump ucontext
    UContext ucontext(uctx);
    std::ostringstream os;
    os << "UContext: " << std::endl;
    ucontext.Dump(os);
    __android_log_write(priority, "QAuxv", os.str().c_str());
    for (const auto& frame: data.frames) {
        __android_log_write(priority, "QAuxv", unwinder.FormatFrame(frame).c_str());
    }
}

} // utils
