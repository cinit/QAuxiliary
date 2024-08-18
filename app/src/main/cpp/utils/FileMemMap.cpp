//
// Created by kinit on 2021-10-28.
//

#include <unistd.h>
#include <fcntl.h>
#include <cerrno>
#include <sys/mman.h>
#include <sys/user.h>
#include <sys/stat.h>

#include "FileMemMap.h"
#include "MemoryUtils.h"

FileMemMap::~FileMemMap() noexcept {
    if (mAddress != nullptr) {
        unmap();
    }
}

int FileMemMap::mapFilePath(const char* path, bool readOnly, size_t length) {
    int fd = open(path, O_RDONLY);
    if (fd < 0) {
        return errno;
    }
    if (length == 0) {
        struct stat64 fileInfo = {};
        if (fstat64(fd, &fileInfo) < 0) {
            int err = errno;
            close(fd);
            return err;
        }
        length = size_t(fileInfo.st_size);
    }
    if (length == 0) {
        close(fd);
        return EINVAL;
    }
    size_t kPageSize = utils::GetPageSize();
    size_t pageAlignedSize = (length + kPageSize - 1u) & ~(kPageSize - 1u);
    void* addr = mmap(nullptr, pageAlignedSize, readOnly ? PROT_READ : (PROT_READ | PROT_WRITE), MAP_PRIVATE, fd, 0);
    if (addr == MAP_FAILED) {
        int err = errno;
        close(fd);
        return err;
    }
    close(fd);
    mAddress = addr;
    mLength = length;
    mMapLength = pageAlignedSize;
    return 0;
}

int FileMemMap::mapFileDescriptor(int fd, bool readOnly, size_t length, bool shared) {
    if (fd < 0) {
        return EBADFD;
    }
    if (length == 0) {
        struct stat64 fileInfo = {};
        if (fstat64(fd, &fileInfo) < 0) {
            return errno;
        }
        length = size_t(fileInfo.st_size);
    }
    if (length == 0) {
        return EINVAL;
    }
    size_t kPageSize = utils::GetPageSize();
    size_t pageAlignedSize = (length + kPageSize - 1u) & ~(kPageSize - 1u);
    void* addr = mmap(nullptr, pageAlignedSize, readOnly ? PROT_READ : (PROT_READ | PROT_WRITE),
                      shared ? MAP_SHARED : MAP_PRIVATE, fd, 0);
    if (addr == MAP_FAILED) {
        return errno;
    }
    mAddress = addr;
    mLength = length;
    mMapLength = pageAlignedSize;
    return 0;
}

void FileMemMap::unmap() noexcept {
    if (mAddress != nullptr && munmap(mAddress, mMapLength) == 0) {
        mAddress = nullptr;
        mMapLength = 0;
        mLength = 0;
    }
}

void FileMemMap::detach() noexcept {
    mAddress = nullptr;
    mLength = 0;
    mMapLength = 0;
}
