#pragma once

#include <map>
#include <vector>
#include <string_view>
#include <utility>
#include <unistd.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <zlib.h>

#include "zip_helper.h"

namespace zip_helper {

#define UNZIP_BUF_CHUNK 512

constexpr uint32_t kZipLocalFileSignature = 0x04034b50u;
constexpr uint32_t kZipDataDescSignature = 0x08074b50u;

struct MemMap {
    MemMap() = default;

    explicit MemMap(std::string_view file_name) {
        int fd = open(file_name.data(), O_RDONLY | O_CLOEXEC);
        if (fd > 0) {
            struct stat s{};
            fstat(fd, &s);
            auto *addr = mmap(nullptr, s.st_size, PROT_READ, MAP_PRIVATE, fd, 0);
            if (addr != MAP_FAILED) {
                addr_ = static_cast<uint8_t *>(addr);
                len_ = s.st_size;
            }
        }
        close(fd);
    }

    explicit MemMap(uint32_t size) {
        auto *addr = mmap(nullptr, size, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
        if (addr != MAP_FAILED) {
            addr_ = static_cast<uint8_t *>(addr);
            len_ = size;
        }
    }

    explicit MemMap(uint8_t *addr, uint32_t len) {
        auto *map = mmap(addr, len, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
        if (map != MAP_FAILED) {
            addr_ = static_cast<uint8_t *>(map);
            len_ = len;
        }
        memcpy(addr_, addr, len);
        mprotect(addr_, len_, PROT_READ);
    }

    ~MemMap() {
        if (ok()) {
            munmap(addr_, len_);
        }
    }

    [[nodiscard]] bool ok() const { return addr_ && len_; }

    [[nodiscard]] auto addr() const { return addr_; }

    [[nodiscard]] auto len() const { return len_; }

    MemMap(MemMap &&other) noexcept: addr_(other.addr_), len_(other.len_) {
        other.addr_ = nullptr;
        other.len_ = 0;
    }

    MemMap &operator=(MemMap &&other) noexcept {
        new(this) MemMap(std::move(other));
        return *this;
    }

    MemMap(const MemMap &) = delete;

    MemMap &operator=(const MemMap &) = delete;

private:
    uint8_t *addr_ = nullptr;
    uint32_t len_ = 0;
};

static void *myalloc([[maybe_unused]] void *q, unsigned n, unsigned m) {
    return calloc(n, m);
}

static void myfree([[maybe_unused]] void *q, void *p) {
    (void) q;
    free(p);
}

struct [[gnu::packed]] ZipFileRecord {
    [[maybe_unused]] uint32_t signature;
    [[maybe_unused]] uint16_t version;
    [[maybe_unused]] uint16_t flags;
    [[maybe_unused]] uint16_t compress;
    [[maybe_unused]] uint16_t last_modify_time;
    [[maybe_unused]] uint16_t last_modify_date;
    [[maybe_unused]] uint32_t crc;
    [[maybe_unused]] uint32_t compress_size;
    [[maybe_unused]] uint32_t uncompress_size;
    [[maybe_unused]] uint16_t file_name_length;
    [[maybe_unused]] uint16_t extra_length;
//    [[maybe_unused]] uint8_t file_name[0];

    // fuck apk (compress_size | uncompress_size) == 0
    std::pair<uint32_t, uint32_t> getRealSizeInfo() {
        if (compress_size && uncompress_size) {
            uint32_t tmp_compress_size = compress_size;
            uint32_t tmp_uncompress_size = uncompress_size;
            return {tmp_compress_size, tmp_uncompress_size};
        }
        z_stream stream{};
        auto ret = inflateInit2(&stream, -MAX_WBITS);
        if (ret != Z_OK) {
            return {0, 0};
        }

        char buf[UNZIP_BUF_CHUNK];
        uint32_t total_read = 0;
        uint32_t total_write = 0;
        uint32_t input_pos = 0;

        stream.zalloc = myalloc;
        stream.zfree = myfree;
        stream.opaque = nullptr;
        stream.next_in = this->data();
        stream.avail_in = UNZIP_BUF_CHUNK;

        do {
            if (input_pos == UNZIP_BUF_CHUNK) {
                stream.next_in = this->data() + total_read;
                stream.avail_in = UNZIP_BUF_CHUNK;
                input_pos = 0;
            }
            stream.next_out = (uint8_t *) buf;
            stream.avail_out = UNZIP_BUF_CHUNK;
            ret = inflate(&stream, Z_PARTIAL_FLUSH);
            switch (ret) {
                case Z_OK: {
                    uint32_t input_used = (UNZIP_BUF_CHUNK - input_pos) - stream.avail_in;
                    total_write += UNZIP_BUF_CHUNK - stream.avail_out;
                    input_pos += input_used;
                    total_read += input_used;
                    break;
                }
                case Z_BUF_ERROR:
                    return {0, 0};
                case Z_DATA_ERROR:
                case Z_MEM_ERROR: {
                    inflateEnd(&stream);
                    return {0, 0};
                }
                default:
                    break;
            }
        } while (ret != Z_STREAM_END);

        inflateEnd(&stream);
        total_read += (UNZIP_BUF_CHUNK - input_pos) - stream.avail_in;
        total_write += UNZIP_BUF_CHUNK - stream.avail_out;

        return {total_read, total_write};
    }

    std::string_view file_name() {
        return {reinterpret_cast<char *>(reinterpret_cast<uint8_t *>(this) + sizeof(ZipFileRecord)), file_name_length};
    }

    uint8_t *data() {
        return reinterpret_cast<uint8_t *>(this) + sizeof(ZipFileRecord) + file_name_length + extra_length;
    }
};

struct [[gnu::packed]] ZipLocalFile {

    explicit ZipLocalFile(ZipFileRecord *record) {
        this->record = record;
        // check compress_size and uncompress_size
        if (record->compress_size == 0 || record->uncompress_size == 0) {
            auto info = record->getRealSizeInfo();
            this->real_compress_size = info.first;
            this->real_uncompress_size = info.second;
        } else {
            this->real_compress_size = record->compress_size;
            this->real_uncompress_size = record->uncompress_size;
        }
    }

    static ZipLocalFile *from(uint8_t *begin) {
        auto *pRecord = reinterpret_cast<ZipFileRecord *>(begin);
        if (pRecord->signature == kZipLocalFileSignature) {
            return new ZipLocalFile(pRecord);
        } else {
            return nullptr;
        }
    }

    [[nodiscard]] uint32_t getDataDescriptorSize() const {
        if (record->flags & 0x8u) {
            auto nextPtr = reinterpret_cast<uint8_t *>(record) + sizeof(ZipFileRecord) +
                    record->file_name_length + record->extra_length + real_compress_size;
            auto descSign = reinterpret_cast<uint32_t *>(nextPtr);
            if (*descSign == kZipDataDescSignature) {
                return 16;
            } else {
                return 12;
            }
        }
        return 0;
    }

    [[nodiscard]] inline uint32_t getEntrySize() const {
        return sizeof(ZipFileRecord) + record->file_name_length + record->extra_length + real_compress_size +
                getDataDescriptorSize();
    }

    [[nodiscard]] ZipLocalFile *next() const {
        return from(reinterpret_cast<uint8_t *>(record) + getEntrySize());
    }

    [[nodiscard]] MemMap uncompress() const {
        if (record->compress == 0x8u) {
            MemMap out(real_uncompress_size);
            if (!out.ok()) {
                return {};
            }
            z_stream d_stream;
            d_stream.zalloc = myalloc;
            d_stream.zfree = myfree;
            d_stream.opaque = nullptr;

            d_stream.next_in = data();
            d_stream.avail_in = real_compress_size;
            d_stream.next_out = out.addr();
            d_stream.avail_out = out.len();

            auto ret = inflateInit2(&d_stream, -MAX_WBITS);
            if (ret != Z_OK) {
                return {};
            }

            do {
                ret = inflate(&d_stream, Z_NO_FLUSH);
            } while (ret != Z_STREAM_END && ret == Z_OK);

            inflateEnd(&d_stream);

            if (d_stream.total_out != real_uncompress_size) {
                return {};
            }

            mprotect(out.addr(), out.len(), PROT_READ);
            return out;
        } else if (record->compress == 0 && real_compress_size == real_uncompress_size) {
            MemMap out(data(), real_uncompress_size);
            return out;
        }
        return {};
    }

    [[nodiscard]] std::string_view file_name() const {
        return record->file_name();
    }

    [[nodiscard]] uint8_t *data() const {
        return record->data();
    }

    ZipFileRecord *record;
    uint32_t real_compress_size;
    uint32_t real_uncompress_size;
};

class ZipFile {
public:
    static std::unique_ptr<ZipFile> Open(const MemMap &map) {
        static ZipLocalFile *local_file;
        local_file = ZipLocalFile::from(map.addr());
        if (!local_file) return nullptr;
        auto r = std::make_unique<ZipFile>();
        while (local_file) {
            r->entries_map.emplace(local_file->file_name(), local_file);
            r->entries.emplace_back(local_file);
            local_file = local_file->next();
        }
        return r;
    }

    ZipLocalFile *Find(std::string_view entry_name) {
        if (auto i = entries_map.find(entry_name); i != entries_map.end()) {
            return i->second;
        }
        return nullptr;
    }

    std::vector<ZipLocalFile *> entries;

private:
    std::map<std::string_view, ZipLocalFile *> entries_map;
};

#undef UNZIP_BUF_CHUNK

}
