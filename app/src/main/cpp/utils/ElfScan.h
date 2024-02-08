//
// Created by sulfate on 2023-06-24.
//

#ifndef QAUXV_ELFSCAN_H
#define QAUXV_ELFSCAN_H

#include <cstdint>
#include <string>
#include <vector>
#include <optional>
#include <span>

namespace utils {

std::vector<uint64_t> FindByteSequenceForImageFile(const void* baseAddress, std::span<const uint8_t> sequence,
                                                   std::span<const uint8_t> mask, bool execMemOnly, int step,
                                                   std::optional<uint64_t> hint);

std::vector<uint64_t> FindByteSequenceForLoadedImage(const void* baseAddress, std::span<const uint8_t> sequence,
                                                     std::span<const uint8_t> mask, bool execMemOnly, int step,
                                                     std::optional<uint64_t> hint);

std::vector<uint64_t> FindByteSequenceImpl(const void* baseAddress, bool isLoaded, std::span<const uint8_t> sequence,
                                           std::span<const uint8_t> mask, bool execMemOnly, int step,
                                           std::optional<uint64_t> hint);

}

#endif //QAUXV_ELFSCAN_H
