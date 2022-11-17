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

package cc.ioctl.hook.notification

import android.app.Activity
import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.view.WindowInsets
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.MessagingStyle.Message
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import cc.ioctl.util.Reflex
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Initiator._QQAppInterface
import io.github.qauxv.util.LicenseStatus
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.hostInfo
import xyz.nextalone.util.clazz
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.method
import kotlin.collections.set

@FunctionHookEntry
@UiItemAgentEntry
object MessageStyleNotification : CommonSwitchFunctionHook(SyncUtils.PROC_MAIN or SyncUtils.PROC_MSF) {

    override val name = "MessageStyle通知"
    override val description = "致敬QQ Helper"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.NOTIFICATION_CATEGORY

    private val numRegex = Regex("""\((\d+)\S{1,3}新消息\)?$""")
    private val senderName = Regex("""^.*?: """)
    private const val activityName = "com.tencent.mobileqq.activity.miniaio.MiniChatActivity"

    private val historyMessage: HashMap<Int, MutableList<Message>> = HashMap()
    private val personCache: HashMap<Int, Person> = HashMap()
    private var windowHeight = -1

    override fun initOnce(): Boolean {
        val buildNotification = Reflex.findSingleMethod(
            Initiator._MobileQQServiceExtend()!!,
            Notification::class.java,
            false,
            Intent::class.java,
            Bitmap::class.java,
            String::class.java,
            String::class.java,
            String::class.java
        )
        buildNotification.hookAfter(this) { param ->
            val intent = param.args[0] as Intent
            val context = hostInfo.application as Context
            val uin = intent.getStringExtra("uin")
                ?: intent.getStringExtra("param_uin")!!
            val isTroop = intent.getIntExtra(
                "uintype",
                intent.getIntExtra("param_uinType", -1)
            )
            if (isTroop != 0 && isTroop != 1 && isTroop != 3000) return@hookAfter
            val bitmap = param.args[1] as Bitmap?
            var title = param.args[3] as String
            var text = param.args[4] as String
            val oldNotification = param.result as Notification
            val notificationId =
                intent.getIntExtra("KEY_NOTIFY_ID_FROM_PROCESSOR", -113)
            val messageStyle = NotificationCompat.MessagingStyle(
                Person.Builder().setName("我").build()
            )
            historyMessage[notificationId]?.forEach(messageStyle::addMessage)

            title = numRegex.replace(title, "")

            val person: Person

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
            } else {
                val personInCache = personCache[notificationId]
                if (personInCache == null) {
                    val builder = Person.Builder()
                        .setName(title)
                        .setIcon(IconCompat.createWithBitmap(bitmap!!))
                    if (title.contains("[特别关心]")) {
                        builder.setImportant(true)
                    }
                    person = builder.build()
                    personCache[notificationId] = person
                } else {
                    person = personInCache
                }
            }

            val message = Message(text, oldNotification.`when`, person)
            messageStyle.addMessage(message)
            if (historyMessage[notificationId] == null) {
                historyMessage[notificationId] = ArrayList()
            }
            historyMessage[notificationId]?.add(message)

            //Log.d(historyMessage.toString())
            val builder = NotificationCompat.Builder(
                context,
                oldNotification
            )
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
                    uin.toInt(),
                    newIntent,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
                )

                val bubbleData = NotificationCompat.BubbleMetadata.Builder(
                    bubbleIntent,
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
                }
            }
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
                            XposedHelpers.findAndHookMethod(_QQAppInterface(),
                                "removeNotification",
                                object : XC_MethodHook() {
                                    override fun beforeHookedMethod(param: MethodHookParam) {
                                        if (id == Thread.currentThread().id) param.result =
                                            null
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
}
