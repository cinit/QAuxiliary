#include <cstdint>
#include <cstddef>
#include <type_traits>

namespace platform::arch::endian {

#if __BYTE_ORDER == __LITTLE_ENDIAN
static constexpr bool kIsLittleEndian = true;
#elif __BYTE_ORDER == __BIG_ENDIAN
static constexpr bool kIsLittleEndian = false;
#else
#error "Unknown endianness"
#endif

static constexpr bool kIsBigEndian = !kIsLittleEndian;

template<typename T>
requires(std::is_integral_v<T> && std::is_unsigned_v<T> && sizeof(T) == 2)
static constexpr T SwapEndian16(T value) {
    return static_cast<T>((value >> 8) | (value << 8));
}

template<typename T>
requires(std::is_integral_v<T> && std::is_unsigned_v<T> && sizeof(T) == 4)
static constexpr T SwapEndian32(T value) {
    return static_cast<T>((value >> 24) | ((value & 0x00ff0000u) >> 8) |
            ((value & 0x0000ff00u) << 8) | (value << 24));
}

template<typename T>
requires(std::is_integral_v<T> && std::is_unsigned_v<T> && sizeof(T) == 8)
static constexpr T SwapEndian64(T value) {
    return static_cast<T>((value >> 56) | ((value & 0x00ff000000000000ULL) >> 40) |
            ((value & 0x0000ff0000000000ULL) >> 24) |
            ((value & 0x000000ff00000000ULL) >> 8) |
            ((value & 0x00000000ff000000ULL) << 8) |
            ((value & 0x0000000000ff0000ULL) << 24) |
            ((value & 0x000000000000ff00ULL) << 40) | (value << 56));
}

static constexpr uint16_t ltoh16(uint16_t value) {
    return kIsLittleEndian ? value : SwapEndian16(value);
}

static constexpr uint32_t ltoh32(uint32_t value) {
    return kIsLittleEndian ? value : SwapEndian32(value);
}

static constexpr uint64_t ltoh64(uint64_t value) {
    return kIsLittleEndian ? value : SwapEndian64(value);
}

static constexpr uint16_t htol16(uint16_t value) {
    return kIsLittleEndian ? value : SwapEndian16(value);
}

static constexpr uint32_t htol32(uint32_t value) {
    return kIsLittleEndian ? value : SwapEndian32(value);
}

static constexpr uint64_t htol64(uint64_t value) {
    return kIsLittleEndian ? value : SwapEndian64(value);
}

static constexpr uint16_t btoh16(uint16_t value) {
    return kIsBigEndian ? value : SwapEndian16(value);
}

static constexpr uint32_t btoh32(uint32_t value) {
    return kIsBigEndian ? value : SwapEndian32(value);
}

static constexpr uint64_t btoh64(uint64_t value) {
    return kIsBigEndian ? value : SwapEndian64(value);
}

static constexpr uint16_t htob16(uint16_t value) {
    return kIsBigEndian ? value : SwapEndian16(value);
}

static constexpr uint32_t htob32(uint32_t value) {
    return kIsBigEndian ? value : SwapEndian32(value);
}

static constexpr uint64_t htob64(uint64_t value) {
    return kIsBigEndian ? value : SwapEndian64(value);
}

static constexpr uint16_t ntoh16(uint16_t value) {
    return btoh16(value);
}

static constexpr uint32_t ntoh32(uint32_t value) {
    return btoh32(value);
}

static constexpr uint64_t ntoh64(uint64_t value) {
    return btoh64(value);
}

static constexpr uint16_t hton16(uint16_t value) {
    return htob16(value);
}

static constexpr uint32_t hton32(uint32_t value) {
    return htob32(value);
}

static constexpr uint64_t hton64(uint64_t value) {
    return htob64(value);
}

}
