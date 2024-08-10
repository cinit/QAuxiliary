//
// Created by sulfate on 2024-08-09.
//


#include "Log.h"

#include <cstring>
#include <cstdlib>
#include <unistd.h>

#include <android/log.h>
#include <android/set_abort_message.h>

namespace qauxv::utils {

[[noreturn]] void Abort(std::string_view msg) noexcept {
    if (!msg.empty()) {
        static const char oomWhenAborting[] = "Out of memory when trying to allocate memory for abort message.";
        auto* buf = reinterpret_cast<char*>(malloc(msg.size() + 1));
        size_t len;
        if (buf == nullptr) {
            len = sizeof(oomWhenAborting);
            buf = const_cast<char*>(oomWhenAborting);
        } else {
            len = msg.size();
            std::memcpy(buf, msg.data(), len);
            buf[len] = '\0';
        }
#ifdef __ANDROID__
        __android_log_write(ANDROID_LOG_FATAL, "DEBUG", buf);
        android_set_abort_message(buf);
#else
        ::write(STDERR_FILENO, buf, len);
#endif
    }
    ::abort();
}

}
