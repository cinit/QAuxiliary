#ifndef QAUXV_LOG_H
#define QAUXV_LOG_H

#ifdef __cplusplus

#include <string_view>
#include <fmt/format.h>

namespace qauxv::utils {

[[noreturn]] void Abort(std::string_view msg) noexcept;

}

#ifdef __ANDROID__

#include <android/log.h>
#include <android/set_abort_message.h>

#else

typedef enum android_LogPriority {
    /** For internal use only.  */
    ANDROID_LOG_UNKNOWN = 0,
    /** The default priority, for internal use only.  */
    ANDROID_LOG_DEFAULT, /* only for SetMinPriority() */
    /** Verbose logging. Should typically be disabled for a release apk. */
    ANDROID_LOG_VERBOSE,
    /** Debug logging. Should typically be disabled for a release apk. */
    ANDROID_LOG_DEBUG,
    /** Informational logging. Should typically be disabled for a release apk. */
    ANDROID_LOG_INFO,
    /** Warning logging. For use with recoverable failures. */
    ANDROID_LOG_WARN,
    /** Error logging. For use with unrecoverable failures. */
    ANDROID_LOG_ERROR,
    /** Fatal logging. For use when aborting. */
    ANDROID_LOG_FATAL,
    /** For internal use only.  */
    ANDROID_LOG_SILENT, /* only for SetMinPriority(); must be last */
} android_LogPriority;

int __android_log_print(int prio, const char* tag, const char* fmt, ...) __attribute__((__format__(printf, 3, 4)));

#endif

#define LOGD(...) ::__android_log_write(ANDROID_LOG_DEBUG, "QAuxv", ::fmt::format(__VA_ARGS__).c_str())
#define LOGI(...) ::__android_log_write(ANDROID_LOG_INFO, "QAuxv", ::fmt::format(__VA_ARGS__).c_str())
#define LOGW(...) ::__android_log_write(ANDROID_LOG_WARN, "QAuxv", ::fmt::format(__VA_ARGS__).c_str())
#define LOGE(...) ::__android_log_write(ANDROID_LOG_ERROR, "QAuxv", ::fmt::format(__VA_ARGS__).c_str())

#define CHECK(cond_) if (!(cond_)) [[unlikely]] { ::android_set_abort_message("check fail: " # cond_); ::abort(); } ; (void) 0

#endif

#endif //QAUXV_LOG_H
