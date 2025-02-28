#include <jni.h>
#include <regex>
#include <cstring>
#include <string>
#include <string_view>

#include <unistd.h>
#include <errno.h>
#include <dirent.h>

#include <linux_syscall_support.h>

#include "utils/Log.h"
#include "md5.h"

#define PKG_NAME "io.github.qauxv"
#define __STRING(x) #x
#define STRING(x) __STRING(x)

#if defined(__LP64__)
static_assert(sizeof(void *) == 8, "64-bit pointer size expected");
#else
static_assert(sizeof(void *) == 4, "32-bit pointer size expected");
#endif

#if defined(NDEBUG) || defined(TEST_SIGNATURE)
#if !defined(MODULE_SIGNATURE)
#error "MODULE_SIGNATURE must be defined for release build, but no signature key digest found in the signing config."
#error "Please set it in the signing config in build.gradle, or set it in local.properties with key 'qauxv.signature.md5digest'"
#endif
static_assert(sizeof(STRING(MODULE_SIGNATURE)) == 33);
#endif

namespace qauxv::utils {

int dumpMemory(int fd, const void *address, size_t size) {
    LOGD("dump memory: {}, {} to fd {}", address, size, fd);
    if (size == 0) {
        return 0;
    }
    if (address == nullptr) {
        return -EINVAL;
    }
    if (fd < 0) {
        return -EBADF;
    }
    size_t written = 0;
    while (written < size) {
        ssize_t ret = write(fd, (const uint8_t *) (address) + written, size - written);
        if (ret < 0) {
            if (errno == EINTR) {
                continue;
            } else {
                int err = errno;
                LOGE("error write({}, 0x{:x}, {}): {}", fd, (uintptr_t) address + written, size - written, strerror(err));
                return -err;
            }
        }
        written += ret;
    }
    return 0;
}

}

namespace teble::v2sign {

const uint32_t EOCD_MAGIC = 0x06054b50;
const uint32_t CD_MAGIC = 0x2014b50;
const uint8_t APK_SIGNING_BLOCK_MAGIC[16]{
        0x41, 0x50, 0x4b, 0x20, 0x53, 0x69, 0x67, 0x20,
        0x42, 0x6c, 0x6f, 0x63, 0x6b, 0x20, 0x34, 0x32
};
const uint32_t SIGNATURE_SCHEME_V2_MAGIC = 0x7109871a;

std::vector <uint8_t> readSignBlock(int fd) {
    auto seek_and_read = [](int fd, int64_t offset, void* buffer, size_t size) -> bool {
        if (lseek(fd, offset, SEEK_CUR) == -1) {
            LOGE("Failed to seek file");
            return false;
        }
        if (read(fd, buffer, size) != (ssize_t) size) {
            LOGE("Failed to read file");
            return false;
        }
        return true;
    };

    off_t file_size = lseek(fd, 0, SEEK_END);
    if (file_size <= 0) {
        LOGE("Invalid file size");
        return {};
    }

    uint32_t magic = 0;

    for (uint16_t i = 0; i <= 0xffff; ++i) {
        uint16_t comment_size = 0;
        if (!seek_and_read(fd, -(i + 2), &comment_size, sizeof(comment_size))) {
            return {};
        }

        if (comment_size == i) {
            if (!seek_and_read(fd, -22, &magic, sizeof(magic))) {
                return {};
            }
            if (magic == EOCD_MAGIC) {
                LOGD("EndOfCentralDirectory off: 0x{:x}", (uint32_t)(file_size - i - 22));
                break;
            }
        }

        if (i == 0xffff) {
            return {};
        }
    }

    uint32_t central_directory_off = 0;
    if (!seek_and_read(fd, 12, &central_directory_off, sizeof(central_directory_off))) {
        return {};
    }

    LOGD("CentralDirectory off: 0x{:x}", central_directory_off);

    if (lseek(fd, central_directory_off, SEEK_SET) == -1) {
        LOGE("Failed to seek to CentralDirectory");
        return {};
    }
    if (read(fd, &magic, sizeof(magic)) != sizeof(magic) || magic != CD_MAGIC) {
        LOGE("Invalid CentralDirectory magic");
        return {};
    }

    uint64_t signing_block_size = 0;
    if (!seek_and_read(fd, -28, &signing_block_size, sizeof(signing_block_size))) {
        return {};
    }

    LOGD("signing_block_size: {}", signing_block_size);

    uint8_t magic_buf[16] = {0};
    if (!seek_and_read(fd, 0, magic_buf, sizeof(magic_buf))) {
        return {};
    }
    if (std::memcmp(magic_buf, APK_SIGNING_BLOCK_MAGIC, sizeof(magic_buf)) != 0) {
        LOGE("Invalid signing block magic");
        return {};
    }

    std::vector <uint8_t> sign_block(signing_block_size);
    if (!seek_and_read(fd, (int64_t) - signing_block_size, sign_block.data(), signing_block_size)) {
        return {};
    }

    return sign_block;
}

std::vector <uint8_t> getV2SignCert(std::vector <uint8_t>& block) {
    std::vector <uint8_t> certificate;
    auto* p = block.data();
    auto* end_p = block.data() + block.size();
    while (p < end_p) {
        uint64_t blockSize = 0;
        for (int i = 0; i < 8; ++i) {
            blockSize = (blockSize >> 8) | (((uint64_t) * p++) << (8 * 7));
        }
        uint32_t id = 0;
        for (int i = 0; i < 4; ++i) {
            id = (id >> 8) | (((uint32_t) * p++) << (8 * 3));
        }
        if (id != SIGNATURE_SCHEME_V2_MAGIC) {
            p += blockSize - 12;
            continue;
        }
        p += 12;
        uint32_t size = 0;
        for (int i = 0; i < 4; ++i) {
            size = (size >> 8) | (((uint32_t) * p++) << (8 * 3));
        }
        p += size + 4;
        for (int i = 0; i < 4; ++i) {
            size = (size >> 8) | (((uint32_t) * p++) << (8 * 3));
        }
        for (int i = 0; i < size; ++i) {
            certificate.push_back(*p++);
        }
        break;
    }

    return certificate;
}


std::string getModulePath(JNIEnv* env) {
    jclass cMainHook = env->FindClass("io/github/qauxv/core/MainHook");
    jclass cClass = env->FindClass("java/lang/Class");
    jmethodID mGetClassLoader = env->GetMethodID(cClass, "getClassLoader",
                                                 "()Ljava/lang/ClassLoader;");
    jobject classloader = env->CallObjectMethod(cMainHook, mGetClassLoader);
    jclass cClassloader = env->FindClass("java/lang/ClassLoader");
    jmethodID mGetResource = env->GetMethodID(cClassloader, "findResource",
                                              "(Ljava/lang/String;)Ljava/net/URL;");
    jstring manifestPath = env->NewStringUTF("AndroidManifest.xml");
    jobject url = env->CallObjectMethod(classloader, mGetResource, manifestPath);
    jclass cURL = env->FindClass("java/net/URL");
    jmethodID mGetPath = env->GetMethodID(cURL, "getPath", "()Ljava/lang/String;");
    auto file = (jstring) env->CallObjectMethod(url, mGetPath);
    const char* cStr = env->GetStringUTFChars(file, nullptr);
    std::string filePathStr(cStr);
    if (filePathStr.empty()) {
        return {};
    }
    std::string s = filePathStr.substr(5, filePathStr.size() - 26);
    env->ReleaseStringUTFChars(file, cStr);
    return s;
}

static bool string2int(const std::string& str, int& value) {
    char* endptr = nullptr;
    value = static_cast<int>(strtol(str.c_str(), &endptr, 10));
    return endptr != str.c_str() && *endptr == '\0';
}

int getModulePathFd(JNIEnv* env) {
    auto apkPath = getModulePath(env);
    return sys_openat(AT_FDCWD, apkPath.c_str(), O_RDONLY, 0);
}

int getSelfApkFd(JNIEnv* env) {
    // walk through /proc/pid/fd to find the apk path
    std::string selfFdDir = "/proc/" + std::to_string(getpid()) + "/fd";
    DIR* dir = opendir(selfFdDir.c_str());
    if (dir == nullptr) {
        return -1;
    }
    struct dirent* entry;
    while ((entry = readdir(dir)) != nullptr) {
        if (entry->d_type != DT_LNK) {
            continue;
        }
        std::string linkPath = selfFdDir + "/" + entry->d_name;
        char buf[PATH_MAX] = {};
        ssize_t len = sys_readlinkat(AT_FDCWD, linkPath.c_str(), buf, sizeof(buf));
        if (len < 0) {
            continue;
        }
        buf[len] = '\0';
        std::string path(buf);
        if (path.starts_with("/data/app/") && path.find(PKG_NAME) != std::string::npos && path.ends_with(".apk")) {
            closedir(dir);
            int resultFd = -1;
            if (string2int(entry->d_name, resultFd)) {
                return resultFd;
            }
            return -1;
        }
    }
    closedir(dir);
    return -1;
}

std::string getBlockMd5(const std::string& block) {
    return MD5(block).getDigest();
}

bool checkSignature(JNIEnv* env, bool isInHostAsModule) {
    int fd;
    if (isInHostAsModule) {
        fd = getModulePathFd(env);
    } else {
        fd = getSelfApkFd(env);
    }
    // fd is stolen from system, so we don't need to close it
    if (fd < 0) {
        LOGE("getSelfApkFd failed");
        return false;
    }
    int ret;
    size_t fileSize = 0;
#if defined(__LP64__)
    kernel_stat stat = {};
    ret = sys_fstat(fd, &stat);
    fileSize = stat.st_size;
#else
    kernel_stat64 stat = {};
    ret = sys_fstat64(fd, &stat);
    fileSize = stat.st_size;
#endif
    if (ret < 0) {
        sys_close(fd);
        LOGE("fstat failed");
        return false;
    }
    auto signBlock = readSignBlock(fd);
    if (isInHostAsModule) {
        sys_close(fd);
    }
    auto cert = getV2SignCert(signBlock);
    std::string cert_str(cert.begin(), cert.end());
    std::string md5 = getBlockMd5(cert_str);
    std::string str(STRING(MODULE_SIGNATURE));
    LOGD("cal md5: {}", md5.c_str());
    LOGD("rel md5: {}", str.c_str());
    auto match = md5 == str;

    if (!isInHostAsModule && !match) {
        sys_kill(sys_getpid(), SIGKILL);
    }
    return match;
}

}

#undef __STRING
#undef STRING
