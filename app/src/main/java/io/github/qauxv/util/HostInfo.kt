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
@file:JvmName("HostInfo")
package io.github.qauxv.util

import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat

const val PACKAGE_NAME_QQ = "com.tencent.mobileqq"
const val PACKAGE_NAME_QQ_INTERNATIONAL = "com.tencent.mobileqqi"
const val PACKAGE_NAME_QQ_LITE = "com.tencent.qqlite"
const val PACKAGE_NAME_TIM = "com.tencent.tim"
const val PACKAGE_NAME_SELF = "io.github.qauxv"

lateinit var hostInfo: HostInfoImpl

fun init(applicationContext: Application) {
    if (::hostInfo.isInitialized) throw IllegalStateException("Host Information Provider has been already initialized")
    val packageInfo = getHostInfo(applicationContext)
    val packageName = applicationContext.packageName
    hostInfo = HostInfoImpl(
        applicationContext,
        packageName,
        applicationContext.applicationInfo.loadLabel(applicationContext.packageManager).toString(),
        PackageInfoCompat.getLongVersionCode(packageInfo),
        PackageInfoCompat.getLongVersionCode(packageInfo).toInt(),
        packageInfo.versionName,
        when (packageName) {
            PACKAGE_NAME_QQ -> {
                if ("GoogleMarket" in (packageInfo.applicationInfo.metaData["AppSetting_params"]
                        ?: "") as String) {
                    HostSpecies.QQ_Play
                } else HostSpecies.QQ
            }
            PACKAGE_NAME_TIM -> HostSpecies.TIM
            PACKAGE_NAME_QQ_LITE -> HostSpecies.QQ_Lite
            PACKAGE_NAME_QQ_INTERNATIONAL -> HostSpecies.QQ_International
            PACKAGE_NAME_SELF -> HostSpecies.QNotified
            else -> HostSpecies.Unknown
        },
    )
}

private fun getHostInfo(context: Context): PackageInfo {
    return try {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_META_DATA)
    } catch (e: Throwable) {
        Log.e("Utils", "Can not get PackageInfo!")
        throw AssertionError("Can not get PackageInfo!")
    }
}

fun isTim(): Boolean {
    return hostInfo.hostSpecies == HostSpecies.TIM
}

fun isPlayQQ(): Boolean {
    return hostInfo.hostSpecies == HostSpecies.QQ_Play
}

fun requireMinQQVersion(versionCode: Long): Boolean {
    return requireMinVersion(versionCode, HostSpecies.QQ)
}

fun requireMinPlayQQVersion(versionCode: Long): Boolean {
    return requireMinVersion(versionCode, HostSpecies.QQ_Play)
}

fun requireMinTimVersion(versionCode: Long): Boolean {
    return requireMinVersion(versionCode, HostSpecies.TIM)
}

fun requireMinVersion(versionCode: Long, hostSpecies: HostSpecies): Boolean {
    return hostInfo.hostSpecies == hostSpecies && hostInfo.versionCodeLong >= versionCode
}

fun requireMinVersion(
    QQVersionCode: Long = Long.MAX_VALUE,
    TimVersionCode: Long = Long.MAX_VALUE,
    PlayQQVersionCode: Long = Long.MAX_VALUE
): Boolean {
    return requireMinQQVersion(QQVersionCode) || requireMinTimVersion(TimVersionCode) || requireMinPlayQQVersion(PlayQQVersionCode)
}

data class HostInfoImpl(
    val application: Application,
    val packageName: String,
    val hostName: String,
    val versionCodeLong: Long,
    val versionCode32: Int,
    val versionName: String,
    val hostSpecies: HostSpecies
)

enum class HostSpecies {
    QQ,
    TIM,
    QQ_Play,
    QQ_Lite,
    QQ_International,
    QNotified,
    Unknown
}
