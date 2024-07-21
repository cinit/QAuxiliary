/*
 * QNotified - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 dmca@ioctl.cc
 * https://github.com/ferredoxin/QNotified
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
 * <https://github.com/ferredoxin/QNotified/blob/master/LICENSE.md>.
 */
package me.singleneuron.hook

import com.microsoft.appcenter.analytics.channel.SessionTracker
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XC_MethodReplacement
import io.github.qauxv.util.xpcompat.XposedHelpers
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.util.Log
import java.text.SimpleDateFormat
import java.util.*

fun initAppCenterHook() {
    XposedHelpers.findAndHookMethod(
            SessionTracker::class.java,
            "hasSessionTimedOut",
            object : XC_MethodReplacement() {
                @Throws(Throwable::class)
                override fun replaceHookedMethod(methodHookParam: MethodHookParam): Any {
                    try {
                        val configManager = ConfigManager.getDefaultConfig()
                        val LAST_TRACE_DATA_CONFIG = "lastTraceDate"
                        val format = "yyyy-MM-dd"
                        val simpleDateFormat = SimpleDateFormat(format, Locale.getDefault())
                        val nowTime = simpleDateFormat.format(Date(System.currentTimeMillis()))
                        val oldTime = configManager.getString(LAST_TRACE_DATA_CONFIG)
                        if (oldTime != null && oldTime == nowTime) {
                            Log.d("Hooked hasSessionTimedOut: oldTime=$oldTime nowTime=$nowTime, ignore")
                            return false
                        }
                        configManager.putString(LAST_TRACE_DATA_CONFIG, nowTime)
                        configManager.save()
                        Log.d("Hooked hasSessionTimedOut: oldTime=$oldTime nowTime=$nowTime, continue")
                    } catch (e: Exception) {
                        Log.e(e)
                    }
                    return true
                }
            })
    XposedHelpers.findAndHookMethod(
            SessionTracker::class.java,
            "sendStartSessionIfNeeded",
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val configManager = ConfigManager.getDefaultConfig()
                        val LAST_TRACE_DATA_CONFIG2 = "lastTraceDate2"
                        val format = "yyyy-MM-dd"
                        val simpleDateFormat = SimpleDateFormat(format, Locale.getDefault())
                        val nowTime = simpleDateFormat.format(Date(System.currentTimeMillis()))
                        val oldTime = configManager.getString(LAST_TRACE_DATA_CONFIG2)
                        if (oldTime != null && oldTime == nowTime) {
                            Log.d("Hooked sendStartSessionIfNeeded: oldTime=$oldTime nowTime=$nowTime, ignore")
                            param.result = null
                            return
                        }
                        configManager.putString(LAST_TRACE_DATA_CONFIG2, nowTime)
                        configManager.save()
                        Log.d("Hooked sendStartSessionIfNeeded: oldTime=$oldTime nowTime=$nowTime, continue")
                    } catch (e: Exception) {
                        Log.e(e)
                    }
                }
            })
}
