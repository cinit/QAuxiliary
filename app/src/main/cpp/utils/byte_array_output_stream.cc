//
// Created by sulfate on 2024-08-17.
//

#include "byte_array_output_stream.h"

#include <cstring>

void util::ByteArrayOutputStream::Reset() noexcept {
    mBuffers.clear();
    mOffsetInCurrentBuffer = 0;
}

std::vector<uint8_t> util::ByteArrayOutputStream::GetBytes() const {
    auto bufferCount = mBuffers.size();
    int64_t lastBufferSize = mOffsetInCurrentBuffer;
    if (bufferCount == 0) {
        return {};
    }
    if (bufferCount == 1) {
        std::vector<uint8_t> result;
        result.resize(lastBufferSize);
        const auto& currentBuffer = mBuffers[0];
        memcpy(result.data(), currentBuffer.data(), lastBufferSize);
        return result;
    } else {
        // calculate the total size
        int64_t totalSize = (bufferCount - 1) * kBlockSize + lastBufferSize;
        std::vector<uint8_t> result;
        result.resize(totalSize);
        // copy buffers
        for (int i = 0; i < bufferCount - 1; i++) {
            const auto& currentBuffer = mBuffers[i];
            memcpy(result.data() + i * kBlockSize, currentBuffer.data(), kBlockSize);
        }
        // the last buffer
        {
            const auto& currentBuffer = mBuffers[bufferCount - 1];
            memcpy(result.data() + (bufferCount - 1) * kBlockSize, currentBuffer.data(), lastBufferSize);
        }
        return result;
    }
}

int64_t util::ByteArrayOutputStream::GetSize() const noexcept {
    if (mBuffers.empty()) {
        return 0;
    } else if (mBuffers.size() == 1) {
        return mOffsetInCurrentBuffer;
    } else {
        return (mBuffers.size() - 1) * kBlockSize + mOffsetInCurrentBuffer;
    }
}

void util::ByteArrayOutputStream::Write(const uint8_t* data, int64_t size) {
    if (size <= 0) {
        return;
    }
    auto remainingSize = size;
    auto ptr = data;
    while (remainingSize > 0) {
        // need a new buffer?
        if (mBuffers.empty() || mOffsetInCurrentBuffer == kBlockSize) {
            mBuffers.emplace_back();
            mOffsetInCurrentBuffer = 0;
        }
        auto& currentBuffer = mBuffers.back();
        auto toWrite = std::min(kBlockSize - mOffsetInCurrentBuffer, remainingSize);
        memcpy(currentBuffer.data() + mOffsetInCurrentBuffer, ptr, toWrite);
        mOffsetInCurrentBuffer += toWrite;
        ptr += toWrite;
        remainingSize -= toWrite;
    }
}

void util::ByteArrayOutputStream::GetCurrentBuffer(uint8_t** buffer, int64_t* currentOffset) noexcept {
    // need a new buffer?
    if (mBuffers.empty() || mOffsetInCurrentBuffer == kBlockSize) {
        mBuffers.emplace_back();
        mOffsetInCurrentBuffer = 0;
    }
    *buffer = mBuffers.back().data();
    *currentOffset = mOffsetInCurrentBuffer;
}

void util::ByteArrayOutputStream::Skip(int64_t size) {
    if (size <= 0) {
        return;
    }
    auto remainingSize = size;
    while (remainingSize > 0) {
        // need a new buffer?
        if (mBuffers.empty() || mOffsetInCurrentBuffer == kBlockSize) {
            mBuffers.emplace_back();
            mOffsetInCurrentBuffer = 0;
        }
        auto& currentBuffer = mBuffers.back();
        auto toSkip = std::min(kBlockSize - mOffsetInCurrentBuffer, remainingSize);
        mOffsetInCurrentBuffer += toSkip;
        remainingSize -= toSkip;
    }
}
