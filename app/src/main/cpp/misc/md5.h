//
// Created by teble on 2022/4/9.
//

#ifndef MD5_H
#define MD5_H

#include <bitset>
#include <string>
#include <vector>

typedef unsigned int bit32;

class MD5 {
public:
    explicit MD5(std::string str);
    MD5();

    void init();

    std::string getDigest();

    void padding();
    void sort_little_endian();
    void appendLength();
    void transform(int beginIndex);
    void decode(int beginIndex, bit32* x);
    static bit32 convertToBit32(const std::vector<bool>& a);

    std::string to_str() const;

private:
    std::string input_msg;
    std::vector<bool> bin_msg;

    // b is the length of the original msg
    int b{};
    std::vector<bool> bin_b;

    bit32 A{}, B{}, C{}, D{};
};


#endif //MD5_H
