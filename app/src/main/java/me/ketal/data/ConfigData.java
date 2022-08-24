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

package me.ketal.data;

import android.os.Looper;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.config.ConfigManager;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.util.Toasts;

public class ConfigData<T> {

    final String mKeyName;
    final ConfigManager mgr;

    public ConfigData(String keyName) {
        this(keyName, ConfigManager.getDefaultConfig());
    }

    public ConfigData(String keyName, ConfigManager manager) {
        mKeyName = keyName;
        mgr = manager;
    }

    public void remove() {
        try {
            mgr.remove(mKeyName);
        } catch (Exception e) {
            Log.e(e);
        }
    }

    public T getValue() {
        try {
            return (T) mgr.getObject(mKeyName);
        } catch (Exception e) {
            try {
                mgr.remove(mKeyName);
            } catch (Exception ignored) {
            }
            Log.e(e);
            return null;
        }
    }

    public void setValue(T value) {
        try {
            mgr.putObject(mKeyName, value);
            mgr.save();
        } catch (Exception e) {
            try {
                mgr.remove(mKeyName);
            } catch (Exception ignored) {
            }
            Log.e(e);
            if (Looper.myLooper() == Looper.getMainLooper()) {
                Toasts.error(HostInfo.getApplication(), "设置存储失败, 请重新设置" + e + "");
            } else {
                SyncUtils.post(() -> Toasts
                    .error(HostInfo.getApplication(), "设置存储失败, 请重新设置" + e + ""));
            }
        }
    }

    public T getOrDefault(T def) {
        try {
            return (T) mgr.getOrDefault(mKeyName, def);
        } catch (Exception e) {
            Log.e(e);
            return def;
        }
    }
}
