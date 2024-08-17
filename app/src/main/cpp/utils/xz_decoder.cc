//
// Created by sulfate on 2024-08-17.
//

#include "xz_decoder.h"

#include "Precomp.h"

#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <utility>
#include <mutex>

#include <fmt/format.h>

#include "CpuArch.h"

#include "Alloc.h"
#include "7zFile.h"
#include "7zVersion.h"
#include "Xz.h"
#include "7zCrc.h"
#include "XzCrc64.h"

#include "byte_array_output_stream.h"


std::vector<uint8_t> util::DecodeXzData(std::span<const uint8_t> inputData, bool* isSuccess, std::string* errorMsg) {
    constexpr auto fnSetError = [](std::string* errorMsg, std::string_view msg) {
        if (errorMsg) {
            *errorMsg = msg;
        }
    };
    if (inputData.empty()) {
        fnSetError(errorMsg, "input data is empty");
        if (isSuccess) {
            *isSuccess = false;
        }
        return {};
    }
    ISzAlloc alloc;
    alloc.Alloc = +[](ISzAllocPtr, size_t size) { return malloc(size); };
    alloc.Free = +[](ISzAllocPtr, void* ptr) { free(ptr); };
    CXzUnpacker state = {};
    XzUnpacker_Construct(&state, &alloc);
    static std::once_flag crc_initialized;
    std::call_once(crc_initialized, []() {
        CrcGenerateTable();
        Crc64GenerateTable();
    });
    size_t src_offset = 0;
    size_t dst_offset = 0;
    util::ByteArrayOutputStream dst;
    ECoderStatus status = CODER_STATUS_NOT_FINISHED;
    while (status == CODER_STATUS_NOT_FINISHED) {
        SizeT src_io = inputData.size() - src_offset;
        SizeT dst_io;
        Byte* dst_plus_dst_offset;
        {
            uint8_t* buf = nullptr;
            int64_t dst_buf_off = 0;
            dst.GetCurrentBuffer(&buf, &dst_buf_off);
            dst_io = dst.GetBlockSize() - dst_buf_off;
            dst_plus_dst_offset = buf + dst_buf_off;
        }
        int res = XzUnpacker_Code(&state, dst_plus_dst_offset, &dst_io,
                                  (const Byte*) (inputData.data() + src_offset), &src_io,
                                  true, CODER_FINISH_ANY, &status);
        if (res != SZ_OK) {
            fnSetError(errorMsg, fmt::format("LZMA decompression failed with error {}", res));
            if (isSuccess) {
                *isSuccess = false;
            }
            XzUnpacker_Free(&state);
            return {};
        }
        src_offset += src_io;
        dst_offset += dst_io;
        auto dst_written = dst_io;
        if (dst_written > 0) {
            dst.Skip((int64_t) dst_written);
        }
    }
    XzUnpacker_Free(&state);
    if (!XzUnpacker_IsStreamWasFinished(&state)) {
        fnSetError(errorMsg, "LZMA decompresstion failed due to incomplete stream");
        if (isSuccess) {
            *isSuccess = false;
        }
        return {};
    }
    if (isSuccess) {
        *isSuccess = true;
    }
    auto ret = dst.GetBytes();
    fnSetError(errorMsg, "");
    return ret;
}
