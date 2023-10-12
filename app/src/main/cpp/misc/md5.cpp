//
// Created by teble on 2022/4/9.
//

#include "md5.h"

#include <utility>

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

MD5::MD5() = default;

MD5::MD5(std::string  str) : input_msg(std::move(str)) {}

void MD5::init() {
    // init the binary message
    bin_msg.clear();
    for (char i : input_msg) {
        auto temp = std::bitset<8>(i);
        for (int j = 7; j >= 0; --j) {
            bin_msg.push_back(temp[j]);
        }
    }

    // calculate the binary b
    bin_b.clear();
    b = (int) bin_msg.size();
    std::bitset<64> tempb(bin_msg.size());
    for (int i = 63; i >= 0; --i) {
        bin_b.push_back(tempb[i]);
    }
}

std::string MD5::getDigest() {
    init();
    // Step 1. Append Padding Bits
    padding();
    // Step 2. Append Length
    appendLength();
    // Step 3. Initialize MD Buffer
    A = 0x67452301;
    B = 0xefcdab89;
    C = 0x98badcfe;
    D = 0x10325476;
    // Step 4. Process Message in 16-Word Blocks
    int L = (int) bin_msg.size() / 512;
    for (int i = 0; i < L; ++i) {
        transform(i);
    }

    // Step 5. Output
    return to_str();
}

void MD5::padding() {
    int diff;
    if (b % 512 < 448) {
        diff = 448 - b % 512;
    } else {
        diff = 960 - b % 512;
    }

    bin_msg.push_back(true);
    std::vector<bool> pad(diff - 1, false);
    bin_msg.insert(bin_msg.end(), pad.begin(), pad.end());
    sort_little_endian();
}

void MD5::sort_little_endian() {
    // in each word, all 4 bytes should be sorted as little-endian
    // That means:
    // in the original msg: 00000001 00000010 00000011 00000100
    // after the sort:      00000100 00000011 00000010 00000001
    int wordnums = (int) bin_msg.size() / 32;

    std::vector<bool> ret;

    for (int i = 0; i < wordnums; ++i) {
        std::vector<bool> word(bin_msg.begin() + i * 32,
                          bin_msg.begin() + (i + 1) * 32);
        ret.insert(ret.end(), word.begin() + 24, word.end());
        ret.insert(ret.end(), word.begin() + 16, word.begin() + 24);
        ret.insert(ret.end(), word.begin() + 8, word.begin() + 16);
        ret.insert(ret.end(), word.begin(), word.begin() + 8);
    }

    bin_msg.clear();
    bin_msg.insert(bin_msg.end(), ret.begin(), ret.end());
}

void MD5::appendLength() {
    bin_msg.insert(bin_msg.end(), bin_b.begin() + 32, bin_b.end());
    bin_msg.insert(bin_msg.end(), bin_b.begin(), bin_b.begin() + 32);
};

void MD5::decode(int beginIndex, bit32* x) {
    // prepare each 512 bits part in the x[16], and use the x[] for the
    // transform
    std::vector<bool>::const_iterator st = bin_msg.begin() + beginIndex;
    std::vector<bool>::const_iterator ed = bin_msg.begin() + beginIndex + 512;
    std::vector<bool> cv(st, ed);

    for (int i = 0; i < 16; ++i) {
        std::vector<bool>::const_iterator first = cv.begin() + 32 * i;
        std::vector<bool>::const_iterator last = cv.begin() + 32 * (i + 1);
        std::vector<bool> newvec(first, last);

        x[i] = convertToBit32(newvec);
    }
}

bit32 MD5::convertToBit32(const std::vector<bool>& a) {
    // convert a 32 bits long vector<bool> to bit32(int) type
    int partlen = 32;
    bit32 res = 0;
    for (int i = 0; i < partlen; ++i) {
        res = res | (a[i] << (partlen - i - 1));
    }
    return res;
}

void MD5::transform(int beginIndex) {
    bit32 AA = A, BB = B, CC = C, DD = D;

    bit32 x[16];
    decode(512 * beginIndex, x);

    /* Round 1 */
    FF(A, B, C, D, x[0], S11, 0xd76aa478);
    FF(D, A, B, C, x[1], S12, 0xe8c7b756);
    FF(C, D, A, B, x[2], S13, 0x242070db);
    FF(B, C, D, A, x[3], S14, 0xc1bdceee);
    FF(A, B, C, D, x[4], S11, 0xf57c0faf);
    FF(D, A, B, C, x[5], S12, 0x4787c62a);
    FF(C, D, A, B, x[6], S13, 0xa8304613);
    FF(B, C, D, A, x[7], S14, 0xfd469501);
    FF(A, B, C, D, x[8], S11, 0x698098d8);
    FF(D, A, B, C, x[9], S12, 0x8b44f7af);
    FF(C, D, A, B, x[10], S13, 0xffff5bb1);
    FF(B, C, D, A, x[11], S14, 0x895cd7be);
    FF(A, B, C, D, x[12], S11, 0x6b901122);
    FF(D, A, B, C, x[13], S12, 0xfd987193);
    FF(C, D, A, B, x[14], S13, 0xa679438e);
    FF(B, C, D, A, x[15], S14, 0x49b40821);

    /* Round 2 */
    GG(A, B, C, D, x[1], S21, 0xf61e2562);
    GG(D, A, B, C, x[6], S22, 0xc040b340);
    GG(C, D, A, B, x[11], S23, 0x265e5a51);
    GG(B, C, D, A, x[0], S24, 0xe9b6c7aa);
    GG(A, B, C, D, x[5], S21, 0xd62f105d);
    GG(D, A, B, C, x[10], S22, 0x2441453);
    GG(C, D, A, B, x[15], S23, 0xd8a1e681);
    GG(B, C, D, A, x[4], S24, 0xe7d3fbc8);
    GG(A, B, C, D, x[9], S21, 0x21e1cde6);
    GG(D, A, B, C, x[14], S22, 0xc33707d6);
    GG(C, D, A, B, x[3], S23, 0xf4d50d87);
    GG(B, C, D, A, x[8], S24, 0x455a14ed);
    GG(A, B, C, D, x[13], S21, 0xa9e3e905);
    GG(D, A, B, C, x[2], S22, 0xfcefa3f8);
    GG(C, D, A, B, x[7], S23, 0x676f02d9);
    GG(B, C, D, A, x[12], S24, 0x8d2a4c8a);

    /* Round 3 */
    HH(A, B, C, D, x[5], S31, 0xfffa3942);
    HH(D, A, B, C, x[8], S32, 0x8771f681);
    HH(C, D, A, B, x[11], S33, 0x6d9d6122);
    HH(B, C, D, A, x[14], S34, 0xfde5380c);
    HH(A, B, C, D, x[1], S31, 0xa4beea44);
    HH(D, A, B, C, x[4], S32, 0x4bdecfa9);
    HH(C, D, A, B, x[7], S33, 0xf6bb4b60);
    HH(B, C, D, A, x[10], S34, 0xbebfbc70);
    HH(A, B, C, D, x[13], S31, 0x289b7ec6);
    HH(D, A, B, C, x[0], S32, 0xeaa127fa);
    HH(C, D, A, B, x[3], S33, 0xd4ef3085);
    HH(B, C, D, A, x[6], S34, 0x4881d05);
    HH(A, B, C, D, x[9], S31, 0xd9d4d039);
    HH(D, A, B, C, x[12], S32, 0xe6db99e5);
    HH(C, D, A, B, x[15], S33, 0x1fa27cf8);
    HH(B, C, D, A, x[2], S34, 0xc4ac5665);

    /* Round 4 */
    II(A, B, C, D, x[0], S41, 0xf4292244);
    II(D, A, B, C, x[7], S42, 0x432aff97);
    II(C, D, A, B, x[14], S43, 0xab9423a7);
    II(B, C, D, A, x[5], S44, 0xfc93a039);
    II(A, B, C, D, x[12], S41, 0x655b59c3);
    II(D, A, B, C, x[3], S42, 0x8f0ccc92);
    II(C, D, A, B, x[10], S43, 0xffeff47d);
    II(B, C, D, A, x[1], S44, 0x85845dd1);
    II(A, B, C, D, x[8], S41, 0x6fa87e4f);
    II(D, A, B, C, x[15], S42, 0xfe2ce6e0);
    II(C, D, A, B, x[6], S43, 0xa3014314);
    II(B, C, D, A, x[13], S44, 0x4e0811a1);
    II(A, B, C, D, x[4], S41, 0xf7537e82);
    II(D, A, B, C, x[11], S42, 0xbd3af235);
    II(C, D, A, B, x[2], S43, 0x2ad7d2bb);
    II(B, C, D, A, x[9], S44, 0xeb86d391);

    A = A + AA;
    B = B + BB;
    C = C + CC;
    D = D + DD;
}

std::string MD5::to_str() const {
    // Output in Big-Endian
    // For a value 0x6789abcd
    // Output as    "cdab8967"
    bit32 input[4];
    input[0] = A;
    input[1] = B;
    input[2] = C;
    input[3] = D;

    std::string ret;
    char buffer[4];

    for (unsigned int i : input) {
        for (int j = 0; j < 4; ++j) {
            snprintf(buffer, 4, "%02X", i >> j * 8 & 0xff);
            ret += buffer;
        }
    }
    return ret;
}
