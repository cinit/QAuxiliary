//
// Created by sulfate on 2023-06-22.
//

#ifndef QAUXV_CONFIGMANAGER_H
#define QAUXV_CONFIGMANAGER_H

#include <cstdint>
#include <optional>
#include <string>
#include <span>
#include <vector>
#include <string_view>

class MMKV;

namespace qauxv {

class ConfigManager {
public:
    ConfigManager() = delete;
    ~ConfigManager() = default;

    // no copy and assign
    ConfigManager(const ConfigManager&) = delete;
    ConfigManager& operator=(const ConfigManager&) = delete;

private:
    ConfigManager(MMKV* mmkv, std::string_view mmkvId);

public:
    [[nodiscard]] static ConfigManager& GetDefaultConfig();
    [[nodiscard]] static ConfigManager& GetCache();
    [[nodiscard]] static ConfigManager& GetOatInlineDeoptCache();
    [[nodiscard]] static ConfigManager& ForAccount(int64_t uin);

    [[nodiscard]] bool ContainsKey(const std::string& key);
    void Remove(const std::string& key);

    // string
    [[nodiscard]] std::optional<std::string> GetString(const std::string& key);
    void PutString(const std::string& key, std::string_view value);
    [[nodiscard]] std::string GetString(const std::string& key, std::string_view defaultValue);

    // bool
    [[nodiscard]] std::optional<bool> GetBool(const std::string& key);
    [[nodiscard]] bool GetBool(const std::string& key, bool defaultValue);
    void PutBool(const std::string& key, bool value);

    // int32
    [[nodiscard]] std::optional<int32_t> GetInt32(const std::string& key);
    [[nodiscard]] int32_t GetInt32(const std::string& key, int32_t defaultValue);
    void PutInt32(const std::string& key, int32_t value);

    // uint32
    [[nodiscard]] std::optional<uint32_t> GetUInt32(const std::string& key);
    [[nodiscard]] uint32_t GetUInt32(const std::string& key, uint32_t defaultValue);
    void PutUInt32(const std::string& key, uint32_t value);

    // int64
    [[nodiscard]] std::optional<int64_t> GetInt64(const std::string& key);
    [[nodiscard]] int64_t GetInt64(const std::string& key, int64_t defaultValue);
    void PutInt64(const std::string& key, int64_t value);

    // uint64
    [[nodiscard]] std::optional<uint64_t> GetUInt64(const std::string& key);
    [[nodiscard]] uint64_t GetUInt64(const std::string& key, uint64_t defaultValue);
    void PutUInt64(const std::string& key, uint64_t value);

    // float
    [[nodiscard]] std::optional<float> GetFloat(const std::string& key);
    [[nodiscard]] float GetFloat(const std::string& key, float defaultValue);
    void PutFloat(const std::string& key, float value);

    // bytes
    [[nodiscard]] std::optional<std::vector<uint8_t>> GetBytes(const std::string& key);
    void PutBytes(const std::string& key, std::span<const uint8_t> value);
    [[nodiscard]] std::vector<uint8_t> GetBytes(const std::string& key, std::span<const uint8_t> defaultValue);

    void Save();

    void ClearAll();

    [[nodiscard]] std::vector<std::string> GetAllKeys();

    [[nodiscard]] MMKV* GetInternalMmkv();

    [[nodiscard]] std::string GetMmkvId();

    [[nodiscard]] size_t GetValueSize(const std::string& key, bool actualSize);

private:
    MMKV* const mMmkv;
    const std::string mMmkvId;
};

}

#endif //QAUXV_CONFIGMANAGER_H
