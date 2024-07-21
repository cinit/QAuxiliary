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

//credit to https://github.com/singleNeuron/QQSpeciallyCare/blob/master/app/src/main/java/me/singleNeuron/QQSpeciallyCare/main.java

package me.singleneuron.hook

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.annotation.RequiresApi
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedBridge
import io.github.qauxv.util.xpcompat.XposedHelpers
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.hostInfo
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object SpecialCareNewChannel : CommonSwitchFunctionHook(SyncUtils.PROC_MAIN or SyncUtils.PROC_MSF) {

    override val isAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    override val name = "特别关心通知单独分组"
    override val description: String = "将特别关心发送的消息通知移动到单独的通知渠道" +
            if (isAvailable) "" else " 仅支持Android O及以上"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.NOTIFICATION_CATEGORY

    override fun initOnce() = throwOrTrue {
        XposedBridge.hookAllMethods(
                NotificationManager::class.java,
                "notify",
                object : XC_MethodHook() {
                    @RequiresApi(Build.VERSION_CODES.O)
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val notification: Notification = param.args[param.args.size - 1] as Notification
                            val title: String = notification.extras.get(Notification.EXTRA_TITLE).toString()
                            val text: String = notification.extras.get(Notification.EXTRA_TEXT).toString()
                            //if (BuildConfig.DEBUG) XposedBridge.log("QQ特别关心：$title$text")
                            if (title.contains("[特别关心]")) {
                                //Log.d("QQ特别关心：" + title + text + "true")
                                /*Log.d(
                                    "QQ特别关心Channel：" + XposedHelpers.getObjectField(
                                        notification,
                                        "mChannelId"
                                    ).toString()
                                )*/
                                val notificationChannel = NotificationChannel(
                                        "CHANNEL_ID_SPECIALLY_CARE",
                                        "特别关心",
                                        NotificationManager.IMPORTANCE_HIGH
                                )
                                hostInfo.application.getSystemService(NotificationManager::class.java)
                                        .createNotificationChannel(notificationChannel)
                                XposedHelpers.setObjectField(
                                        notification,
                                        "mChannelId",
                                        "CHANNEL_ID_SPECIALLY_CARE"
                                )
                                param.args[param.args.size - 1] = notification
                            }
                        } catch (e: Exception) {
                            traceError(e)
                        }
                    }
                })
    }
}
