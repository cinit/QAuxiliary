/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */
package io.github.qauxv.config;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.github.qauxv.bridge.AppRuntimeHelper;
import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ConfigManager implements SharedPreferences, SharedPreferences.Editor {

    private static ConfigManager sDefConfig;
    private static ConfigManager sCache;
    private static ConfigManager sOatInlineDeoptCacahe;
    private static final ConcurrentHashMap<Long, ConfigManager> sUinConfig =
        new ConcurrentHashMap<>(4);

    protected ConfigManager() {
    }

    private static ConfigManager sLastUseEmoticonStore;
    @NonNull
    public static synchronized ConfigManager getLastUseEmoticonStore() {
        if (sLastUseEmoticonStore == null) {
            sLastUseEmoticonStore = new MmkvConfigManagerImpl("last_use_emoticon_time");
        }
        return sLastUseEmoticonStore;
    }
    private static ConfigManager sDumpTG_LastUseEmoticonPackStore;
    @NonNull
    public static synchronized ConfigManager getDumpTG_LastUseEmoticonPackStore() {
        if (sDumpTG_LastUseEmoticonPackStore == null) {
            sDumpTG_LastUseEmoticonPackStore = new MmkvConfigManagerImpl("sDumpTG_LastUseEmoticonPackStore");
        }
        return sDumpTG_LastUseEmoticonPackStore;
    }

    private static ConfigManager sDumpTG_LastUseEmoticonStore;
    @NonNull
    public static synchronized ConfigManager getDumpTG_LastUseEmoticonStore() {
        if (sDumpTG_LastUseEmoticonStore == null) {
            sDumpTG_LastUseEmoticonStore = new MmkvConfigManagerImpl("sDumpTG_LastUseEmoticonStore");
        }
        return sDumpTG_LastUseEmoticonStore;
    }

    @NonNull
    public static synchronized ConfigManager getDefaultConfig() {
        if (sDefConfig == null) {
            sDefConfig = new MmkvConfigManagerImpl("global_config");
        }
        return sDefConfig;
    }

    @NonNull
    public static synchronized ConfigManager getOatInlineDeoptCache() {
        if (sOatInlineDeoptCacahe == null) {
            sOatInlineDeoptCacahe = new MmkvConfigManagerImpl("oat_inline_deopt_cache");
        }
        return sOatInlineDeoptCacahe;
    }

    /**
     * Get isolated config for a specified account
     *
     * @param uin account number
     * @return config for raed/write
     */
    @NonNull
    public static synchronized ConfigManager forAccount(long uin) {
        if (uin < 10000) {
            throw new IllegalArgumentException("uin must >= 10000");
        }
        ConfigManager cfg = sUinConfig.get(uin);
        if (cfg != null) {
            return cfg;
        }
        cfg = new MmkvConfigManagerImpl("u_" + uin);
        sUinConfig.put(uin, cfg);
        // save uin to config
        if (cfg.getLongOrDefault("uin", 0) == 0) {
            cfg.putLong("uin", uin);
        }
        return cfg;
    }

    /**
     * Get isolated config for current account logged in. See {@link #forAccount(long)}
     *
     * @return if no account is logged in, {@code null} will be returned.
     */
    @Nullable
    public static ConfigManager getExFriendCfg() {
        long uin = AppRuntimeHelper.getLongAccountUin();
        if (uin >= 10000) {
            return forAccount(uin);
        }
        return null;
    }

    @NonNull
    public static synchronized ConfigManager getCache() {
        if (sCache == null) {
            sCache = new MmkvConfigManagerImpl("global_cache");
        }
        return sCache;
    }

    @Nullable
    public abstract File getFile();

    @Nullable
    public Object getOrDefault(@NonNull String key, @Nullable Object def) {
        if (!containsKey(key)) {
            return def;
        }
        return getObject(key);
    }

    public boolean getBooleanOrFalse(@NonNull String key) {
        return getBooleanOrDefault(key, false);
    }

    public boolean getBooleanOrDefault(@NonNull String key, boolean def) {
        return getBoolean(key, def);
    }

    public int getIntOrDefault(@NonNull String key, int def) {
        return getInt(key, def);
    }

    @Nullable
    public abstract String getString(@NonNull String key);

    @NonNull
    public String getStringOrDefault(@NonNull String key, @NonNull String defVal) {
        return getString(key, defVal);
    }

    @NonNull
    public Set<String> getStringSetOrDefault(@NonNull String key, @NonNull Set<String> defVal) {
        return getStringSet(key, defVal);
    }

    @Nullable
    public abstract Object getObject(@NonNull String key);

    @Nullable
    public byte[] getBytes(@NonNull String key) {
        return getBytes(key, null);
    }

    @Nullable
    public abstract byte[] getBytes(@NonNull String key, @Nullable byte[] defValue);

    @NonNull
    public abstract byte[] getBytesOrDefault(@NonNull String key, @NonNull byte[] defValue);

    @NonNull
    public abstract ConfigManager putBytes(@NonNull String key, @NonNull byte[] value);

    /**
     * @return READ-ONLY all config
     * @deprecated Avoid use getAll(), MMKV only have limited support for this.
     */
    @Override
    @Deprecated
    @NonNull
    public abstract Map<String, ?> getAll();

    public abstract void save();

    public long getLongOrDefault(@Nullable String key, long i) {
        return getLong(key, i);
    }

    @NonNull
    public abstract ConfigManager putObject(@NonNull String key, @NonNull Object v);

    public boolean containsKey(@NonNull String k) {
        return contains(k);
    }

    @NonNull
    @Override
    public Editor edit() {
        return this;
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(
        @NonNull OnSharedPreferenceChangeListener listener) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(
        @NonNull OnSharedPreferenceChangeListener listener) {
        throw new UnsupportedOperationException("not implemented");
    }

    public abstract boolean isReadOnly();

    public abstract boolean isPersistent();
}
