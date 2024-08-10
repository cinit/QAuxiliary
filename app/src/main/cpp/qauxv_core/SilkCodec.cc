#include <jni.h>
#include <unistd.h>
#include <cstdint>
#include <cstring>
#include <string>
#include <vector>
#include <malloc.h>
#include <cerrno>
#include <fcntl.h>
#include <sys/mman.h>
#include <android/log.h>

#include "utils/auto_close_fd.h"
#include "SKP_Silk_SDK_API.h"
#include "qauxv_core/jni_method_registry.h"

/* Define codec specific settings */
#define MAX_BYTES_PER_FRAME     250 // Equals peak bitrate of 100 kbps
#define MAX_INPUT_FRAMES        5
#define FRAME_LENGTH_MS         20
#define MAX_API_FS_KHZ          48

void throwIOException(JNIEnv *env, const char *msg) {
    jclass exceptionClass = env->FindClass("java/io/IOException");
    env->ThrowNew(exceptionClass, msg);
}

std::string jstring2string(JNIEnv *env, jstring jstr) {
    if (jstr == nullptr) {
        return "";
    }
    const char *cstr = env->GetStringUTFChars(jstr, nullptr);
    std::string str(cstr);
    env->ReleaseStringUTFChars(jstr, cstr);
    return str;
}

void __attribute__((format(printf, 2, 3))) throwIOExceptionF(JNIEnv *env, const char *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    char msg[1024] = {};
    vsnprintf(msg, sizeof(msg), fmt, args);
    va_end(args);
    throwIOException(env, msg);
}

int writeFully(int fd, const void *buf, size_t count) {
    const auto *p = (const uint8_t *) buf;
    while (count > 0) {
        ssize_t n = write(fd, p, count);
        if (n < 0) {
            return -errno;
        }
        p += n;
        count -= n;
    }
    return 0;
}

int writeOrThrow(JNIEnv *env, int fd, const void *buf, size_t count) {
    int ret = writeFully(fd, buf, count);
    if (ret < 0) {
        throwIOExceptionF(env, "write(%d, %p, %zu) failed: %s", fd, buf, count, strerror(-ret));
    }
    return ret;
}

void convertPcm16leToSilk(
        JNIEnv *env,
        jint input_fd,
        jint output_fd,
        jint sample_rate,
        jint bit_rate,
        jint packet_size,
        jboolean tencent
) {
    auto_close_fd _input(input_fd);
    auto_close_fd _output(output_fd);
    // get input file size
    off_t input_size = lseek(input_fd, 0, SEEK_END);
    if (input_size < 0) {
        throwIOExceptionF(env, "lseek() failed: %s", strerror(errno));
        return;
    }
    if (lseek(input_fd, 0, SEEK_SET) < 0) {
        throwIOExceptionF(env, "lseek() failed: %s", strerror(errno));
        return;
    }
    if (lseek(output_fd, 0, SEEK_SET) < 0) {
        throwIOExceptionF(env, "lseek(output_fd, 0, SEEK_SET) failed: %s", strerror(errno));
        return;
    }
    // truncate output file
    if (ftruncate(output_fd, 0) < 0) {
        throwIOExceptionF(env, "ftruncate(output_fd, 0) failed: %s", strerror(errno));
        return;
    }
    /* Add Silk header to stream */
    {
        if (tencent) {
            static const char Tencent_break[] = {2};
            if (writeOrThrow(env, output_fd, Tencent_break, 1) < 0) {
                return;
            }
        }
        static const char Silk_header[] = "#!SILK_V3";
        if (writeOrThrow(env, output_fd, Silk_header, sizeof(Silk_header) - 1) < 0) {
            return;
        }
    }
    SKP_int32 encSizeBytes;
    /* Create Encoder */
    int ret = SKP_Silk_SDK_Get_Encoder_Size(&encSizeBytes);
    if (ret) {
        throwIOExceptionF(env, "SKP_Silk_SDK_Get_Encoder_Size returned %d\n", ret);
        return;
    }
    std::vector<uint8_t> encoder_state;
    encoder_state.resize(encSizeBytes);
    void *psEnc = encoder_state.data();

    SKP_SILK_SDK_EncControlStruct encControl = {}; // Struct for input to encoder
    SKP_SILK_SDK_EncControlStruct encStatus = {};  // Struct for status of encoder
    /* Reset Encoder */
    ret = SKP_Silk_SDK_InitEncoder(psEnc, &encStatus);
    if (ret) {
        throwIOExceptionF(env, "SKP_Silk_SDK_InitEncoder returned %d\n", ret);
        return;
    }

    /* Set Encoder parameters */
    encControl.API_sampleRate = sample_rate;
    encControl.maxInternalSampleRate = sample_rate;
    encControl.packetSize = packet_size;
    encControl.packetLossPercentage = 0;
    encControl.useInBandFEC = 0;
    encControl.useDTX = 0;
    encControl.complexity = 2;
    encControl.bitRate = bit_rate;

    if (sample_rate > MAX_API_FS_KHZ * 1000 || sample_rate < 0) {
        throwIOExceptionF(env, "Error: API sampling rate = %d out of range, valid range 8000 - 48000", sample_rate);
        return;
    }
    class UnmapHelper {
     private:
      void *addr;
      size_t length;
     public:
      UnmapHelper(void *addr, size_t length) : addr(addr), length(length) {}

      ~UnmapHelper() {
          if (addr) {
              munmap(addr, length);
          }
      }

      void release() {
          addr = nullptr;
          length = 0;
      }
    };

    void *input_addr = mmap(nullptr, input_size, PROT_READ, MAP_SHARED, input_fd, 0);
    if (input_addr == MAP_FAILED) {
        throwIOExceptionF(env, "mmap() failed: %s", strerror(errno));
        return;
    }
    UnmapHelper _input_addr(input_addr, input_size);
    int frameSizeReadFromFile_ms = 20;
    auto *inputBase = reinterpret_cast<SKP_int16 *> (input_addr);
    int totalSampleCount = (int) (input_size / sizeof(SKP_int16));
    int offset = 0;
    constexpr auto outputBufferSize = MAX_BYTES_PER_FRAME * MAX_INPUT_FRAMES;
    SKP_uint8 outputBuffer[outputBufferSize];
    int outputBufferOffset = 0;
    int smplsSinceLastPacket = 0;
    while (true) {
        /* Read input from file */
        int count = (frameSizeReadFromFile_ms * sample_rate) / 1000;
        if (offset + count > totalSampleCount) {
            break;
        }
        auto nBytes = (SKP_int16) (outputBufferSize - outputBufferOffset);
        /* Silk Encoder */
        ret = SKP_Silk_SDK_Encode(psEnc, &encControl,
                                  inputBase + offset, (SKP_int16) count,
                                  outputBuffer + outputBufferOffset, &nBytes);
        if (ret) {
            throwIOExceptionF(env, "SKP_Silk_Encode returned %d", ret);
            return;
        }
        offset += count;
        outputBufferOffset += nBytes;
        /* Get packet size */
        auto packetSize_ms = (SKP_int) ((1000 * (SKP_int32) encControl.packetSize) / encControl.API_sampleRate);
        smplsSinceLastPacket += (SKP_int) count;
        if (((1000 * smplsSinceLastPacket) / sample_rate) == packetSize_ms) {
            /* Sends a dummy zero size packet in case of DTX period  */
            /* to make it work with the decoder test program.        */
            /* In practice should be handled by RTP sequence numbers */
            /* Write payload size */
            nBytes = (SKP_int16) outputBufferOffset;
            if (writeOrThrow(env, output_fd, &nBytes, sizeof(SKP_int16)) < 0) {
                return;
            }
            /* Write payload */
            if (writeOrThrow(env, output_fd, outputBuffer, outputBufferOffset) < 0) {
                return;
            }
            smplsSinceLastPacket = 0;
            outputBufferOffset = 0;
        }
    }
    /* Write dummy because it can not end with 0 bytes */
    SKP_int16 nBytes = -1;

    /* Write payload size */
    if (!tencent) {
        if (writeOrThrow(env, output_fd, &nBytes, sizeof(SKP_int16)) < 0) {
            return;
        }
    }

}

extern "C"
JNIEXPORT void JNICALL
Java_io_github_qauxv_util_ptt_SilkEncodeUtils_nativePcm16leToSilkII(JNIEnv *env,
                                                                    jclass,
                                                                    jint input_fd,
                                                                    jint output_fd,
                                                                    jint sample_rate,
                                                                    jint bit_rate,
                                                                    jint packet_size,
                                                                    jboolean tencent) {
    __android_log_print(ANDROID_LOG_INFO,
                        "QAuxv",
                        "nativePcm16leToSilkII: %d %d %d %d %d %d",
                        input_fd,
                        output_fd,
                        sample_rate,
                        bit_rate,
                        packet_size,
                        tencent);
    convertPcm16leToSilk(env, input_fd, output_fd, sample_rate, bit_rate, packet_size, tencent);
}

extern "C"
JNIEXPORT void JNICALL
Java_io_github_qauxv_util_ptt_SilkEncodeUtils_nativePcm16leToSilkIS(JNIEnv *env,
                                                                    jclass,
                                                                    jint input_fd,
                                                                    jstring output_path,
                                                                    jint sample_rate,
                                                                    jint bit_rate,
                                                                    jint packet_size,
                                                                    jboolean tencent) {
    std::string output_path_str = jstring2string(env, output_path);
    if (output_path_str.empty()) {
        throwIOExceptionF(env, "output_path is empty");
        return;
    }
    int output_fd = open(output_path_str.c_str(), O_WRONLY | O_CREAT | O_TRUNC, 0644);
    if (output_fd < 0) {
        throwIOExceptionF(env, "open(output_path_str) failed: %s", strerror(errno));
        return;
    }
    convertPcm16leToSilk(env, input_fd, output_fd, sample_rate, bit_rate, packet_size, tencent);
}

extern "C"
JNIEXPORT void JNICALL
Java_io_github_qauxv_util_ptt_SilkEncodeUtils_nativePcm16leToSilkSS(JNIEnv *env,
                                                                    jclass,
                                                                    jstring input_path,
                                                                    jstring output_path,
                                                                    jint sample_rate,
                                                                    jint bit_rate,
                                                                    jint packet_size,
                                                                    jboolean tencent) {
    std::string input_path_str = jstring2string(env, input_path);
    std::string output_path_str = jstring2string(env, output_path);
    if (input_path_str.empty()) {
        throwIOExceptionF(env, "input_path is empty");
        return;
    }
    if (output_path_str.empty()) {
        throwIOExceptionF(env, "output_path is empty");
        return;
    }
    int input_fd = open(input_path_str.c_str(), O_RDONLY);
    if (input_fd < 0) {
        throwIOExceptionF(env, "open(input_path_str) failed: %s", strerror(errno));
        return;
    }
    int output_fd = open(output_path_str.c_str(), O_WRONLY | O_CREAT | O_TRUNC, 0644);
    if (output_fd < 0) {
        close(input_fd);
        throwIOExceptionF(env, "open(output_path_str) failed: %s", strerror(errno));
        return;
    }
    convertPcm16leToSilk(env, input_fd, output_fd, sample_rate, bit_rate, packet_size, tencent);
}

extern "C"
JNIEXPORT void JNICALL
Java_io_github_qauxv_util_ptt_SilkEncodeUtils_nativePcm16leToSilkSI(JNIEnv *env,
                                                                    jclass clazz,
                                                                    jstring input_path,
                                                                    jint output_fd,
                                                                    jint sample_rate,
                                                                    jint bit_rate,
                                                                    jint packet_size,
                                                                    jboolean tencent) {
    std::string input_path_str = jstring2string(env, input_path);
    if (input_path_str.empty()) {
        throwIOExceptionF(env, "input_path is empty");
        close(output_fd);
        return;
    }
    int input_fd = open(input_path_str.c_str(), O_RDONLY);
    if (input_fd < 0) {
        throwIOExceptionF(env, "open(input_path_str) failed: %s", strerror(errno));
        close(output_fd);
        return;
    }
    convertPcm16leToSilk(env, input_fd, output_fd, sample_rate, bit_rate, packet_size, tencent);
}

//@formatter:off
static JNINativeMethod gMethods[] = {
        {"nativePcm16leToSilkII", "(IIIIIZ)V", reinterpret_cast<void*>(Java_io_github_qauxv_util_ptt_SilkEncodeUtils_nativePcm16leToSilkII)},
        {"nativePcm16leToSilkIS", "(ILjava/lang/String;IIIZ)V", reinterpret_cast<void*>(Java_io_github_qauxv_util_ptt_SilkEncodeUtils_nativePcm16leToSilkIS)},
        {"nativePcm16leToSilkSI", "(Ljava/lang/String;IIIIZ)V", reinterpret_cast<void*>(Java_io_github_qauxv_util_ptt_SilkEncodeUtils_nativePcm16leToSilkSI)},
        {"nativePcm16leToSilkSS", "(Ljava/lang/String;Ljava/lang/String;IIIZ)V", reinterpret_cast<void*>(Java_io_github_qauxv_util_ptt_SilkEncodeUtils_nativePcm16leToSilkSS)},
};
//@formatter:on
REGISTER_PRIMARY_PRE_INIT_NATIVE_METHODS("io/github/qauxv/util/ptt/SilkEncodeUtils", gMethods);
