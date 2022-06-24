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

package sakura.kooi.QAuxiliaryModified.hooks

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import cc.chenhe.qqnotifyevo.core.NevoNotificationProcessor
import cc.chenhe.qqnotifyevo.utils.getNotificationChannels
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.qauxv.SyncUtils
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.hostInfo
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object QNotifyEvolutionChannel  : CommonSwitchFunctionHook(SyncUtils.PROC_ANY) {
    override val isAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    override val name = "QQ通知进化"
    override val description: String = "使用 Xposed 实现的 QQ-Notify-Evolution" + if (isAvailable) "" else " [系统不支持]"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.NOTIFICATION_CATEGORY

    @SuppressLint("StaticFieldLeak")
    private lateinit var processor: NevoNotificationProcessor

    override fun initOnce() = throwOrTrue {
        processor = NevoNotificationProcessor(hostInfo.application)

        XposedBridge.hookAllMethods(
            NotificationManager::class.java,
            "notify",
            object : XC_MethodHook() {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val notification: Notification =
                            param.args[param.args.size - 1] as Notification

                        createNotificationChannels()
                        val decoratedNotification: Notification? = processor.resolveNotification(hostInfo.application, hostInfo.packageName, notification);
                        if (decoratedNotification != null) {
                            param.args[param.args.size - 1] = decoratedNotification
                        }
                    } catch (e: Exception) {
                        traceError(e)
                    }
                }
            })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannels() {
        val notificationChannels: List<NotificationChannel> = getNotificationChannels()
        val notificationChannelGroup = NotificationChannelGroup("qq_evolution", "QQ通知进化")
        val notificationManager: NotificationManager = hostInfo.application.getSystemService(NotificationManager::class.java);
        if (notificationChannels.any {
                    notificationChannel -> notificationManager.getNotificationChannel(notificationChannel.id) == null
            }) {
            Log.i("QNotifyEvolutionXp", "Creating channels...");
            notificationManager.createNotificationChannelGroup(notificationChannelGroup)
            notificationManager.createNotificationChannels(notificationChannels);
        }
    }
}
