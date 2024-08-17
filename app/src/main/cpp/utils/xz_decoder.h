//
// Created by sulfate on 2024-08-17.
//

#ifndef NATIVES_XZ_DECODER_H
#define NATIVES_XZ_DECODER_H

#include <vector>
#include <cstdint>
#include <span>
#include <string>

namespace util {

std::vector<uint8_t> DecodeXzData(std::span<const uint8_t> inputData, bool* isSuccess, std::string* errorMsg);

}

#endif //NATIVES_XZ_DECODER_H
