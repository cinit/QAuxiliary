/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the qwq233 Universal License
 * as published on https://github.com/qwq233/license; either
 * version 2 of the License, or any later version and our EULA as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the qwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package moe.zapic.hook

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.MessagingStyle
import androidx.core.app.Person
import androidx.core.content.LocusIdCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import cc.chenhe.qqnotifyevo.utils.NotifyChannel
import cc.chenhe.qqnotifyevo.utils.getChannelId
import cc.chenhe.qqnotifyevo.utils.getNotificationChannels
import cc.ioctl.util.HostInfo
import cc.ioctl.util.Reflex
import cc.ioctl.util.hookAfterIfEnabled
import cc.ioctl.util.hookBeforeIfEnabled
import com.github.kyuubiran.ezxhelper.utils.paramCount
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.ui.ResUtils
import io.github.qauxv.util.Log
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.hostInfo
import me.singleneuron.util.NonNTMessageStyleNotification
import moe.zapic.util.QQAvatarHelper
import moe.zapic.util.QQRecentContactInfo
import xyz.nextalone.util.clazz
import xyz.nextalone.util.get
import java.lang.reflect.Method
import java.util.WeakHashMap

@FunctionHookEntry
@UiItemAgentEntry
object MessagingStyleNotification : CommonSwitchFunctionHook(SyncUtils.PROC_ANY) {
    override val isAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    override val name = "MessagingStyle通知"
    override val description: String = "更加优雅的通知样式，致敬QQ Helper" + if (isAvailable) "" else " [系统不支持]"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.NOTIFICATION_CATEGORY

    private val notificationInfoMap = WeakHashMap<Any, Pair<Any, Intent>>()

    private val historyMessage: HashMap<String, MessagingStyle> = HashMap()
    val avatarHelper = QQAvatarHelper()

    @Throws(Exception::class)
    override fun initOnce(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannels()
        }

        if (!HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_63_BETA_11345)) {
            return NonNTMessageStyleNotification(this).hook()
        }
        val cNotificationFacade = "com.tencent.qqnt.notification.NotificationFacade".clazz!!
        val cAppRuntime = "mqq.app.AppRuntime".clazz!!
        val cCommonInfo = "com.tencent.qqnt.kernel.nativeinterface.NotificationCommonInfo".clazz!!
        val cRecentInfo = "com.tencent.qqnt.kernel.nativeinterface.RecentContactInfo".clazz!!
        val postNotification = Reflex.findSingleMethod(
            cNotificationFacade,
            null,
            false,
            Notification::class.java,
            Int::class.javaPrimitiveType
        )

        lateinit var buildNotification: Method
        lateinit var recentInfoBuilder: Method
        cNotificationFacade.declaredMethods.forEach {
            if (it.paramCount < 3 || it.parameterTypes[0] != cAppRuntime) return@forEach
            if (it.paramCount == 3 && it.parameterTypes[2] == cCommonInfo ||
                it.paramCount == 4 && it.parameterTypes[2] == cCommonInfo && it.parameterTypes[3] == cRecentInfo // since 9.1.0
            ) {
                buildNotification = it
            } else if (it.paramCount >= 3 && it.parameterTypes[1] == cRecentInfo && it.parameterTypes[2] == Boolean::class.java) {
                // paramCount=4 since 9.0.75
                recentInfoBuilder = it
            }
        }

        val context = hostInfo.application

        // 获取消息详细信息 使用WeakMap存储(K/V:简单信息->详细信息)
        hookAfterIfEnabled(recentInfoBuilder) { param ->
            val si = param.args[1]
            val it = param.result.get(Intent::class.java) ?: return@hookAfterIfEnabled
            notificationInfoMap[param.result] = Pair(si, it)
        }

        // 构建通知前获取(K/V:contentIntent->详细信息)
        hookBeforeIfEnabled(buildNotification) { param ->
            val el = param.args[1] as Any
            val pt = el.get(PendingIntent::class.java) ?: return@hookBeforeIfEnabled
            notificationInfoMap[pt] = notificationInfoMap[el]
            notificationInfoMap.remove(el)
        }
        hookBeforeIfEnabled(postNotification) { param ->
            val oldNotification = param.args[0] as Notification
            val pair = notificationInfoMap[oldNotification.contentIntent] ?: return@hookBeforeIfEnabled
            val info = QQRecentContactInfo(pair.first)
            notificationInfoMap.remove(oldNotification.contentIntent)
            if (info.chatType == 1 || info.chatType == 2) {
                val content =
                    info.abstractContent?.joinToString(separator = "") { it.get("content", String::class.java) ?: "[未解析消息]" } ?: return@hookBeforeIfEnabled
                val senderName = info.getUserName() ?: return@hookBeforeIfEnabled
                val senderUin = info.senderUin ?: return@hookBeforeIfEnabled
                val senderIcon: IconCompat
                val shortcut: ShortcutInfoCompat
                var groupName: String? = null
                var groupUin: Long? = null
                var groupIcon: IconCompat? = null
                val isSpecial = info.specialCareFlag == 1.toByte() || info.listOfSpecificEventTypeInfosInMsgBox.toString().contains("eventTypeInMsgBox=1006")
                // 特别关心判定改自9.0.17版本方法：
                // Lcom/tencent/mobileqq/app/notification/processor/basemessage/NTC2CFriendNotificationProcessor;->s(Lcom/tencent/qqnt/kernel/nativeinterface/RecentContactInfo;)Z

                // 好友消息
                when (info.chatType) {
                    1 -> {
                        senderIcon = IconCompat.createFromIcon(hostInfo.application, oldNotification.getLargeIcon())
                            ?: IconCompat.createWithBitmap(ResUtils.loadDrawableFromAsset("face.png", context).toBitmap())
                        shortcut = getShortcut("private_$senderUin", senderName, senderIcon, pair.second)
                    }

                    2 -> {
                        groupName = info.peerName ?: return@hookBeforeIfEnabled
                        groupUin = info.peerUin ?: return@hookBeforeIfEnabled

                        senderIcon = avatarHelper.getAvatar(senderUin.toString()) ?: IconCompat.createWithBitmap(
                            ResUtils.loadDrawableFromAsset("face.png", context).toBitmap()
                        )
                        groupIcon = IconCompat.createFromIcon(hostInfo.application, oldNotification.getLargeIcon())
                            ?: IconCompat.createWithBitmap(ResUtils.loadDrawableFromAsset("face.png", context).toBitmap())
                        shortcut = getShortcut("group_$groupUin", groupName, groupIcon, pair.second)
                    }

                    else -> {
                        // Impossible
                        throw Error("what the hell?")
                    }
                }
                val notification = createNotification(
                    content,
                    senderName,
                    senderUin,
                    senderIcon,
                    groupName,
                    groupUin,
                    groupIcon,
                    shortcut,
                    oldNotification,
                    isSpecial
                )
                param.args[0] = notification
            }
        }
        hookBeforeIfEnabled(
            Reflex.findMethod(
                "com.tencent.commonsdk.util.notification.QQNotificationManager".clazz!!,
                "cancelAll"
            )
        ) {
            historyMessage.clear()
        }
        return true
    }

    private fun getShortcut(id: String, name: String, icon: IconCompat, intent: Intent): ShortcutInfoCompat {
        val context = hostInfo.application

        val shortcut =
            ShortcutInfoCompat.Builder(context, id)
                .setLongLived(true)
                .setIntent(intent)
                .setShortLabel(name)
                .setIcon(icon)
                .setLocusId(LocusIdCompat(id))
                .build()
        ShortcutManagerCompat.pushDynamicShortcut(
            context,
            shortcut
        )
        return shortcut
    }

    private fun createNotification(
        content: String,
        senderName: String,
        senderUin: Long,
        senderIcon: IconCompat,
        groupName: String?,
        groupUin: Long?,
        groupIcon: IconCompat?,
        shortcut: ShortcutInfoCompat,
        oldNotification: Notification,
        isSpecial: Boolean
    ): Notification {
        val mainUin: Long
        val mainName: String?
        val mainIcon: IconCompat?
        val channelId: NotifyChannel

        if (groupUin != null) {
            mainUin = groupUin
            mainName = groupName ?: ""
            mainIcon = groupIcon
            channelId = NotifyChannel.GROUP
        } else {
            mainUin = senderUin
            mainName = senderName
            mainIcon = senderIcon
            channelId = if (isSpecial) NotifyChannel.FRIEND_SPECIAL else NotifyChannel.FRIEND
        }

        var messageStyle = historyMessage["$channelId+$mainUin"]

        if (messageStyle == null) {
            messageStyle = MessagingStyle(
                Person.Builder()
                    .setName(mainName)
                    .setIcon(mainIcon)
                    .setKey(mainUin.toString())
                    .build()
            )
            messageStyle.conversationTitle = mainName
            messageStyle.isGroupConversation = groupUin != null
            historyMessage["$channelId+$mainUin"] = messageStyle
        }
        var senderPerson: Person? = null
        if (groupUin != null) {
            senderPerson = Person.Builder()
                .setName(senderName)
                .setIcon(senderIcon)
                .setKey(senderUin.toString())
                .build()
        }
        val message = MessagingStyle.Message(content, oldNotification.`when`, senderPerson)
        messageStyle.addMessage(message)
        val builder = NotificationCompat.Builder(hostInfo.application, oldNotification)
            .setContentTitle(mainName)
            .setContentText(content)
            .setLargeIcon(null as Bitmap?)
            .setStyle(messageStyle)
            .setChannelId(getChannelId(channelId))

        builder.setShortcutInfo(shortcut)
        return builder.build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannels() {
        val notificationChannels: List<NotificationChannel> = getNotificationChannels()
        // don't create new channel group since the old channel ids are still used
        val notificationChannelGroup = NotificationChannelGroup("qq_evolution", "QQ通知进化 Plus")
        val notificationManager: NotificationManager = hostInfo.application.getSystemService(NotificationManager::class.java)
        if (notificationChannels.any { notificationChannel ->
                notificationManager.getNotificationChannel(notificationChannel.id) == null
            }) {
            Log.i("QNotifyEvolutionXp: Creating channels...")
            notificationManager.createNotificationChannelGroup(notificationChannelGroup)
            notificationManager.createNotificationChannels(notificationChannels)
        }
    }


}
