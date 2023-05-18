//
// Created by kinit on 2021-06-24.
//

#include "shared_memory.h"
#include <unistd.h>
#include <cstdint>
#include <sys/fcntl.h>
#include <sys/stat.h>
#include <sys/ioctl.h>
#include <sys/syscall.h>
#include <linux/ioctl.h>
#include <linux/types.h>
#include <linux/memfd.h>
#include <cerrno>
#include <cstring>
#include <cstdio>

#define ASHMEM_NAME_DEF "dev/ashmem"
#define ASHMEM_NOT_PURGED 0
#define ASHMEM_WAS_PURGED 1
#define ASHMEM_IS_UNPINNED 0
#define ASHMEM_IS_PINNED 1
struct ashmem_pin {
    __u32 offset;
    __u32 len;
};

#define __ASHMEMIOC 0x77
#define ASHMEM_SET_NAME _IOW(__ASHMEMIOC, 1, char[ASHMEM_NAME_LEN])
#define ASHMEM_GET_NAME _IOR(__ASHMEMIOC, 2, char[ASHMEM_NAME_LEN])
#define ASHMEM_SET_SIZE _IOW(__ASHMEMIOC, 3, size_t)
#define ASHMEM_GET_SIZE _IO(__ASHMEMIOC, 4)
#define ASHMEM_SET_PROT_MASK _IOW(__ASHMEMIOC, 5, unsigned long)
#define ASHMEM_GET_PROT_MASK _IO(__ASHMEMIOC, 6)
#define ASHMEM_PIN _IOW(__ASHMEMIOC, 7, struct ashmem_pin)
#define ASHMEM_UNPIN _IOW(__ASHMEMIOC, 8, struct ashmem_pin)
#define ASHMEM_GET_PIN_STATUS _IO(__ASHMEMIOC, 9)
#define ASHMEM_PURGE_ALL_CACHES _IO(__ASHMEMIOC, 10)
#define ASHMEM_NAME_LEN 256

static int memfd_create_region_post_q(const char *name, size_t size) {
    // This code needs to build on old API levels, so we can't use the libc
    // wrapper.
    int fd = (int) syscall(__NR_memfd_create, name, MFD_CLOEXEC | MFD_ALLOW_SEALING);
    if (fd == -1) {
        return -errno;
    }
    if (ftruncate(fd, size) == -1) {
        int orig = errno;
        close(fd);
        errno = orig;
        return -orig;
    }
    return fd;
}

static bool testMemfdSupport() {
    int fd = (int) syscall(__NR_memfd_create, "test_support_memfd", MFD_CLOEXEC | MFD_ALLOW_SEALING);
    if (fd == -1) {
        return false;
    }
    if (fcntl(fd, F_ADD_SEALS, F_SEAL_FUTURE_WRITE) == -1) {
        close(fd);
        return false;
    }
    close(fd);
    return true;
}

bool has_memfd_support() {
    /*
     * memfd is available when Build.SDK >= Q
     * /dev/ashmem is no longer accessible when SDK >= Q
     */
    static bool memfd_supported = testMemfdSupport();
    return memfd_supported;
}


/* logistics of getting file descriptor for ashmem */
static int ashmem_open_legacy() {
    int fd = TEMP_FAILURE_RETRY(open("/dev/ashmem", O_RDWR | O_CLOEXEC));
    struct stat st = {};
    int ret = TEMP_FAILURE_RETRY(fstat(fd, &st));
    if (ret < 0) {
        int save_errno = errno;
        close(fd);
        errno = save_errno;
        return ret;
    }
    if (!S_ISCHR(st.st_mode) || !st.st_rdev) {
        close(fd);
        errno = ENOTTY;
        return -1;
    }
    return fd;
}

/*
 * ashmem_create_region - creates a new ashmem region and returns the file
 * descriptor, or < 0 on error
 *
 * `name' is an optional label to give the region (visible in /proc/pid/maps)
 * `size' is the size of the region, in page-aligned bytes
 */
int ashmem_create_region(const char *name, size_t size) {
    int ret, save_errno;
    size_t pagesize = getpagesize();
    if (size % pagesize != 0) {
        size = pagesize * (size / pagesize + 1);
    }
    if (has_memfd_support()) {
        return memfd_create_region_post_q(name ? name : "none", size);
    }
    int fd = ashmem_open_legacy();
    if (fd < 0) {
        return fd;
    }
    if (name) {
        char buf[ASHMEM_NAME_LEN] = {0};
        strncpy(buf, name, sizeof(buf));
        ret = TEMP_FAILURE_RETRY(ioctl(fd, ASHMEM_SET_NAME, buf));
        if (ret < 0) {
            goto L_ERROR;
        }
    }
    ret = TEMP_FAILURE_RETRY(ioctl(fd, ASHMEM_SET_SIZE, size));
    if (ret < 0) {
        goto L_ERROR;
    }
    return fd;
    L_ERROR:
    save_errno = errno;
    close(fd);
    errno = save_errno;
    return -save_errno;
}

int create_in_memory_file(const char *dir, const char *name, size_t size) {
    if (dir == nullptr || name == nullptr) {
        return -EINVAL;
    }
    if (has_memfd_support()) {
        return memfd_create_region_post_q(name, size);
    }
    // no memfd support, fallback to open+unlink
    // check dir writable
    if (access(dir, W_OK) != 0) {
        return -errno;
    }
    if (strlen(name) + strlen(dir) + 2 > PATH_MAX) {
        return -ENAMETOOLONG;
    }
    char path[PATH_MAX] = {};
    snprintf(path, PATH_MAX, "%s/%s", dir, name);
    // check file not exist
    if (access(path, F_OK) == 0) {
        // unlink existing file
        if (TEMP_FAILURE_RETRY(unlink(path)) != 0) {
            return -errno;
        }
    }
    // create file
    int fd = TEMP_FAILURE_RETRY(open(path, O_RDWR | O_CREAT | O_CLOEXEC | O_EXCL, 0600));
    if (fd < 0) {
        return -errno;
    }
    // unlink file
    if (TEMP_FAILURE_RETRY(unlink(path)) != 0) {
        int save_errno = errno;
        close(fd);
        errno = save_errno;
        return -save_errno;
    }
    // truncate file
    if (TEMP_FAILURE_RETRY(ftruncate(fd, size)) != 0) {
        int save_errno = errno;
        close(fd);
        errno = save_errno;
        return -save_errno;
    }
    return fd;
}

int copy_file_to_memfd(int fd, const char *name) {
    if (!has_memfd_support()) {
        return -ENOSYS;
    }
    struct stat64 fileInfo = {};
    if (fstat64(fd, &fileInfo) < 0) {
        return -errno;
    }
    int64_t length = int64_t(fileInfo.st_size);
    if (length <= 0) {
        return -EINVAL;
    }
    if (length > 64 * 1024 * 1024) {
        return -ENOMEM;
    }
    int64_t originOffset = lseek64(fd, 0, SEEK_CUR);
    if (originOffset < 0) {
        return -errno;
    }
    int memfd = (int) syscall(__NR_memfd_create, name ? name : "none", MFD_CLOEXEC | MFD_ALLOW_SEALING);
    if (memfd == -1) {
        return -errno;
    }
    if (ftruncate(memfd, length) == -1) {
        int orig = errno;
        close(memfd);
        errno = orig;
        return -orig;
    }
    if (lseek(fd, 0, SEEK_SET) < 0) {
        int err = errno;
        close(memfd);
        return -err;
    }
    char buf[4096];
    ssize_t i;
    while ((i = read(fd, buf, 4096)) > 0) {
        write(memfd, buf, i);
    }
    if (i != 0) {
        int err = errno;
        lseek64(fd, originOffset, SEEK_SET);
        close(memfd);
        return -err;
    }
    lseek64(memfd, 0, SEEK_SET);
    lseek64(fd, originOffset, SEEK_SET);
    return memfd;
}
