//
// Created by sulfate on 2024-08-17.
//

#ifndef NATIVES_BYTE_ARRAY_OUTPUT_STREAM_H
#define NATIVES_BYTE_ARRAY_OUTPUT_STREAM_H

#include <cstdint>
#include <vector>
#include <span>

namespace util {

// this utility class is used to write data to a byte array
// it is NOT thread-safe
class ByteArrayOutputStream {
public:
    ByteArrayOutputStream() = default;
    ~ByteArrayOutputStream() = default;
    // avoid accidental copying
    ByteArrayOutputStream(const ByteArrayOutputStream&) = delete;
    ByteArrayOutputStream& operator=(const ByteArrayOutputStream&) = delete;
private:
    static constexpr auto kBlockSize = 4096;
public:
    void Write(const uint8_t* data, int64_t size);

    inline void Write(std::span<const uint8_t> data) {
        Write(data.data(), data.size());
    }

    void GetCurrentBuffer(uint8_t** buffer, int64_t* currentOffset) noexcept;

    void Skip(int64_t size);

    [[nodiscard]] std::vector<uint8_t> GetBytes() const;

    [[nodiscard]] int64_t GetSize() const noexcept;

    [[nodiscard]] constexpr int64_t GetBlockSize() const noexcept {
        return kBlockSize;
    }

    void Reset() noexcept;

private:
    int64_t mOffsetInCurrentBuffer = 0;
    std::vector<std::array<uint8_t, kBlockSize>> mBuffers;
};

}

#endif //NATIVES_BYTE_ARRAY_OUTPUT_STREAM_H
