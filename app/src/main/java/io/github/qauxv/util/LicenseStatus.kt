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

package io.github.qauxv.util

import io.github.qauxv.SyncUtils
import io.github.qauxv.bridge.AppRuntimeHelper
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.remote.TransactionHelper.getUserStatus
import io.github.qauxv.util.data.UserStatusConst
import java.util.Date

object LicenseStatus {

    @JvmField
    val sDisableCommonHooks: Boolean = isBlacklisted()

    private const val qn_eula_status = "qa_eula_status" //typo, ignore it
    private const val qn_user_auth_status = "qn_user_auth_status"
    private const val qn_user_auth_last_update = "qn_user_auth_last_update"
    const val CURRENT_EULA_VERSION = 10

    @JvmStatic
    fun getEulaStatus(): Int {
        val cfg = ConfigManager.getDefaultConfig()
        return cfg.getIntOrDefault(qn_eula_status, 0)
    }

    @JvmStatic
    fun setEulaStatus(status: Int) {
        val cfg = ConfigManager.getDefaultConfig()
        cfg.putInt(qn_eula_status, status)
        cfg.save()
    }

    @JvmStatic
    fun hasEulaUpdated(): Boolean {
        val s = getEulaStatus()
        return s != 0 && s != CURRENT_EULA_VERSION
    }

    @JvmStatic
    fun hasUserAcceptEula(): Boolean {
        return getEulaStatus() == CURRENT_EULA_VERSION
    }

    @JvmStatic
    fun setUserCurrentStatus() {
        SyncUtils.async {
            val cfg = ConfigManager.getDefaultConfig()
            val currentStatus = getUserStatus(AppRuntimeHelper.getLongAccountUin())
            // 如果获取不到就放弃更新状态
            if (currentStatus == UserStatusConst.notExist) {
                return@async
            }
            Log.i("User Current Status: $currentStatus")
            cfg.putInt(qn_user_auth_status, currentStatus)
            cfg.putLong(qn_user_auth_last_update, System.currentTimeMillis())
            cfg.save()
            Log.i("User Current Status in ConfigManager: " + cfg.getIntOrDefault(qn_user_auth_status, -1))
            Log.i("User Status Last Update: " + Date(cfg.getLongOrDefault(qn_user_auth_last_update, System.currentTimeMillis())))
        }
    }


    @JvmStatic
    fun isInsider(): Boolean {
        val cfg = ConfigManager.getDefaultConfig()
        var currentStatus = cfg.getIntOrDefault(qn_user_auth_status, -1)
        if (currentStatus == UserStatusConst.notExist) {
            setUserCurrentStatus()
            currentStatus = cfg.getIntOrDefault(qn_user_auth_status, -1)
        }
        val lastUpdate = cfg.getLongOrDefault(qn_user_auth_last_update, System.currentTimeMillis())
        if (lastUpdate >= lastUpdate + 30 * 60 * 1000) {
            setUserCurrentStatus()
        }
        return currentStatus == UserStatusConst.developer
    }

    @JvmStatic
    fun isBlacklisted(): Boolean {
        val cfg = ConfigManager.getDefaultConfig()
        var currentStatus = cfg.getIntOrDefault(qn_user_auth_status, -1)
        if (currentStatus == UserStatusConst.notExist) {
            setUserCurrentStatus()
            currentStatus = cfg.getIntOrDefault(qn_user_auth_status, -1)
        }
        val lastUpdate = cfg.getLongOrDefault(qn_user_auth_last_update, System.currentTimeMillis())
        if (lastUpdate >= lastUpdate + 30 * 60 * 1000) {
            setUserCurrentStatus()
        }
        return currentStatus == UserStatusConst.blacklisted
    }

    @JvmStatic
    fun isWhitelisted(): Boolean {
        if (isInsider())
            return true
        val cfg = ConfigManager.getDefaultConfig()
        var currentStatus = cfg.getIntOrDefault(qn_user_auth_status, -1)
        if (currentStatus == UserStatusConst.notExist) {
            setUserCurrentStatus()
            currentStatus = cfg.getIntOrDefault(qn_user_auth_status, -1)
        }
        val lastUpdate = cfg.getLongOrDefault(qn_user_auth_last_update, System.currentTimeMillis())
        if (lastUpdate >= lastUpdate + 30 * 60 * 1000) {
            setUserCurrentStatus()
        }
        return currentStatus == UserStatusConst.whitelisted
    }


}
