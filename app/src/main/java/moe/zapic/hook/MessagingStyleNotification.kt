/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
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

package moe.zapic.hook

import android.annotation.SuppressLint
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
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.util.LruCache
import android.view.WindowInsets
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.MessagingStyle
import androidx.core.app.Person
import androidx.core.app.RemoteInput
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
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.bridge.AppRuntimeHelper
import io.github.qauxv.bridge.ChatActivityFacade
import io.github.qauxv.bridge.FaceImpl
import io.github.qauxv.bridge.SessionInfoImpl
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.ui.ResUtils
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.LicenseStatus
import io.github.qauxv.util.Log
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.hostInfo
import moe.zapic.util.QQAvatarHelper
import moe.zapic.util.QQRecentContactInfo
import xyz.nextalone.util.clazz
import xyz.nextalone.util.get
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.method
import java.io.File
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
    private val avatarHelper = QQAvatarHelper()

    @Throws(Exception::class)
    override fun initOnce(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannels()
        }

        if (!HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_63)) {
            return hookNonNt()
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
            if (it.parameterTypes.size != 3) return@forEach
            if (it.parameterTypes[0] != cAppRuntime) return@forEach
            if (it.parameterTypes[2] == cCommonInfo) {
                buildNotification = it
            } else if (it.parameterTypes[1] == cRecentInfo){
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
                val content = info.abstractContent?.joinToString(separator = "") { it.get("content", String::class.java) ?: "[未解析消息]" } ?: return@hookBeforeIfEnabled
                val senderName = info.getUserName() ?: return@hookBeforeIfEnabled
                val senderUin = info.senderUin ?: return@hookBeforeIfEnabled
                val senderIcon: IconCompat
                val shortcut: ShortcutInfoCompat
                var groupName: String? = null
                var groupUin: Long? = null
                var groupIcon: IconCompat? = null

                // 好友消息
                if (info.chatType == 1) {
                    senderIcon = IconCompat.createFromIcon(hostInfo.application, oldNotification.getLargeIcon()) ?: IconCompat.createWithBitmap(ResUtils.loadDrawableFromAsset("face.png", context).toBitmap())
                    shortcut = getShortcut("private_$senderUin", senderName, senderIcon, pair.second)
                } else if (info.chatType == 2) {
                    groupName = info.peerName ?: return@hookBeforeIfEnabled
                    groupUin = info.peerUin ?: return@hookBeforeIfEnabled

                    senderIcon = avatarHelper.getAvatar(senderUin.toString()) ?: IconCompat.createWithBitmap(ResUtils.loadDrawableFromAsset("face.png", context).toBitmap())
                    groupIcon = IconCompat.createFromIcon(hostInfo.application, oldNotification.getLargeIcon()) ?: IconCompat.createWithBitmap(ResUtils.loadDrawableFromAsset("face.png", context).toBitmap())
                    shortcut = getShortcut("group_$groupUin", groupName, groupIcon, pair.second)
                } else {
                    // Impossible
                    throw Error("what the hell?")
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
                    oldNotification
                )
                param.args[0] = notification
            }
        }

        XposedHelpers.findAndHookMethod(
            "com.tencent.commonsdk.util.notification.QQNotificationManager".clazz,
            "cancelAll",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!isEnabled || LicenseStatus.sDisableCommonHooks) return
                    historyMessage.clear()
                }
            }
        )
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
        oldNotification: Notification
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
            channelId = NotifyChannel.FRIEND
        }

        var messageStyle = historyMessage["$mainUin"]

        if (messageStyle == null) {
            messageStyle = MessagingStyle(Person.Builder()
                .setName(mainName)
                .setIcon(mainIcon)
                .setKey(mainUin.toString())
                .build())
            messageStyle.conversationTitle = senderName
            historyMessage["$mainUin"] = messageStyle
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
            .setLargeIcon(null)
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

    private fun hookNonNt() : Boolean {
        val numRegex = Regex("""\((\d+)\S{1,3}新消息\)?$""")
        val senderName = Regex("""^.*?: """)
        val activityName = "com.tencent.mobileqq.activity.miniaio.MiniChatActivity"
        val avatarCachePath: String?

        val historyMessage: HashMap<Int, MessagingStyle> = HashMap()
        val avatarCache: LruCache<String, IconCompat> = LruCache(50)
        var windowHeight: Int

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
        avatarCachePath = File(
            context.getExternalFilesDir(null)?.parent,
            "Tencent/MobileQQ/head/_hd"
        ).absolutePath

        hookAfterIfEnabled(buildNotification) { param ->
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
                    .setIcon(getAvatar(senderUin, avatarCachePath, avatarCache))
                    .build()
                messageStyle.conversationTitle = title
                messageStyle.isGroupConversation = true
                channelId = NotifyChannel.GROUP
            }

            val message = MessagingStyle.Message(text, oldNotification.`when`, person)
            messageStyle.addMessage(message)

            val builder = NotificationCompat.Builder(context, oldNotification)
                .setContentTitle(null)
                .setContentText(null)
                .setLargeIcon(null)
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
            Log.i("QNotifyEvolutionXp: send as channel " + channelId.name)
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
                        val uin = intent.getStringExtra("TO_UIN") ?:  return
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
                        val msg = historyMessage[notifyId]?: return
                        val sendMsg: MessagingStyle.Message = MessagingStyle.Message(
                            result,
                            System.currentTimeMillis(),
                            Person.Builder().setName("我").setIcon(getAvatar(selfUin, avatarCachePath, avatarCache)).setKey(selfUin).build()
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

    // Non NT uses below
    private fun toMD5(uin: String): String {
        val toMD5Method = "com.tencent.qphone.base.util.MD5".clazz!!.getMethod("toMD5", String::class.java)
        return toMD5Method.invoke(null, uin) as String
    }

    private fun getCroppedBitmap(bm: Bitmap): Bitmap {
        var w: Int = bm.width
        var h: Int = bm.height

        val radius = if (w < h) w else h
        w = radius
        h = radius

        val bmOut = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmOut)

        val paint = Paint()
        paint.isAntiAlias = true
        paint.color = -0xbdbdbe

        val rect = Rect(0, 0, w, h)
        val rectF = RectF(rect)

        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(
            rectF.left + rectF.width() / 2, rectF.top + rectF.height() / 2,
            (radius / 2).toFloat(), paint
        )

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bm, rect, rect, paint)

        return bmOut
    }

    private fun getAvatarFromFile(uin: String, avatarCachePath: String): Bitmap? {
        val md5 = toMD5(toMD5(toMD5(uin) + uin) + uin)
        val file = File(avatarCachePath, "$md5.jpg_")
        if (file.isFile) {
            return getCroppedBitmap(BitmapFactory.decodeFile(file.absolutePath))
        }
        return null
    }

    private fun getAvatar(uin: String, avatarCachePath: String, avatarCache: LruCache<String, IconCompat>): IconCompat? {
        if (avatarCache[uin] == null) {
            var cached = getAvatarFromFile(uin,avatarCachePath)
            if (cached == null) {
                val face = FaceImpl.getInstance()
                cached = face.getBitmapFromCache(1, uin)
                if (cached == null) {
                    face.requestDecodeFace(1, uin)
                    return null
                }
            }
            avatarCache.put(uin, IconCompat.createWithBitmap(cached))
        }
        return avatarCache[uin]
    }
}
