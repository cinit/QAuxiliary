//
// Created by teble on 2022/4/9.
//

#ifndef MD5_H
#define MD5_H

#include <bitset>
#include <iostream>
#include <vector>

/* Parameters of MD5. */
#define S11 7
#define S12 12
#define S13 17
#define S14 22
#define S21 5
#define S22 9
#define S23 14
#define S24 20
#define S31 4
#define S32 11
#define S33 16
#define S34 23
#define S41 6
#define S42 10
#define S43 15
#define S44 21

#define F(x, y, z) (((x) & (y)) | ((~(x)) & (z)))
#define G(x, y, z) (((x) & (z)) | ((y) & (~(z))))
#define H(x, y, z) ((x) ^ (y) ^ (z))
#define I(x, y, z) ((y) ^ ((x) | (~(z))))

#define ROTATELEFT(num, n) (((num) << (n)) | ((num) >> (32 - (n))))

/**
 * @Transformations for rounds 1, 2, 3, and 4.
 */
#define FF(a, b, c, d, x, s, ac)                \
    {                                           \
        (a) += F((b), (c), (d)) + (x) + (ac);   \
        (a) = ROTATELEFT((a), (s));             \
        (a) += (b);                             \
    }
#define GG(a, b, c, d, x, s, ac)                \
    {                                           \
        (a) += G((b), (c), (d)) + (x) + (ac);   \
        (a) = ROTATELEFT((a), (s));             \
        (a) += (b);                             \
    }
#define HH(a, b, c, d, x, s, ac)                \
    {                                           \
        (a) += H((b), (c), (d)) + (x) + (ac);   \
        (a) = ROTATELEFT((a), (s));             \
        (a) += (b);                             \
    }
#define II(a, b, c, d, x, s, ac)                \
    {                                           \
        (a) += I((b), (c), (d)) + (x) + (ac);   \
        (a) = ROTATELEFT((a), (s));             \
        (a) += (b);                             \
    }

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
