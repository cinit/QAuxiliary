//
// Created by kinit on 2023-01-03.
//

#include <cerrno>
#include <unistd.h>

#include "auto_close_fd.h"

auto_close_fd::~auto_close_fd() {
    if (mFd >= 0) {
        TEMP_FAILURE_RETRY(::close(mFd));
    }
}

void auto_close_fd::close() noexcept {
    if (mFd >= 0) {
        TEMP_FAILURE_RETRY(::close(mFd));
        mFd = -1;
    }
}
