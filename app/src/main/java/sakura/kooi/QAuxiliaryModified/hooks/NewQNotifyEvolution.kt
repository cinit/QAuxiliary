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

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.view.WindowInsets
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import cc.chenhe.qqnotifyevo.utils.NotifyChannel
import cc.chenhe.qqnotifyevo.utils.getChannelId
import cc.chenhe.qqnotifyevo.utils.getNotificationChannels
import cc.ioctl.util.Reflex
import cc.ioctl.util.hookAfterIfEnabled
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.qauxv.SyncUtils
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.LicenseStatus
import io.github.qauxv.util.hostInfo
import xyz.nextalone.util.clazz
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.method

// FIXME: current not working: channel not assigned or overwritten

@FunctionHookEntry
@UiItemAgentEntry
object NewQNotifyEvolution : CommonSwitchFunctionHook(SyncUtils.PROC_ANY) {
    override val isAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    override val name = "QQ通知进化Plus"
    override val description: String = "更加优雅的通知样式w" + if (isAvailable) "" else " [系统不支持]"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.NOTIFICATION_CATEGORY

    private val numRegex = Regex("""\((\d+)\S{1,3}新消息\)?$""")
    private val senderName = Regex("""^.*?: """)
    private const val activityName = "com.tencent.mobileqq.activity.miniaio.MiniChatActivity"

    private val historyMessage: HashMap<Int, MutableList<NotificationCompat.MessagingStyle.Message>> = HashMap()
    private val personCache: HashMap<Int, Person> = HashMap()
    var windowHeight = -1

    @Throws(Exception::class)
    override fun initOnce(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannels()
        }

        val buildNotification = Reflex.findSingleMethod(
            "com.tencent.mobileqq.service.MobileQQServiceExtend".clazz!!,
            android.app.Notification::class.java,
            false,
            Intent::class.java,
            Bitmap::class.java,
            String::class.java,
            String::class.java,
            String::class.java
        )
        hookAfterIfEnabled(buildNotification) { param ->
            val intent = param.args[0] as Intent
            val context = hostInfo.application as Context
            val uin = intent.getStringExtra("uin")
                ?: intent.getStringExtra("param_uin")!!
            val isTroop = intent.getIntExtra(
                "uintype",
                intent.getIntExtra("param_uinType", -1)
            )
            if (isTroop != 0 && isTroop != 1 && isTroop != 3000) return@hookAfterIfEnabled
            val bitmap = param.args[1] as Bitmap?
            var title = param.args[3] as String
            var text = param.args[4] as String
            val oldNotification = param.result as Notification
            val notificationId =
                intent.getIntExtra("KEY_NOTIFY_ID_FROM_PROCESSOR", -113)
            val messageStyle = NotificationCompat.MessagingStyle(
                Person.Builder().setName("我").build()
            )
            historyMessage[notificationId]?.forEach { it ->
                messageStyle.addMessage(it)
            }

            title = numRegex.replace(title, "")

            val person: Person
            var channelId: NotifyChannel

            if (isTroop == 1) {
                val sender = senderName.find(text)?.value?.replace(": ", "")
                text = senderName.replace(text, "")
                /*throwOrTrue {
                    val senderUin = intent.getStringExtra("param_fromuin")
                    bitmap = face.getBitmapFromCache(TYPE_USER,senderUin)
                }*/
                person = Person.Builder()
                    .setName(sender)
                    //.setIcon(IconCompat.createWithBitmap(bitmap))
                    .build()
                messageStyle.conversationTitle = title
                messageStyle.isGroupConversation = true
                channelId = NotifyChannel.GROUP
            } else {
                channelId = NotifyChannel.FRIEND
                val personInCache = personCache[notificationId]
                if (personInCache == null) {
                    val builder = Person.Builder()
                        .setName(title)
                        // FIXME: 2022-06-24 handle NPE if bitmap is null
                        .setIcon(IconCompat.createWithBitmap(bitmap!!))
                    if (title.contains("[特别关心]")) {
                        builder.setImportant(true)
                        channelId = NotifyChannel.FRIEND_SPECIAL
                        title = title.removePrefix("[特别关心]")
                    }
                    person = builder.build()
                    personCache[notificationId] = person
                } else {
                    person = personInCache
                }
            }

            val message = NotificationCompat.MessagingStyle.Message(text, oldNotification.`when`, person)
            messageStyle.addMessage(message)
            if (historyMessage[notificationId] == null) {
                historyMessage[notificationId] = ArrayList()
            }
            historyMessage[notificationId]?.add(message)

            //Log.d(historyMessage.toString())
            val builder = NotificationCompat.Builder(context, oldNotification)
                .setContentTitle(null)
                .setContentText(null)
                .setLargeIcon(null)
                .setStyle(messageStyle)
            if (isTroop == 1) {
                builder.setLargeIcon(bitmap)
            }
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
                    person.icon ?: IconCompat.createWithBitmap(bitmap!!)
                )
                    .setDesiredHeight(600)
                    .build()

                val shortcut =
                    ShortcutInfoCompat.Builder(context, notificationId.toString())
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

            Log.i("QNotifyEvolutionXp", "send as channel " + channelId.name);
            param.result = builder.build()
        }

        XposedHelpers.findAndHookMethod(
            "com.tencent.commonsdk.util.notification.QQNotificationManager".clazz,
            "cancel", String::class.java, Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!isEnabled or LicenseStatus.sDisableCommonHooks) return
                    if (param.args[0] as String != "MobileQQServiceWrapper.showMsgNotification") {
                        historyMessage.remove(param.args[1] as Int)
                        personCache.remove(param.args[1] as Int)
                        ShortcutManagerCompat.removeDynamicShortcuts(hostInfo.application, arrayListOf((param.args[1] as Int).toString()))
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
                    personCache.clear()
                    ShortcutManagerCompat.removeAllDynamicShortcuts(hostInfo.application)
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
            activityName.clazz!!.method("doOnStart")!!.hookAfter(this) {
                val activity = it.thisObject as Activity
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
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannels() {
        val notificationChannels: List<NotificationChannel> = getNotificationChannels()
        // don't create new channel group since the old channel ids are still used
        val notificationChannelGroup = NotificationChannelGroup("qq_evolution", "QQ通知进化 Plus")
        val notificationManager: NotificationManager = hostInfo.application.getSystemService(NotificationManager::class.java);
        if (notificationChannels.any { notificationChannel ->
                notificationManager.getNotificationChannel(notificationChannel.id) == null
            }) {
            Log.i("QNotifyEvolutionXp", "Creating channels...");
            notificationManager.createNotificationChannelGroup(notificationChannelGroup)
            notificationManager.createNotificationChannels(notificationChannels);
        }
    }
}
