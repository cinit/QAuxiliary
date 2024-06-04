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
#include "zip_helper.h"
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
    const char magic[16]{
            0x32, 0x34, 0x20, 0x6b, 0x63, 0x6f, 0x6c, 0x42,
            0x20, 0x67, 0x69, 0x53, 0x20, 0x4b, 0x50, 0x41,
    };

    const uint64_t revertMagikFirst = 0x3234206b636f6c42LL;
    const uint64_t revertMagikSecond = 0x20676953204b5041LL;
    const uint32_t v2Id = 0x7109871a;

    std::string getModulePath(JNIEnv *env) {
        jclass cMainHook = env->FindClass("io/github/qauxv/startup/HookEntry");
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
        const char *cStr = env->GetStringUTFChars(file, nullptr);
        std::string filePathStr(cStr);
        if (filePathStr.empty()) {
            return std::string();
        }
        std::string s = filePathStr.substr(5, filePathStr.size() - 26);
        env->ReleaseStringUTFChars(file, cStr);
        return s;
    }

    static bool string2int(const std::string &str, int &value) {
        char *endptr = nullptr;
        value = static_cast<int>(strtol(str.c_str(), &endptr, 10));
        return endptr != str.c_str() && *endptr == '\0';
    }

    int getSelfApkFd(JNIEnv *env) {
        // walk through /proc/pid/fd to find the apk path
        std::string selfFdDir = "/proc/" + std::to_string(getpid()) + "/fd";
        DIR *dir = opendir(selfFdDir.c_str());
        if (dir == nullptr) {
            return -1;
        }
        struct dirent *entry;
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

    std::string getSignBlock(uint8_t *base, size_t size) {
        zip_helper::MemMap memMap(base, size);
        auto zip_file = zip_helper::ZipFile::Open(memMap);
        if (!zip_file || zip_file->entries.size() == 0) return {};
        auto last_entry = zip_file->entries.back();
        auto zip_entries_end_ptr = reinterpret_cast<uint8_t *>(last_entry->record) + last_entry->getEntrySize();
        auto apk_end_ptr = memMap.addr() + memMap.len();
        uint64_t curr = 0;
        uint8_t *ptr = apk_end_ptr - 1;
        std::string signBlock;
        while (ptr >= zip_entries_end_ptr) {
            curr = (curr << 8) | *ptr--;
            if (curr == revertMagikFirst) {
                uint64_t tmp = 0;
                for (int i = 0; i < 8; ++i) {
                    tmp = (tmp << 8) | *(ptr - i);
                }
                if (tmp == revertMagikSecond) {
                    for (int i = 8; i < 16; ++i) {
                        tmp = (tmp << 8) | *(ptr - i);
                    }
                    // TODO 只判断魔数“APK Sig Block 42”可能存在误判
                    ptr -= 16;
                    tmp -= 24;
                    for (int i = 0; i < tmp; ++i) {
                        signBlock.push_back(*ptr--);
                    }
                    break;
                }
            }
        }
        std::reverse(signBlock.begin(), signBlock.end());
        return signBlock;
    }

    std::string getBlockMd5(const std::string &block) {
        return MD5(block).getDigest();
    }

    std::string getV2Signature(std::string_view block) {
        std::string signature;
        const char *p = block.data();
        const char *last = block.data() + block.size();
        while (p < last) {
            uint64_t blockSize = 0;
            for (int i = 0; i < 8; ++i) {
                blockSize = (blockSize >> 8) | (((uint64_t) *p++) << (8 * 7));
            }
            uint32_t id = 0;
            for (int i = 0; i < 4; ++i) {
                id = (id >> 8) | (((uint32_t) *p++) << (8 * 3));
            }
            if (id != v2Id) {
                p += blockSize - 12;
                continue;
            }
            p += 12;
            uint32_t size = 0;
            for (int i = 0; i < 4; ++i) {
                size = (size >> 8) | (((uint32_t) *p++) << (8 * 3));
            }
            p += size + 4;
            for (int i = 0; i < 4; ++i) {
                size = (size >> 8) | (((uint32_t) *p++) << (8 * 3));
            }
            for (int i = 0; i < size; ++i) {
                signature.push_back(*p++);
            }
            break;
        }
        return signature;
    }

    bool checkSignature(JNIEnv *env, bool isInHostAsModule) {
        void *baseAddress = nullptr;
        size_t fileSize = 0;
        if (isInHostAsModule) {
            std::string apkPath = getModulePath(env);
            if (apkPath.empty()) {
                LOGE("getModulePath failed");
                return false;
            }
            int fd = sys_openat(AT_FDCWD, apkPath.c_str(), O_RDONLY, 0);
            if (fd < 0) {
                LOGE("open apk failed");
                return false;
            }
            int ret;
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
            baseAddress = sys_mmap(nullptr, fileSize, PROT_READ, MAP_PRIVATE, fd, 0);
            if (baseAddress == MAP_FAILED) {
                sys_close(fd);
                LOGE("mmap failed");
                return false;
            }
            sys_close(fd);
        } else {
            int fd = getSelfApkFd(env);
            // fd is stolen from system, so we don't need to close it
            if (fd < 0) {
                LOGE("getSelfApkFd failed");
                return false;
            }
            int ret;
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
                LOGE("fstat failed");
                return false;
            }
            baseAddress = sys_mmap(nullptr, fileSize, PROT_READ, MAP_PRIVATE, fd, 0);
            if (baseAddress == MAP_FAILED) {
                LOGE("mmap failed");
                return false;
            }
        }
        // __android_log_print(ANDROID_LOG_INFO, "QAuxv", "isModule: %d, path: %s", isModule, path.c_str());
        std::string block = getSignBlock(reinterpret_cast<uint8_t *>(baseAddress), fileSize);

        bool match;
        if (block.empty()) {
            sys_munmap(const_cast<void *>(baseAddress), fileSize);
            LOGE("sign block is empty");
            return false;
        } else {
            std::string currSignature = getV2Signature(block);
            std::string md5 = getBlockMd5(currSignature);
            std::string str(STRING(MODULE_SIGNATURE));
            sys_munmap(const_cast<void *>(baseAddress), fileSize);
            match = str == md5;
        }

        if (!isInHostAsModule && !match) {
            sys_kill(sys_getpid(), SIGKILL);
        }
        return match;
    }
}

#undef __STRING
#undef STRING
