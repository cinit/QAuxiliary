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
package io.github.qauxv.util;

import io.github.qauxv.BuildConfig;
import io.github.qauxv.SyncUtils;
import io.github.qauxv.bridge.AppRuntimeHelper;
import io.github.qauxv.config.ConfigManager;
import io.github.qauxv.remote.TransactionHelper;
import io.github.qauxv.util.data.UserStatusConst;
import java.util.Date;

public class LicenseStatus {

    public static final String qn_eula_status = "qa_eula_status";//typo, ignore it
    public static final String qn_user_auth_status = "qn_user_auth_status";
    public static final String qn_user_auth_last_update = "qn_user_auth_last_update";
    public static final boolean sDisableCommonHooks = LicenseStatus.isBlacklisted();
    public static final int CURRENT_EULA_VERSION = 9;

    public static int getEulaStatus() {
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        return cfg.getIntOrDefault(qn_eula_status, 0);
    }

    public static void setEulaStatus(int status) {
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        cfg.putInt(qn_eula_status, status);
        cfg.save();
    }

    public static boolean hasEulaUpdated() {
        int s = getEulaStatus();
        return (s != 0 && s != CURRENT_EULA_VERSION);
    }

    public static boolean hasUserAcceptEula() {
        // TODO: 2022-02-10 add EULA activity
        return true;
        // return getEulaStatus() == CURRENT_EULA_VERSION;
    }

    public static void setUserCurrentStatus() {
        SyncUtils.async(() -> {
            ConfigManager cfg = ConfigManager.getDefaultConfig();
            int currentStatus = TransactionHelper.getUserStatus(AppRuntimeHelper.getLongAccountUin());
            // 如果获取不到就放弃更新状态
            if (currentStatus == UserStatusConst.notExist) {
                return;
            }
            Log.i("User Current Status: " + "" + currentStatus);
            cfg.putInt(qn_user_auth_status, currentStatus);
            cfg.putLong(qn_user_auth_last_update, System.currentTimeMillis());
            cfg.save();
            Log.i("User Current Status in ConfigManager: "
                    + cfg.getIntOrDefault(qn_user_auth_status, -1));
            Log.i("User Status Last Update: " + new Date(cfg
                    .getLongOrDefault(qn_user_auth_last_update, System.currentTimeMillis())));
        });
    }


    public static boolean isInsider() {
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        int currentStatus = cfg.getIntOrDefault(qn_user_auth_status, -1);
        if (currentStatus == UserStatusConst.notExist) {
            LicenseStatus.setUserCurrentStatus();
            currentStatus = cfg.getIntOrDefault(qn_user_auth_status, -1);
        }
        long lastUpdate = cfg.getLongOrDefault(qn_user_auth_last_update, System.currentTimeMillis());
        if (lastUpdate >= lastUpdate + 30 * 60 * 1000) {
            LicenseStatus.setUserCurrentStatus();
        }
        return currentStatus == UserStatusConst.developer;
    }

    public static boolean isBlacklisted() {
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        int currentStatus = cfg.getIntOrDefault(qn_user_auth_status, -1);
        if (currentStatus == UserStatusConst.notExist) {
            LicenseStatus.setUserCurrentStatus();
            currentStatus = cfg.getIntOrDefault(qn_user_auth_status, -1);
        }
        long lastUpdate = cfg.getLongOrDefault(qn_user_auth_last_update, System.currentTimeMillis());
        if (lastUpdate >= lastUpdate + 30 * 60 * 1000) {
            LicenseStatus.setUserCurrentStatus();
        }
        return currentStatus == UserStatusConst.blacklisted;
    }

    public static boolean isWhitelisted() {
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        int currentStatus = cfg.getIntOrDefault(qn_user_auth_status, -1);
        if (currentStatus == UserStatusConst.notExist) {
            LicenseStatus.setUserCurrentStatus();
            currentStatus = cfg.getIntOrDefault(qn_user_auth_status, -1);
        }
        long lastUpdate = cfg.getLongOrDefault(qn_user_auth_last_update, System.currentTimeMillis());
        if (lastUpdate >= lastUpdate + 30 * 60 * 1000) {
            LicenseStatus.setUserCurrentStatus();
        }
        return currentStatus == UserStatusConst.whitelisted || currentStatus == UserStatusConst.developer;
    }

    public static boolean isAsserted() {
        ConfigManager cfg = ConfigManager.getDefaultConfig();
        int currentStatus = cfg.getIntOrDefault(qn_user_auth_status, -1);
        if (BuildConfig.DEBUG) {
            return true;
        }
        if (currentStatus == UserStatusConst.notExist) {
            LicenseStatus.setUserCurrentStatus();
            currentStatus = cfg.getIntOrDefault(qn_user_auth_status, -1);
        }
        long lastUpdate = cfg.getLongOrDefault(qn_user_auth_last_update, System.currentTimeMillis());
        if (lastUpdate >= lastUpdate + 30 * 60 * 1000) {
            LicenseStatus.setUserCurrentStatus();
        }
        return currentStatus == UserStatusConst.whitelisted || currentStatus == UserStatusConst.developer;
    }

}
