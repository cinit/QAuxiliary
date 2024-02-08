//
// Created by sulfate on 2024-02-08.
//

#ifndef QAUXV_STRING_OPERATORS_H
#define QAUXV_STRING_OPERATORS_H

#include <string_view>
#include <string>

static inline std::string operator+(std::string_view lhs, std::string_view rhs) {
    std::string result;
    result.reserve(lhs.size() + rhs.size());
    result.append(lhs);
    result.append(rhs);
    return result;
}

static inline std::string operator+(std::string_view lhs, const char* rhs) {
    if (rhs == nullptr) [[unlikely]] {
        return std::string(lhs);
    }
    std::string result;
    result.reserve(lhs.size() + std::strlen(rhs));
    result.append(lhs);
    result.append(rhs);
    return result;
}

static inline std::string operator+(const char* lhs, std::string_view rhs) {
    if (lhs == nullptr) [[unlikely]] {
        return std::string(rhs);
    }
    std::string result;
    result.reserve(std::strlen(lhs) + rhs.size());
    result.append(lhs);
    result.append(rhs);
    return result;
}

#endif //QAUXV_STRING_OPERATORS_H
