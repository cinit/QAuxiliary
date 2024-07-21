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

package me.singleneuron.util

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.view.WindowInsets
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.MessagingStyle
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import cc.chenhe.qqnotifyevo.utils.NotifyChannel
import cc.chenhe.qqnotifyevo.utils.getChannelId
import cc.ioctl.util.Reflex
import cc.ioctl.util.hookAfterIfEnabled
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XC_MethodReplacement
import io.github.qauxv.util.xpcompat.XposedBridge
import io.github.qauxv.util.xpcompat.XposedHelpers
import io.github.qauxv.bridge.AppRuntimeHelper
import io.github.qauxv.bridge.ChatActivityFacade
import io.github.qauxv.bridge.SessionInfoImpl
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.LicenseStatus
import io.github.qauxv.util.hostInfo
import moe.zapic.hook.MessagingStyleNotification
import xyz.nextalone.util.clazz
import xyz.nextalone.util.method

// FIXME: current not working: channel not assigned or overwritten

class NonNTMessageStyleNotification(private val parent: MessagingStyleNotification) {
    private val numRegex = Regex("""\((\d+)\S{1,3}新消息\)?$""")
    private val senderName = Regex("""^.*?: """)
    private val activityName = "com.tencent.mobileqq.activity.miniaio.MiniChatActivity"

    private val historyMessage: HashMap<Int, MessagingStyle> = HashMap()
    private var windowHeight = -1

    var isEnabled: Boolean
        get() = this.parent.isEnabled
        set(_) {}

    @Throws(Exception::class)
    fun hook(): Boolean {
        val buildNotification = Reflex.findSingleMethod(
            Initiator._MobileQQServiceExtend()!!,
            android.app.Notification::class.java,
            false,
            Intent::class.java,
            Bitmap::class.java,
            String::class.java,
            String::class.java,
            String::class.java
        )
        val context = hostInfo.application
        parent.hookAfterIfEnabled(buildNotification) { param ->
            val intent = param.args[0] as Intent
            // TODO: 2022-07-14 uin may be null
            val uin = intent.getStringExtra("uin")
                ?: intent.getStringExtra("param_uin")!!
            val isTroop = intent.getIntExtra(
                "uintype",
                intent.getIntExtra("param_uinType", -1)
            )
            if (isTroop != 0 && isTroop != 1 && isTroop != 3000) return@hookAfterIfEnabled
            val bitmap = param.args[1] as Bitmap?
            var title = numRegex.replace(param.args[3] as String, "")
            var text = param.args[4] as String
            val key = when (isTroop) {
                1 -> "group_$uin"
                else -> "private_$uin"
            }
            val oldNotification = param.result as Notification
            val notificationId =
                intent.getIntExtra("KEY_NOTIFY_ID_FROM_PROCESSOR", -113)

            var channelId: NotifyChannel = NotifyChannel.FRIEND
            if (title.contains("[特别关心]")) {
                channelId = NotifyChannel.FRIEND_SPECIAL
                title = title.removePrefix("[特别关心]")
            }

            var messageStyle = historyMessage[notificationId]
            if (messageStyle == null) {
                messageStyle = MessagingStyle(
                    Person.Builder()
                        .setName(title)
                        .setIcon(IconCompat.createWithBitmap(bitmap!!))
                        .setImportant(channelId == NotifyChannel.FRIEND_SPECIAL)
                        .setKey(uin)
                        .build()
                )
                historyMessage[notificationId] = messageStyle
            }

            var person: Person? = null

            if (isTroop == 1) {
                val sender = senderName.find(text)?.value?.replace(": ", "")
                text = senderName.replace(text, "")
                val senderUin = intent.getStringExtra("param_fromuin")!!
                /*throwOrTrue {
                    val senderUin = intent.getStringExtra("param_fromuin")
                    bitmap = face.getBitmapFromCache(TYPE_USER,senderUin)
                }*/
                person = Person.Builder()
                    .setName(sender)
                    .setKey(senderUin)
                    .setIcon(parent.avatarHelper.getAvatar(senderUin))
                    .build()
                messageStyle.conversationTitle = title
                messageStyle.isGroupConversation = true
                channelId = NotifyChannel.GROUP
            }

            val message = MessagingStyle.Message(text, oldNotification.`when`, person)
            messageStyle.addMessage(message)

            //Log.d(historyMessage.toString())
            val builder = NotificationCompat.Builder(context, oldNotification)
                .setContentTitle(null)
                .setContentText(null)
                .setLargeIcon(null as Bitmap?)
                .setStyle(messageStyle)
            if (isTroop == 1) {
                builder.setLargeIcon(bitmap)
            }

            val remoteInput: RemoteInput = RemoteInput.Builder("KEY_REPLY").run {
                setLabel("回复")
                build()
            }
            val replyIntent = Intent(context, "com.tencent.mobileqq.servlet.NotificationClickReceiver".clazz).apply {
                putExtra("TO_UIN", uin)
                putExtra("UIN_TYPE", isTroop)
                putExtra("NOTIFY_ID", notificationId)
            }

            val replyPendingIntent: PendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    uin.toLong().toInt(),
                    replyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
                )
            val replyAction = NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_edit,
                "回复",
                replyPendingIntent
            )
                .addRemoteInput(remoteInput)
                .build()
            builder.addAction(replyAction)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val newIntent = intent.clone() as Intent
                newIntent.component = ComponentName(
                    context,
                    activityName.clazz!!
                )
                newIntent.putExtra("key_mini_from", 2)
                newIntent.putExtra("minaio_height_ration", 1f)
                newIntent.putExtra("minaio_scaled_ration", 1f)
                newIntent.putExtra(
                    "public_fragment_class",
                    "com.tencent.mobileqq.activity.miniaio.MiniChatFragment"
                )
                val bubbleIntent = PendingIntent.getActivity(
                    context,
                    uin.toLong().toInt(), // uin may be lager than Int.MAX_VALUE but small than 2^32-1
                    newIntent,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
                )

                val bubbleData = NotificationCompat.BubbleMetadata.Builder(
                    bubbleIntent,
                    // FIXME: 2022-06-24 handle NPE if bitmap is null
                    IconCompat.createWithBitmap(bitmap!!)
                )
                    .setDesiredHeight(600)
                    .build()

                val shortcut =
                    ShortcutInfoCompat.Builder(context, key)
                        .setIntent(intent)
                        .setLongLived(true)
                        .setShortLabel(title)
                        .setIcon(bubbleData.icon!!)
                        .build()

                ShortcutManagerCompat.pushDynamicShortcut(
                    context,
                    shortcut
                )

                builder.apply {
                    setShortcutInfo(shortcut)
                    bubbleMetadata = bubbleData
                    setChannelId(getChannelId(channelId))
                }
            }
            param.result = builder.build()
        }

        XposedHelpers.findAndHookMethod(
            "com.tencent.mobileqq.servlet.NotificationClickReceiver".clazz,
            "onReceive",
            Context::class.java,
            Intent::class.java,
            object : XC_MethodHook() {
                @SuppressLint("NotificationPermission")
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val ctx = param.args[0] as Context
                    val intent = param.args[1] as Intent
                    val uinType = intent.getIntExtra("UIN_TYPE", -1)
                    if (uinType != -1) {
                        param.result = null
                        val uin = intent.getStringExtra("TO_UIN") ?: return
                        val result = RemoteInput.getResultsFromIntent(intent)?.getCharSequence("KEY_REPLY") ?: return
                        val selfUin = AppRuntimeHelper.getAccount()
                        // send message
                        ChatActivityFacade.sendMessage(
                            AppRuntimeHelper.getQQAppInterface(),
                            hostInfo.application,
                            SessionInfoImpl.createSessionInfo(uin, uinType),
                            result.toString()
                        )

                        // update exist notification
                        val notifyId = intent.getIntExtra("NOTIFY_ID", -113)
                        val notificationManager = ctx.getSystemService(NotificationManager::class.java)
                        val origin = notificationManager.activeNotifications.find {
                            it.id == notifyId
                        } ?: return
                        val msg = historyMessage[notifyId] ?: return
                        val sendMsg: MessagingStyle.Message = MessagingStyle.Message(
                            result,
                            System.currentTimeMillis(),
                            Person.Builder().setName("我").setIcon(parent.avatarHelper.getAvatar(selfUin)).setKey(selfUin).build()
                        )
                        msg.addMessage(sendMsg)
                        historyMessage[notifyId] = msg
                        val newNotification =
                            NotificationCompat.Builder(ctx, origin.notification)
                                .setStyle(msg)
                                .setSilent(true)
                                .build()
                        notificationManager.notify(origin.id, newNotification)
                    }
                }
            }
        )

        XposedHelpers.findAndHookMethod(
            "com.tencent.commonsdk.util.notification.QQNotificationManager".clazz,
            "cancel", String::class.java, Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!isEnabled or LicenseStatus.sDisableCommonHooks) return
                    if (param.args[0] as String != "MobileQQServiceWrapper.showMsgNotification") {
                        historyMessage.remove(param.args[1] as Int)
                    } else {
                        // stop QQ cancel the old message to prevent message flashing in notification area
                        param.result = null
                    }
                }
            }
        )
        XposedHelpers.findAndHookMethod(
            "com.tencent.commonsdk.util.notification.QQNotificationManager".clazz,
            "cancelAll",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!isEnabled or LicenseStatus.sDisableCommonHooks) return
                    historyMessage.clear()
                }
            }
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Fix the height of launch from bubble
            XposedHelpers.findAndHookMethod(
                "com.tencent.widget.immersive.ImmersiveUtils".clazz,
                "getStatusBarHeight",
                Context::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!isEnabled or LicenseStatus.sDisableCommonHooks) return
                        if ((param.args[0] as? Activity)?.isLaunchedFromBubble == true)
                            param.result = 0
                    }
                })
            // Don't clear notification when launching from bubble
            XposedHelpers.findAndHookMethod("com.tencent.mobileqq.app.lifecycle.BaseActivityLifecycleCallbackImpl".clazz,
                "doOnWindowFocusChanged",
                Activity::class.java,
                Boolean::class.javaPrimitiveType,
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        val id = Thread.currentThread().id
                        val unhook = if (isEnabled && !LicenseStatus.sDisableCommonHooks &&
                            param.args[1] as Boolean &&
                            (param.args[0] as Activity).isLaunchedFromBubble
                        )
                            XposedHelpers.findAndHookMethod("com.tencent.mobileqq.app.QQAppInterface".clazz,
                                "removeNotification",
                                object : XC_MethodHook() {
                                    override fun beforeHookedMethod(param: MethodHookParam) {
                                        if (id == Thread.currentThread().id) param.result = null
                                    }
                                }
                            ) else null
                        val result = XposedBridge.invokeOriginalMethod(
                            param.method,
                            param.thisObject,
                            param.args
                        )
                        unhook?.unhook()
                        return result
                    }
                }
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            XposedBridge.hookMethod(activityName.clazz!!.method("doOnStart")!!, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    val activity = param!!.thisObject as Activity
                    val rootView = activity.window.decorView
                    windowHeight = activity.window.attributes.height
                    rootView.setOnApplyWindowInsetsListener { _, insets ->
                        val attr = activity.window.attributes
                        if (insets.isVisible(WindowInsets.Type.ime()) && attr.height != windowHeight) {
                            attr.height = windowHeight
                            activity.window.attributes = attr
                        }
                        insets
                    }
                }
            })
        }
        return true
    }
}
