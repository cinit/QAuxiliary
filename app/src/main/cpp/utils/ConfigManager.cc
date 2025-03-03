//
// Created by sulfate on 2023-06-22.
//

#include "ConfigManager.h"

#include <mutex>
#include <map>
#include <fmt/format.h>

#include <android/set_abort_message.h>

#include "MMKV.h"

// keep the following the same as MmkvConfigManagerImpl.java
static constexpr const char* TYPE_SUFFIX = "$shadow$type";
static constexpr const char* CLASS_SUFFIX = "$shadow$class";
static constexpr int TYPE_BOOL = 0x80 + 2;
static constexpr int TYPE_INT = 0x80 + 4;
static constexpr int TYPE_LONG = 0x80 + 6;
static constexpr int TYPE_FLOAT = 0x80 + 7;
static constexpr int TYPE_STRING = 0x80 + 31;
static constexpr int TYPE_STRING_SET = 0x80 + 32;
static constexpr int TYPE_BYTES = 0x80 + 33;
static constexpr int TYPE_SERIALIZABLE = 0x80 + 41;
static constexpr int TYPE_JSON = 0x80 + 42;

[[noreturn]] static void AbortWithMsg(const char* msg) {
    android_set_abort_message(msg);
    abort();
}

qauxv::ConfigManager::ConfigManager(MMKV* mmkv, std::string_view mmkvId)
        : mMmkv(mmkv), mMmkvId(mmkvId) {}

qauxv::ConfigManager& qauxv::ConfigManager::GetDefaultConfig() {
    static ConfigManager* sConfigManager = nullptr;
    if (sConfigManager != nullptr) {
        return *sConfigManager;
    }
    auto id = "global_config";
    auto mmkv = MMKV::mmkvWithID(std::string(id), mmkv::DEFAULT_MMAP_SIZE, MMKV_MULTI_PROCESS);
    if (mmkv == nullptr) {
        AbortWithMsg(fmt::format("Failed to create MMKV with id '{}'", id).c_str());
    }
    sConfigManager = new ConfigManager(mmkv, id);
    return *sConfigManager;
}

qauxv::ConfigManager& qauxv::ConfigManager::GetCache() {
    static ConfigManager* sConfigManager = nullptr;
    if (sConfigManager != nullptr) {
        return *sConfigManager;
    }
    auto id = "global_cache";
    auto mmkv = MMKV::mmkvWithID(std::string(id), mmkv::DEFAULT_MMAP_SIZE, MMKV_MULTI_PROCESS);
    if (mmkv == nullptr) {
        AbortWithMsg(fmt::format("Failed to create MMKV with id '{}'", id).c_str());
    }
    sConfigManager = new ConfigManager(mmkv, id);
    return *sConfigManager;
}

qauxv::ConfigManager& qauxv::ConfigManager::GetOatInlineDeoptCache() {
    static ConfigManager* sConfigManager = nullptr;
    if (sConfigManager != nullptr) {
        return *sConfigManager;
    }
    auto id = "oat_inline_deopt_cache";
    auto mmkv = MMKV::mmkvWithID(std::string(id), mmkv::DEFAULT_MMAP_SIZE, MMKV_MULTI_PROCESS);
    if (mmkv == nullptr) {
        AbortWithMsg(fmt::format("Failed to create MMKV with id '{}'", id).c_str());
    }
    sConfigManager = new ConfigManager(mmkv, id);
    return *sConfigManager;
}

qauxv::ConfigManager& qauxv::ConfigManager::ForAccount(int64_t uin) {
    static std::mutex sMutex;
    static std::map<int64_t, ConfigManager*> sConfigManagerMap;
    {
        std::lock_guard<std::mutex> lock(sMutex);
        auto it = sConfigManagerMap.find(uin);
        if (it != sConfigManagerMap.end()) {
            return *it->second;
        }
    }
    auto id = fmt::format("u_{}", uin);
    auto mmkv = MMKV::mmkvWithID(std::string(id), mmkv::DEFAULT_MMAP_SIZE, MMKV_MULTI_PROCESS);
    if (mmkv == nullptr) {
        AbortWithMsg(fmt::format("Failed to create MMKV with id '{}'", id).c_str());
    }
    auto configManager = new ConfigManager(mmkv, id);
    {
        std::lock_guard<std::mutex> lock(sMutex);
        sConfigManagerMap[uin] = configManager;
    }
    return *configManager;
}

MMKV* qauxv::ConfigManager::GetInternalMmkv() {
    return mMmkv;
}

std::string qauxv::ConfigManager::GetMmkvId() {
    return mMmkvId;
}

void qauxv::ConfigManager::Save() {
    mMmkv->sync();
}

void qauxv::ConfigManager::ClearAll() {
    mMmkv->clearAll();
}

bool qauxv::ConfigManager::ContainsKey(const std::string& key) {
    return mMmkv->containsKey(key);
}

void qauxv::ConfigManager::Remove(const std::string& key) {
    mMmkv->removeValuesForKeys({key, key + TYPE_SUFFIX});
}

std::optional<std::string> qauxv::ConfigManager::GetString(const std::string& key) {
    std::string result;
    if (mMmkv->getString(key, result)) {
        return result;
    } else {
        return std::nullopt;
    }
}

std::string qauxv::ConfigManager::GetString(const std::string& key, std::string_view defaultValue) {
    std::string result;
    if (mMmkv->getString(key, result)) {
        return result;
    } else {
        return std::string(defaultValue);
    }
}

void qauxv::ConfigManager::PutString(const std::string& key, std::string_view value) {
    mMmkv->set(std::string(value), key);
    mMmkv->set(TYPE_STRING, key + TYPE_SUFFIX);
}

std::optional<bool> qauxv::ConfigManager::GetBool(const std::string& key) {
    bool hasValue = false;
    auto result = mMmkv->getBool(key, false, &hasValue);
    if (hasValue) {
        return result;
    } else {
        return std::nullopt;
    }
}

bool qauxv::ConfigManager::GetBool(const std::string& key, bool defaultValue) {
    return mMmkv->getBool(key, defaultValue);
}

void qauxv::ConfigManager::PutBool(const std::string& key, bool value) {
    mMmkv->set(value, key);
    mMmkv->set(TYPE_BOOL, key + TYPE_SUFFIX);
}

std::optional<int32_t> qauxv::ConfigManager::GetInt32(const std::string& key) {
    bool hasValue = false;
    auto result = mMmkv->getInt32(key, 0, &hasValue);
    if (hasValue) {
        return result;
    } else {
        return std::nullopt;
    }
}

int32_t qauxv::ConfigManager::GetInt32(const std::string& key, int32_t defaultValue) {
    return mMmkv->getInt32(key, defaultValue);
}

void qauxv::ConfigManager::PutInt32(const std::string& key, int32_t value) {
    mMmkv->set(value, key);
    mMmkv->set(TYPE_INT, key + TYPE_SUFFIX);
}

std::optional<uint32_t> qauxv::ConfigManager::GetUInt32(const std::string& key) {
    bool hasValue = false;
    auto result = mMmkv->getUInt32(key, 0, &hasValue);
    if (hasValue) {
        return result;
    } else {
        return std::nullopt;
    }
}

uint32_t qauxv::ConfigManager::GetUInt32(const std::string& key, uint32_t defaultValue) {
    return mMmkv->getUInt32(key, defaultValue);
}

void qauxv::ConfigManager::PutUInt32(const std::string& key, uint32_t value) {
    mMmkv->set(value, key);
    mMmkv->set(TYPE_INT, key + TYPE_SUFFIX);
}

std::optional<int64_t> qauxv::ConfigManager::GetInt64(const std::string& key) {
    bool hasValue = false;
    auto result = mMmkv->getInt64(key, 0, &hasValue);
    if (hasValue) {
        return result;
    } else {
        return std::nullopt;
    }
}

int64_t qauxv::ConfigManager::GetInt64(const std::string& key, int64_t defaultValue) {
    return mMmkv->getInt64(key, defaultValue);
}

void qauxv::ConfigManager::PutInt64(const std::string& key, int64_t value) {
    mMmkv->set(value, key);
    mMmkv->set(TYPE_LONG, key + TYPE_SUFFIX);
}

std::optional<uint64_t> qauxv::ConfigManager::GetUInt64(const std::string& key) {
    bool hasValue = false;
    auto result = mMmkv->getUInt64(key, 0, &hasValue);
    if (hasValue) {
        return result;
    } else {
        return std::nullopt;
    }
}

uint64_t qauxv::ConfigManager::GetUInt64(const std::string& key, uint64_t defaultValue) {
    return mMmkv->getUInt64(key, defaultValue);
}

void qauxv::ConfigManager::PutUInt64(const std::string& key, uint64_t value) {
    mMmkv->set(value, key);
    mMmkv->set(TYPE_LONG, key + TYPE_SUFFIX);
}

std::optional<float> qauxv::ConfigManager::GetFloat(const std::string& key) {
    bool hasValue = false;
    auto result = mMmkv->getFloat(key, 0, &hasValue);
    if (hasValue) {
        return result;
    } else {
        return std::nullopt;
    }
}

float qauxv::ConfigManager::GetFloat(const std::string& key, float defaultValue) {
    return mMmkv->getFloat(key, defaultValue);
}

void qauxv::ConfigManager::PutFloat(const std::string& key, float value) {
    mMmkv->set(value, key);
    mMmkv->set(TYPE_FLOAT, key + TYPE_SUFFIX);
}

std::optional<std::vector<uint8_t>> qauxv::ConfigManager::GetBytes(const std::string& key) {
    mmkv::MMBuffer buffer;
    if (mMmkv->getBytes(key, buffer)) {
        // this may be a bit slow... one will want to use mmkv::MMBuffer directly
        return std::vector<uint8_t>(reinterpret_cast<uint8_t*>(buffer.getPtr()),
                                    reinterpret_cast<uint8_t*>(buffer.getPtr()) + buffer.length());
    } else {
        return std::nullopt;
    }
}

void qauxv::ConfigManager::PutBytes(const std::string& key, std::span<const uint8_t> value) {
    mMmkv->set(mmkv::MMBuffer((void*) (value.data()), value.size()), key);
    mMmkv->set(TYPE_BYTES, key + TYPE_SUFFIX);
}

std::vector<uint8_t> qauxv::ConfigManager::GetBytes(const std::string& key, std::span<const uint8_t> defaultValue) {
    auto result = GetBytes(key);
    if (result.has_value()) {
        return result.value();
    } else {
        return std::vector<uint8_t>(defaultValue.begin(), defaultValue.end());
    }
}

std::vector<std::string> qauxv::ConfigManager::GetAllKeys() {
    return mMmkv->allKeys();
}

size_t qauxv::ConfigManager::GetValueSize(const std::string& key, bool actualSize) {
    return mMmkv->getValueSize(key, actualSize);
}
