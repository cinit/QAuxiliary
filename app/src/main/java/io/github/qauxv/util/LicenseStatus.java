/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
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
package io.github.qauxv.util;

import io.github.qauxv.BuildConfig;
import io.github.qauxv.activity.EulaActivity;
import io.github.qauxv.bridge.AppRuntimeHelper;
import io.github.qauxv.config.ConfigManager;
import io.github.qauxv.remote.TransactionHelper;
import io.github.qauxv.util.data.UserStatusConst;
import java.io.IOException;
import java.util.Date;
import me.singleneuron.qn_kernel.data.HostInfo;

public class LicenseStatus {

    public static final String qn_eula_status = "qa_eula_status";//typo, ignore it
    public static final String qn_user_auth_status = "qn_user_auth_status";
    public static final String qn_user_auth_last_update = "qn_user_auth_last_update";
    public static final boolean sDisableCommonHooks = LicenseStatus.isBlacklisted();

    public static int getEulaStatus() {
        return ConfigManager.getDefaultConfig().getIntOrDefault(qn_eula_status, 0);
    }

    public static void setEulaStatus(int status) {
        ConfigManager.getDefaultConfig().putInt(qn_eula_status, status);
        try {
            ConfigManager.getDefaultConfig().save();
        } catch (IOException e) {
            Log.e(e);
            Toasts.error(HostInfo.getHostInfo().getApplication(), e.toString());
        }
    }

    public static boolean hasEulaUpdated() {
        int s = getEulaStatus();
        return (s != 0 && s != EulaActivity.CURRENT_EULA_VERSION);
    }

    public static boolean hasUserAcceptEula() {
        return getEulaStatus() == EulaActivity.CURRENT_EULA_VERSION;
    }


    public static void setUserCurrentStatus() {
        new Thread(() -> {
            int currentStatus = TransactionHelper.getUserStatus(AppRuntimeHelper.getLongAccountUin());
            // 如果获取不到就放弃更新状态
            if (currentStatus == UserStatusConst.notExist) {
                return;
            }
            Log.i("User Current Status: "
                + "" + currentStatus);
            ConfigManager.getDefaultConfig().putInt(qn_user_auth_status, currentStatus);
            ConfigManager.getDefaultConfig()
                .putLong(qn_user_auth_last_update, System.currentTimeMillis());
            try {
                ConfigManager.getDefaultConfig().save();
                Log.i("User Current Status in ConfigManager: "
                    + ConfigManager.getDefaultConfig().getIntOrDefault(qn_user_auth_status, -1));
                Log.i("User Status Last Update: " + new Date(ConfigManager.getDefaultConfig()
                    .getLongOrDefault(qn_user_auth_last_update, System.currentTimeMillis())));
            } catch (IOException e) {
                Log.e(e);
                Toasts.error(HostInfo.getHostInfo().getApplication(), e.toString());
            }
        }).start();

    }


    public static boolean isInsider() {
        int currentStatus = ConfigManager.getDefaultConfig()
            .getIntOrDefault(qn_user_auth_status, -1);
        if (currentStatus == UserStatusConst.notExist) {
            LicenseStatus.setUserCurrentStatus();
            currentStatus = ConfigManager.getDefaultConfig()
                .getIntOrDefault(qn_user_auth_status, -1);
        }
        long lastUpdate = ConfigManager.getDefaultConfig()
            .getLongOrDefault(qn_user_auth_last_update, System.currentTimeMillis());
        if (lastUpdate >= lastUpdate + 30 * 60 * 1000) {
            LicenseStatus.setUserCurrentStatus();
        }
        return currentStatus == UserStatusConst.developer;
    }

    public static boolean isBlacklisted() {
        int currentStatus = ConfigManager.getDefaultConfig()
            .getIntOrDefault(qn_user_auth_status, -1);
        if (currentStatus == UserStatusConst.notExist) {
            LicenseStatus.setUserCurrentStatus();
            currentStatus = ConfigManager.getDefaultConfig()
                .getIntOrDefault(qn_user_auth_status, -1);
        }
        long lastUpdate = ConfigManager.getDefaultConfig()
            .getLongOrDefault(qn_user_auth_last_update, System.currentTimeMillis());
        if (lastUpdate >= lastUpdate + 30 * 60 * 1000) {
            LicenseStatus.setUserCurrentStatus();
        }
        return currentStatus == UserStatusConst.blacklisted;
    }

    public static boolean isWhitelisted() {
        int currentStatus = ConfigManager.getDefaultConfig()
            .getIntOrDefault(qn_user_auth_status, -1);
        if (currentStatus == UserStatusConst.notExist) {
            LicenseStatus.setUserCurrentStatus();
            currentStatus = ConfigManager.getDefaultConfig()
                .getIntOrDefault(qn_user_auth_status, -1);
        }
        long lastUpdate = ConfigManager.getDefaultConfig()
            .getLongOrDefault(qn_user_auth_last_update, System.currentTimeMillis());
        if (lastUpdate >= lastUpdate + 30 * 60 * 1000) {
            LicenseStatus.setUserCurrentStatus();
        }
        return currentStatus == UserStatusConst.whitelisted
            || currentStatus == UserStatusConst.developer;
    }

    public static boolean isAsserted() {
        int currentStatus = ConfigManager.getDefaultConfig()
            .getIntOrDefault(qn_user_auth_status, -1);
        if (BuildConfig.DEBUG) {
            return true;
        }
        if (currentStatus == UserStatusConst.notExist) {
            LicenseStatus.setUserCurrentStatus();
            currentStatus = ConfigManager.getDefaultConfig()
                .getIntOrDefault(qn_user_auth_status, -1);
        }
        long lastUpdate = ConfigManager.getDefaultConfig()
            .getLongOrDefault(qn_user_auth_last_update, System.currentTimeMillis());
        if (lastUpdate >= lastUpdate + 30 * 60 * 1000) {
            LicenseStatus.setUserCurrentStatus();
        }
        return currentStatus == UserStatusConst.whitelisted
            || currentStatus == UserStatusConst.developer;
    }

}
