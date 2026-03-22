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

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.view.View
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
import io.github.qauxv.activity.SettingsUiFragmentHostActivity
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.ui.ResUtils
import io.github.qauxv.util.Log
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.hostInfo
import kotlinx.coroutines.flow.MutableStateFlow
import me.singleneuron.util.NonNTMessageStyleNotification
import moe.zapic.util.QQAvatarHelper
import moe.zapic.util.QQRecentContactInfo
import moe.zapic.fragment.MessagingStyleNotificationConfigFragment
import xyz.nextalone.util.clazz
import xyz.nextalone.util.get
import java.lang.reflect.Method
import java.util.WeakHashMap

@FunctionHookEntry
@UiItemAgentEntry
object MessagingStyleNotification : CommonConfigFunctionHook(targetProc = SyncUtils.PROC_ANY) {
    private data class ConversationTarget(
        val mainUin: Long,
        val mainName: String,
        val mainIcon: IconCompat?,
        val channelId: NotifyChannel,
        val isGroupConversation: Boolean
    ) {
        val historyKey: String
            get() = "$channelId+$mainUin"
    }

    override val isAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    override val name = "MessagingStyle通知"
    override val description: String = "更加优雅的通知样式，致敬QQ Helper" + if (isAvailable) "" else " [系统不支持]"
    override val extraSearchKeywords: Array<String> =
        arrayOf("会话通知", "通知子渠道", "通知气泡", "Bubble")

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.NOTIFICATION_CATEGORY
    private val stateFlowDelegate = lazy(LazyThreadSafetyMode.NONE) { MutableStateFlow(stateText()) }
    override val valueState: MutableStateFlow<String>
        get() = stateFlowDelegate.value
    override val onUiItemClickListener: (IUiItemAgent, Activity, View) -> Unit = { _, activity, _ ->
        SettingsUiFragmentHostActivity.startFragmentWithContext(activity, MessagingStyleNotificationConfigFragment::class.java)
    }

    private val notificationInfoMap = WeakHashMap<Any, Pair<Any, Intent>>()

    private val historyMessage: HashMap<String, MessagingStyle> = HashMap()
    val avatarHelper = QQAvatarHelper()

    var disableConversationSubChannel: Boolean
        get() = MessagingStyleNotificationConfig.disableConversationSubChannel
        set(value) {
            MessagingStyleNotificationConfig.disableConversationSubChannel = value
        }

    var disableBubble: Boolean
        get() = MessagingStyleNotificationConfig.disableBubble
        set(value) {
            MessagingStyleNotificationConfig.disableBubble = value
        }

    private fun stateText(): String = if (isEnabled) "已开启" else "禁用"

    override var isEnabled: Boolean
        get() = super.isEnabled
        set(value) {
            super.isEnabled = value
            if (stateFlowDelegate.isInitialized()) {
                stateFlowDelegate.value.value = stateText()
            }
        }

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
        val (postNotification, postNotificationIndex) =
            runCatching {
                Reflex.findSingleMethod(
                    cNotificationFacade,
                    null,
                    false,
                    Notification::class.java,
                    Int::class.javaPrimitiveType
                ) to 0
            }.getOrNull() ?: runCatching {
                Reflex.findSingleMethod(
                    cNotificationFacade,
                    null,
                    false,
                    String::class.java, Notification::class.java, Int::class.javaPrimitiveType
                ) to 1
            }.getOrThrow()

        lateinit var buildNotification: Method
        lateinit var recentInfoBuilder: Method
        cNotificationFacade.declaredMethods.forEach {
            if (it.paramCount < 3 || it.parameterTypes[0] != cAppRuntime) return@forEach
            if (it.paramCount == 3 && it.parameterTypes[2] == cCommonInfo ||
                it.paramCount == 4 && it.parameterTypes[2] == cCommonInfo && it.parameterTypes[3] == cRecentInfo || // since 9.1.0
                it.paramCount == 5 && it.parameterTypes[2] == cCommonInfo && it.parameterTypes[3] == cRecentInfo && it.parameterTypes[4] == Boolean::class.javaPrimitiveType // since 9.2.20
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
            val oldNotification = param.args[postNotificationIndex] as Notification
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
                param.args[postNotificationIndex] = notification
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
        return buildShortcut(id, name, icon, intent, withLocusId = true)
    }

    internal fun buildShortcut(
        id: String,
        name: String,
        icon: IconCompat,
        intent: Intent,
        withLocusId: Boolean
    ): ShortcutInfoCompat {
        val context = hostInfo.application
        val shortcutBuilder = ShortcutInfoCompat.Builder(context, id)
            .setLongLived(true)
            .setIntent(intent)
            .setShortLabel(name)
            .setIcon(icon)
        if (withLocusId) {
            shortcutBuilder.setLocusId(LocusIdCompat(id))
        }
        val shortcut = shortcutBuilder.build()
        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
        return shortcut
    }

    @RequiresApi(Build.VERSION_CODES.O)
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
        val conversationTarget = resolveConversationTarget(
            senderName,
            senderUin,
            senderIcon,
            groupName,
            groupUin,
            groupIcon,
            isSpecial
        )
        val messageStyle = getOrCreateMessageStyle(conversationTarget)
        val message = createMessage(content, senderName, senderUin, senderIcon, oldNotification)
        messageStyle.addMessage(message)
        val builder = createNotificationBuilder(
            oldNotification,
            conversationTarget.mainName,
            content,
            messageStyle
        )
        if (!disableConversationSubChannel) {
            applyConversationChannel(builder, conversationTarget, shortcut)
        } else {
            builder.setChannelId(getChannelId(conversationTarget.channelId))
        }
        applyBubbleMetadataIfNeeded(builder, shortcut)
        builder.setShortcutInfo(shortcut)
        return builder.build()
    }

    private fun resolveConversationTarget(
        senderName: String,
        senderUin: Long,
        senderIcon: IconCompat,
        groupName: String?,
        groupUin: Long?,
        groupIcon: IconCompat?,
        isSpecial: Boolean
    ): ConversationTarget {
        if (groupUin != null) {
            return ConversationTarget(
                mainUin = groupUin,
                mainName = groupName ?: "",
                mainIcon = groupIcon,
                channelId = NotifyChannel.GROUP,
                isGroupConversation = true
            )
        }
        return ConversationTarget(
            mainUin = senderUin,
            mainName = senderName,
            mainIcon = senderIcon,
            channelId = if (isSpecial) NotifyChannel.FRIEND_SPECIAL else NotifyChannel.FRIEND,
            isGroupConversation = false
        )
    }

    private fun getOrCreateMessageStyle(conversationTarget: ConversationTarget): MessagingStyle {
        historyMessage[conversationTarget.historyKey]?.let { return it }
        return MessagingStyle(
            Person.Builder()
                .setName(conversationTarget.mainName)
                .setIcon(conversationTarget.mainIcon)
                .setKey(conversationTarget.mainUin.toString())
                .build()
        ).also { messageStyle ->
            messageStyle.conversationTitle = conversationTarget.mainName
            messageStyle.isGroupConversation = conversationTarget.isGroupConversation
            historyMessage[conversationTarget.historyKey] = messageStyle
        }
    }

    private fun createMessage(
        content: String,
        senderName: String,
        senderUin: Long,
        senderIcon: IconCompat,
        oldNotification: Notification
    ): MessagingStyle.Message {
        val senderPerson = Person.Builder()
            .setName(senderName)
            .setIcon(senderIcon)
            .setKey(senderUin.toString())
            .build()
        return MessagingStyle.Message(content, oldNotification.`when`, senderPerson)
    }

    private fun createNotificationBuilder(
        oldNotification: Notification,
        mainName: String,
        content: String,
        messageStyle: MessagingStyle
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(hostInfo.application, oldNotification)
            .setContentTitle(mainName)
            .setContentText(content)
            .setLargeIcon(null as Bitmap?)
            .setStyle(messageStyle)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun applyConversationChannel(
        builder: NotificationCompat.Builder,
        conversationTarget: ConversationTarget,
        shortcut: ShortcutInfoCompat
    ) {
        val notificationChannel = ensureConversationNotificationChannel(conversationTarget, shortcut)
        builder.setChannelId(notificationChannel.id)
    }

    private fun applyBubbleMetadataIfNeeded(
        builder: NotificationCompat.Builder,
        shortcut: ShortcutInfoCompat
    ) {
        if (disableBubble || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }
        builder.setBubbleMetadata(
            NotificationCompat.BubbleMetadata.Builder(shortcut.id)
                .build()
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ensureConversationNotificationChannel(
        conversationTarget: ConversationTarget,
        shortcut: ShortcutInfoCompat
    ): NotificationChannel {
        val notificationChannel = NotificationChannel(
            conversationTarget.mainUin.toString(),
            conversationTarget.mainName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            group = "qq_evolution"
            description = "来自 ${conversationTarget.mainName} 的消息"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setConversationId(getChannelId(conversationTarget.channelId), shortcut.id)
            }
        }
        val notificationManager = hostInfo.application.getSystemService(NotificationManager::class.java)
        if (notificationManager.getNotificationChannel(notificationChannel.id) == null) {
            notificationManager.createNotificationChannel(notificationChannel)
        }
        return notificationChannel
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
