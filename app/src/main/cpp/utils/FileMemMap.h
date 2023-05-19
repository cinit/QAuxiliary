//
// Created by kinit on 2021-10-28.
//

#ifndef NATIVES_FILEMEMMAP_H
#define NATIVES_FILEMEMMAP_H

#include <cstddef>

class FileMemMap {
private:
    void* mAddress = nullptr;
    size_t mMapLength = 0;
    size_t mLength = 0;

public:
    FileMemMap() = default;

    ~FileMemMap() noexcept;

    FileMemMap(const FileMemMap&) = delete;

    FileMemMap& operator=(const FileMemMap& other) = delete;

    /**
     * Map a file into memory.
     * @param path the absolute path to the file to map.
     * @param readOnly whether the file should be mapped read-only.
     * @param length the length of the file to map, may be 0 to map the entire file.
     * @return 0 on success, errno on error.
     */
    [[nodiscard]] int mapFilePath(const char* path, bool readOnly = true, size_t length = 0);

    /**
     * Map a file into memory.
     * Note that the fd will not be closed when the FileMemMap is destroyed.
     * @param fd the file descriptor of the file to map.
     * @param readOnly whether the file should be mapped read-only.
     * @param length the length of the file to map, may be 0 to map the entire file.
     * @param shared whether the file should be mapped with MAP_SHARED or MAP_PRIVATE.
     * @return 0 on success, errno on error.
     */
    [[nodiscard]] int mapFileDescriptor(int fd, bool readOnly = true, size_t length = 0, bool shared = false);

    /**
     * Get the address of the mapped file.
     * @return address of the mapped file, or nullptr if the file is not mapped.
     */
    [[nodiscard]] inline void* getAddress() const noexcept {
        return mAddress;
    }

    /**
     * Get the length of the mapped file.
     * @return length of the mapped file, or 0 if the file is not mapped.
     */
    [[nodiscard]] inline size_t getLength() const noexcept {
        return mLength;
    }

    /**
     * Get is the file is mapped.
     * @return true if the file is mapped, false otherwise.
     */
    [[nodiscard]] inline bool isValid() const noexcept {
        return mAddress != nullptr && mLength != 0;
    }

    /**
     * Unmap the file.
     * Note that the fd will not be closed when the FileMemMap is destroyed.
     */
    void unmap() noexcept;

    /**
     * Detach the memory mapping from this object, this is useful when you want to keep the memory mapping
     * when you destroy this object. Note that you must unmap the memory mapping yourself when you are done.
     * When you detach the memory mapping, the FileMemMap object will be reset to an invalid state.
     */
    void detach() noexcept;
};

#endif //NATIVES_FILEMEMMAP_H
