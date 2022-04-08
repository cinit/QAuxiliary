package cc.chenhe.qqnotifyevo.core

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import cc.chenhe.qqnotifyevo.utils.*
import io.github.qauxv.activity.ConfigV2Activity
import java.util.regex.Matcher
import java.util.regex.Pattern

abstract class NotificationProcessor(context: Context) {

    companion object {
        lateinit var res_inject_ic_notify_qq: Drawable
        lateinit var res_inject_ic_notify_qzone: Drawable

        private const val TAG = "QNotifyEvoXP"

        /**
         * 用于在优化后的通知中保留原始来源标记。通过 [Notification.extras] 提取。
         *
         * 值为 [String] 类型，关联于 [Tag].
         */
        const val NOTIFICATION_EXTRA_TAG = "qqevo.tag"

        private const val CONVERSATION_NAME_QZONE = "QZone"
        private const val CONVERSATION_NAME_QZONE_SPECIAL = "QZoneSpecial" // 特别关心空间动态推送


        fun getTagFromPackageName(packageName: String): Tag {
            return when (packageName) {
                "com.tencent.mobileqq" -> Tag.QQ
                "com.tencent.tim" -> Tag.TIM
                "com.tencent.qqlite" -> Tag.QQ_LITE
                "com.tencent.minihd.qq" -> Tag.QQ_HD
                else -> Tag.UNKNOWN
            }
        }

        // 群聊消息
        // title: 群名 | 群名 (x条新消息)
        // ticker: 昵称(群名):消息内容
        // text: 昵称: 消息内容 //特别关注前缀：[有关注的内容]
        // QQHD v5.8.8.3445 中群里特别关心前缀为 特别关注。

        /**
         * 匹配群聊消息 Ticker.
         *
         * Group: 1昵称, 2群名, 3消息内容
         *
         * 限制：昵称不能包含英文括号 `()`.
         */
        @VisibleForTesting
        val groupMsgPattern: Pattern = Pattern.compile("^(.+?)\\((.+)\\):([\\s\\S]+)$")

        /**
         * 匹配群聊消息 Content.
         *
         * Group: 1[有关注的内容
         *
         * QQHD v5.8.8.3445 中群里特别关心前缀为 特别关注。
         */
        @VisibleForTesting
        val groupMsgContentPattern: Pattern = Pattern.compile("^(\\[(?:有关注的内容|特别关注)])?[\\s\\S]+")

        // 私聊消息
        // title: 昵称 | 昵称 (x条新消息) //特别关心前缀：[特别关心]
        // ticker: 昵称: 消息内容
        // text: 消息内容

        /**
         * 匹配私聊消息 Ticker.
         *
         * Group: 1昵称, 2消息内容
         */
        @VisibleForTesting
        val msgPattern: Pattern = Pattern.compile("^(.+?): ([\\s\\S]+)$")

        /**
         * 匹配私聊消息 Title.
         *
         * Group: 1\[特别关心\], 2新消息数目
         */
        @VisibleForTesting
        val msgTitlePattern: Pattern = Pattern.compile("^(\\[特别关心])?.*?(?: \\((\\d+)条新消息\\))?$")

        // 关联QQ消息
        // title:
        //      - 只有一条消息: 关联QQ号
        //      - 一人发来多条消息: 关联QQ号 ({x}条新消息)
        //      - 多人发来消息: QQ
        // ticker:  关联QQ号-{关联昵称} {发送者昵称}:{消息内容}
        // content:
        //      - 一人发来消息: {消息内容}
        //      - 多人发来消息: 有 {x} 个联系人给你发过来{y}条新消息

        /**
         * 匹配关联 QQ 消息 ticker.
         *
         * Group: 1关联账号昵称, 2消息内容
         */
        @VisibleForTesting
        val bindingQQMsgTickerPattern: Pattern = Pattern.compile("^关联QQ号-(.+?):([\\s\\S]+)$")

        /**
         * 匹配关联 QQ 消息 content. 用于提取未读消息个数。
         *
         * Group: 1未读消息个数
         */
        @VisibleForTesting
        val bindingQQMsgContextPattern: Pattern =
            Pattern.compile("^有 (?:\\d+) 个联系人给你发过来(\\d+)条新消息$")

        /**
         * 匹配关联 QQ 消息 title. 用于提取未读消息个数。
         *
         * Group: 1未读消息个数
         */
        @VisibleForTesting
        val bindingQQMsgTitlePattern: Pattern = Pattern.compile("^关联QQ号 \\((\\d+)条新消息\\)$")

        // Q空间动态
        // title(与我相关): QQ空间动态(共x条未读); (特别关心): QQ空间动态
        // ticker(与我相关): 详情（例如xxx评论了你）; (特别关心): 【特别关心】昵称：内容
        // text: 与 ticker 相同
        // 注意：与我相关动态、特别关心动态是两个独立的通知，不会互相覆盖。

        /**
         * 匹配 QQ 空间 Title.
         *
         * Group: 1新消息数目
         */
        @VisibleForTesting
        val qzonePattern: Pattern = Pattern.compile("^QQ空间动态(?:\\(共(\\d+)条未读\\))?$")

        // 隐藏消息详情
        // title: QQ
        // ticker: 你收到了x条新消息
        // text: 与 ticker 相同

        /**
         * 匹配隐藏通知详情时的 Ticker.
         *
         * Group: 1新消息数目
         */
        @VisibleForTesting
        val hideMsgPattern: Pattern = Pattern.compile("^你收到了(\\d+)条新消息$")

    }

    protected val ctx: Context = context.applicationContext

    private val qzoneSpecialTitle = "特别关心动态"

    private val qqHistory = ArrayList<Conversation>()
    private val qqLiteHistory = ArrayList<Conversation>()
    private val qqHdHistory = ArrayList<Conversation>()
    private val timHistory = ArrayList<Conversation>()

    private val avatarManager =
        AvatarManager.get(getAvatarDiskCacheDir(ctx), getAvatarCachePeriod())

    fun setAvatarCachePeriod(period: Long) {
        avatarManager.period = period
    }

    /**
     * 检测到合并消息的回调。
     *
     * 合并消息：有 x 个联系人给你发过来y条新消息
     *
     * @param isBindingMsg 是否来自关联 QQ 的消息。
     */
    protected open fun onMultiMessageDetected(isBindingMsg: Boolean) {}

    /**
     * 创建优化后的QQ空间通知。
     *
     * @param tag 来源应用标记。
     * @param conversation 需要展示的内容。
     * @param original 原始通知。
     *
     * @return 优化后的通知。
     */
    protected abstract fun renewQzoneNotification(
        context: Context,
        tag: Tag,
        conversation: Conversation,
        original: Notification
    ): Notification

    /**
     * 创建优化后的会话消息通知。
     *
     * @param tag 来源应用标记。
     * @param channel 隶属的通知渠道。
     * @param conversation 需要展示的内容。
     * @param original 原始通知。
     *
     * @return 优化后的通知。
     */
    protected abstract fun renewConversionNotification(
        context: Context,
        tag: Tag,
        channel: NotifyChannel,
        conversation: Conversation,
        original: Notification
    ): Notification


    /**
     * 解析原始通知，返回优化后的通知。
     *
     * @param packageName 来源应用包名。
     * @param sbn 原始通知。
     * @return 优化后的通知。若未匹配到已知模式或消息内容被隐藏则返回 `null`.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun resolveNotification(
        context: Context,
        packageName: String,
        sbn: Notification
    ): Notification? {
        val tag = getTagFromPackageName(packageName)
        if (tag == Tag.UNKNOWN) {
            Log.w(TAG, "Unknown tag, skip. pkgName=$packageName")
            return null
        }

        val title = sbn.extras.getString(Notification.EXTRA_TITLE)
        val content = sbn.extras.getString(Notification.EXTRA_TEXT)
        val ticker = sbn.tickerText?.toString()

        val isMulti = isMulti(ticker, content)
        val isQzone = isQzone(title)

        if (isMulti) {
            onMultiMessageDetected(ticker?.contains("关联QQ号-") ?: false)
        }

        // 隐藏消息详情
        if (isHidden(ticker, content)) {
            Log.v(TAG, "Hidden message content, skip.")
            return null
        }

        // QQ空间
        tryResolveQzone(
            context,
            tag,
            sbn,
            isQzone,
            title,
            ticker,
            content
        )?.also { conversation ->
            return renewQzoneNotification(context, tag, conversation, sbn)
        }

        if (ticker == null) {
            Log.i(TAG, "Ticker is null, skip.")
            return null
        }

        // 群消息
        tryResolveGroupMsg(
            context,
            tag,
            sbn,
            isMulti,
            title,
            ticker,
            content
        )?.also { (channel, conversation) ->
            return renewConversionNotification(context, tag, channel, conversation, sbn)
        }

        // 私聊消息
        tryResolvePrivateMsg(
            context,
            tag,
            sbn,
            isMulti,
            title,
            ticker
        )?.also { (channel, conversation) ->
            return renewConversionNotification(context, tag, channel, conversation, sbn)
        }

        // 关联账号消息
        tryResolveBindingMsg(
            context,
            tag,
            sbn,
            title,
            ticker,
            content
        )?.also { (channel, conversation) ->
            return renewConversionNotification(context, tag, channel, conversation, sbn)
        }

        Log.w(TAG, "[None] Not match any pattern.")
        return null
    }

    private fun isMulti(ticker: String?, content: String?): Boolean {
        // 合并消息
        // title: QQ
        // ticker: 昵称:内容
        // text: 有 x 个联系人给你发过来y条新消息
        return content?.let {
            !it.contains(":") && !(ticker?.endsWith(it) ?: false)
        } ?: false
    }

    private fun isQzone(title: String?): Boolean {
        return title?.let { qzonePattern.matcher(it).matches() } ?: false
    }

    private fun isHidden(ticker: String?, content: String?): Boolean {
        return ticker != null && ticker == content && hideMsgPattern.matcher(ticker).matches()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun tryResolveQzone(
        context: Context, tag: Tag, original: Notification, isQzone: Boolean, title: String?,
        ticker: String?, content: String?
    ): Conversation? {
        if (isQzone && !content.isNullOrEmpty()) {
            val num = matchQzoneNum(title)
            val conversation: Conversation
            if (num == -1) {
                // 特别关心动态推送
                getNotifyLargeIcon(context, original)?.also {
                    avatarManager.saveAvatar(CONVERSATION_NAME_QZONE_SPECIAL.hashCode(), it)
                }
                conversation = addMessage(
                    tag,
                    qzoneSpecialTitle,
                    content,
                    null,
                    avatarManager.getAvatar(CONVERSATION_NAME_QZONE_SPECIAL.hashCode()),
                    original.contentIntent,
                    original.deleteIntent,
                    false
                )
                // 由于特别关心动态推送的通知没有显示未读消息个数，所以这里无法提取并删除多余的历史消息。
                // Workaround: 在通知删除回调下来匹配并清空特别关心动态历史记录。
                Log.d(TAG, "[QZoneSpecial] Ticker: $ticker")
            } else {
                // 与我相关的动态
                getNotifyLargeIcon(context, original)?.also {
                    avatarManager.saveAvatar(CONVERSATION_NAME_QZONE.hashCode(), it)
                }
                conversation = addMessage(
                    tag,
                    "空间动态",
                    content,
                    null,
                    avatarManager.getAvatar(CONVERSATION_NAME_QZONE.hashCode()),
                    original.contentIntent,
                    original.deleteIntent,
                    false
                )
                deleteOldMessage(conversation, num)
                Log.d(TAG, "[QZone] Ticker: $ticker")
            }
            return conversation
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun tryResolveGroupMsg(
        context: Context, tag: Tag, original: Notification, isMulti: Boolean, title: String?,
        ticker: String, content: String?
    ): Pair<NotifyChannel, Conversation>? {
        groupMsgPattern.matcher(ticker).also { matcher ->
            if (matcher.matches()) {
                val name = matcher.group(1) ?: return null
                val groupName = matcher.group(2) ?: return null
                val text = matcher.group(3) ?: return null

                val contentMatcher = groupMsgContentPattern.matcher(content!!)
                val special = contentMatcher.matches() && contentMatcher.group(1) != null

                if (!isMulti)
                    getNotifyLargeIcon(context, original)?.also {
                        avatarManager.saveAvatar(groupName.hashCode(), it)
                    }
                val conversation = addMessage(
                    tag, name, text, groupName, avatarManager.getAvatar(name.hashCode()),
                    original.contentIntent, original.deleteIntent, special
                )
                deleteOldMessage(conversation, if (isMulti) 0 else matchMessageNum(title))
                Log.d(TAG, "[${if (special) "GroupS" else "Group"}] Name: $name; Group: $groupName; Text: $text")
                val channel = if (special && specialGroupMsgChannel())
                    NotifyChannel.FRIEND_SPECIAL
                else
                    NotifyChannel.GROUP
                return Pair(channel, conversation)
            }
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun tryResolvePrivateMsg(
        context: Context, tag: Tag, original: Notification, isMulti: Boolean,
        title: String?, ticker: String
    ): Pair<NotifyChannel, Conversation>? {
        msgPattern.matcher(ticker).also { matcher ->
            if (matcher.matches()) {
                val titleMatcher = msgTitlePattern.matcher(title ?: "")
                val special = titleMatcher.matches() && titleMatcher.group(1) != null
                val name = matcher.group(1) ?: return null
                val text = matcher.group(2) ?: return null
                if (!isMulti)
                    getNotifyLargeIcon(context, original)?.also {
                        avatarManager.saveAvatar(name.hashCode(), it)
                    }
                val conversation = addMessage(
                    tag, name, text, null, avatarManager.getAvatar(name.hashCode()),
                    original.contentIntent, original.deleteIntent, special
                )
                deleteOldMessage(conversation, if (isMulti) 0 else matchMessageNum(titleMatcher))
                return if (special) {
                    Log.d(TAG, "[FriendS] Name: $name; Text: $text")
                    Pair(NotifyChannel.FRIEND_SPECIAL, conversation)
                } else {
                    Log.d(TAG, "[Friend] Name: $name; Text: $text")
                    Pair(NotifyChannel.FRIEND, conversation)
                }
            }
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun tryResolveBindingMsg(
        context: Context, tag: Tag, original: Notification, title: String?,
        ticker: String, content: String?
    ): Pair<NotifyChannel, Conversation>? {
        bindingQQMsgTickerPattern.matcher(ticker).also { matcher ->
            if (matcher.matches()) {
                val account = matcher.group(1) ?: ""
                val text = matcher.group(2) ?: return null
                val conversation = addMessage(
                    tag, "关联账号 $account",
                    text, null, getNotifyLargeIcon(context, original), original.contentIntent,
                    original.deleteIntent, false
                )
                deleteOldMessage(conversation, matchBindingMsgNum(title, content))
                Log.d(TAG, "[Binding] Account: $account; Text: $text")
                return Pair(NotifyChannel.FRIEND, conversation)
            }
        }
        return null
    }

    /**
     * 提取新消息个数。
     */
    private fun matchMessageNum(text: String?): Int {
        if (text.isNullOrEmpty()) return 0
        return matchMessageNum(msgTitlePattern.matcher(text))
    }

    /**
     * @param matcher [msgTitlePattern] 生成的匹配器。
     */
    private fun matchMessageNum(matcher: Matcher): Int {
        if (matcher.matches()) {
            return matcher.group(2)?.toInt() ?: 1
        }
        return 1
    }

    /**
     * 提取空间未读消息个数。
     *
     * @return 动态未读消息个数。若是特别关心推送则返回 `-1`。[title] 为空或不匹配则返回 `0`。
     */
    private fun matchQzoneNum(title: String?): Int {
        if (title.isNullOrEmpty()) return 0
        qzonePattern.matcher(title).also { matcher ->
            if (matcher.matches()) {
                return matcher.group(1)?.toInt() ?: -1
            }
        }
        return 0
    }

    /**
     * 提取关联账号的未读消息个数。
     */
    private fun matchBindingMsgNum(title: String?, content: String?): Int {
        if (title == null || content == null) return 1
        if (title == "QQ") {
            bindingQQMsgContextPattern.matcher(content).also { matcher ->
                if (matcher.matches()) {
                    return matcher.group(1)?.toInt() ?: 1
                }
            }
        } else {
            bindingQQMsgTitlePattern.matcher(title).also { matcher ->
                if (matcher.matches()) {
                    return matcher.group(1)?.toInt() ?: 1
                }
            }
        }

        return 1
    }

    /**
     * 获取通知的大图标。
     *
     * @param notification 原有通知。
     * @return 通知的大图标。
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun getNotifyLargeIcon(context: Context, notification: Notification): Bitmap? {
        return notification.getLargeIcon()?.loadDrawable(context)?.toBitmap()
    }

    /**
     * 创建新样式的通知。
     *
     * @param tag       来源标记。
     * @param channel   通知渠道。
     * @param style     通知样式。
     * @param largeIcon 大图标。
     * @param original  原始通知。
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun createNotification(
        context: Context,
        tag: Tag,
        channel: NotifyChannel,
        style: NotificationCompat.Style?,
        largeIcon: Bitmap?,
        original: Notification,
        subtext: String? = null,
        title: String? = null, text: String? = null, ticker: String? = null,
        shortcutInfo: ShortcutInfoCompat? = null
    ): Notification {
        val channelId = getChannelId(channel)

        @Suppress("DEPRECATION")
        val builder = NotificationCompat.Builder(context, channelId)
            .setColor(
                if (channel == NotifyChannel.QZONE)
                    Color.argb(0xff, 0xfe, 0xce, 0x00)
                else
                    Color.argb(0xff, 0x1E, 0xD0, 0xFC)
            )
            .setAutoCancel(true)
            .setShowWhen(true)
            .setStyle(style)
            .setAutoCancel(true)
            .setPriority(Notification.PRIORITY_HIGH)
            .setLights(original.ledARGB, original.ledOnMS, original.ledOffMS)
            .setLargeIcon(largeIcon)
            .setChannelId(channelId)

        if (subtext != null)
            builder.setSubText(subtext)
        if (title != null)
            builder.setContentTitle(title)
        if (text != null)
            builder.setContentText(text)
        if (ticker != null)
            builder.setTicker(ticker)

        setIcon(context, builder, tag, channel == NotifyChannel.QZONE)

        return buildNotification(builder, shortcutInfo).apply {
            extras.putString(NOTIFICATION_EXTRA_TAG, tag.name)
        }
    }

    protected open fun buildNotification(
        builder: NotificationCompat.Builder,
        shortcutInfo: ShortcutInfoCompat?
    ): Notification {
        return builder.build()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    protected fun createQZoneNotification(
        context: Context, tag: Tag, conversation: Conversation,
        original: Notification
    ): Notification {
        val style = NotificationCompat.MessagingStyle(
            Person.Builder()
                .setName("空间动态").build()
        )
        conversation.messages.forEach { msg ->
            style.addMessage(msg)
        }
        val num = conversation.messages.size
        val subtext =
            if (num > 1) "$num 条新动态" else null
        Log.v(TAG, "Create QZone notification for $num messages.")
        return createNotification(
            context, tag, NotifyChannel.QZONE, style,
            avatarManager.getAvatar(CONVERSATION_NAME_QZONE.hashCode()), original, subtext
        )
    }


    /**
     * 创建会话消息通知。
     *
     * @param tag      来源标记。
     * @param original 原始通知。
     */
    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("BinaryOperationInTimber")
    protected fun createConversationNotification(
        context: Context, tag: Tag, channel: NotifyChannel,
        conversation: Conversation, original: Notification
    ): Notification {
        val style =
            NotificationCompat.MessagingStyle(Person.Builder().setName(conversation.name).build())
        if (conversation.isGroup) {
            style.conversationTitle = conversation.name
            style.isGroupConversation = true
        }
        conversation.messages.forEach { msg ->
            style.addMessage(msg)
        }
        val num = conversation.messages.size
        val subtext =
            if (num > 1) "$num 条新消息" else null
        Log.v(TAG, "Create conversation notification for $num messages.")

        val shortcut = ShortcutInfoCompat.Builder(context, conversation.name)
            .setIsConversation()
            .setPersons(conversation.messages.map { it.person }.toSet().toTypedArray())
            .setShortLabel(conversation.name)
            .setLongLabel(conversation.name)
            .setIcon(
                avatarManager.getAvatar(conversation.name.hashCode())
                    ?.let { IconCompat.createWithBitmap(it) }
            )
            .setIntent( // just a placeholder, use any activity here is okay
                context.packageManager.getLaunchIntentForPackage(tag.pkg)
                    ?: Intent(context, ConfigV2Activity::class.java).apply {
                        action = Intent.ACTION_MAIN
                    }
            )
            .build()
        return createNotification(
            context, tag, channel, style,
            avatarManager.getAvatar(conversation.name.hashCode()), original, subtext,
            shortcutInfo = shortcut
        )
    }

    private fun NotificationCompat.MessagingStyle.addMessage(message: Message) {
        var name = message.person.name!!

        name = name

        if (message.special && showSpecialPrefix()) {
            // 添加特别关心或关注前缀
            name = if (isGroupConversation)
                "[特别关注] $name"
            else
                "[特别关心] $name"
        }

        val person = if (name == message.person.name) {
            message.person
        } else {
            message.person.clone(name)
        }
        addMessage(message.content, message.time, person)
    }

    private fun Person.clone(newName: CharSequence? = null): Person {
        return Person.Builder()
            .setBot(this.isBot)
            .setIcon(this.icon)
            .setImportant(this.isImportant)
            .setKey(this.key)
            .setName(newName ?: this.name)
            .setUri(this.uri)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setIcon(
        context: Context,
        builder: NotificationCompat.Builder,
        tag: Tag,
        isQzone: Boolean
    ) {
        builder.setSmallIcon(IconCompat.createWithBitmap((
            if (isQzone)
                res_inject_ic_notify_qzone
            else
                res_inject_ic_notify_qq
            ).toBitmap()
        ))
    }

    /**
     * 获取历史消息。
     */
    private fun getHistoryMessage(tag: Tag): ArrayList<Conversation> {
        return when (tag) {
            Tag.TIM -> timHistory
            Tag.QQ_LITE -> qqLiteHistory
            Tag.QQ -> qqHistory
            Tag.QQ_HD -> qqHdHistory
            else -> throw RuntimeException("Unknown tag: $tag.")
        }
    }

    /**
     * 加入历史消息记录。
     *
     * @param name 发送者昵称。
     * @param content 消息内容。
     * @param group 群组名。`null` 表示非群组消息。
     * @param special 是否来自特别关心或特别关注。
     */
    private fun addMessage(
        tag: Tag, name: String, content: String, group: String?, icon: Bitmap?,
        contentIntent: PendingIntent, deleteIntent: PendingIntent, special: Boolean
    ): Conversation {
        var conversation: Conversation? = null
        // 以会话名为标准寻找已存在的会话
        for (item in getHistoryMessage(tag)) {
            if (group != null) {
                if (item.isGroup && item.name == group) {
                    conversation = item
                    break
                }
            } else {
                if (!item.isGroup && item.name == name) {
                    conversation = item
                    break
                }
            }
        }
        if (conversation == null) {
            // 创建新会话
            conversation = Conversation(group != null, group ?: name, contentIntent, deleteIntent)
            getHistoryMessage(tag).add(conversation)
        }
        conversation.messages.add(Message(name, icon, content, special))
        return conversation
    }

    /**
     * 删除旧的消息，直到剩余消息个数 <= [maxMessageNum].
     *
     * @param conversation 要清理消息的会话。
     * @param maxMessageNum 最多允许的消息个数，若小于1则忽略。
     */
    private fun deleteOldMessage(conversation: Conversation, maxMessageNum: Int) {
        if (maxMessageNum < 1)
            return
        if (conversation.messages.size <= maxMessageNum)
            return
        Log.d(TAG, "Delete old messages. conversation: ${conversation.name}, max: $maxMessageNum")
        while (conversation.messages.size > maxMessageNum) {
            conversation.messages.removeAt(0)
        }
    }

    protected data class Conversation(
        val isGroup: Boolean,
        val name: String,
        var contentIntent: PendingIntent,
        var deleteIntent: PendingIntent
    ) {
        val messages = ArrayList<Message>()
    }

    /**
     * @param name 发送者昵称。
     * @param icon 头像。
     * @param content 消息内容。
     * @param special 是否来自特别关心或特别关注。仅在聊天消息中有效。
     */
    protected data class Message(
        val name: String,
        val icon: Bitmap?,
        val content: String,
        val special: Boolean
    ) {
        val person: Person = Person.Builder()
            .setIcon(icon?.let { IconCompat.createWithBitmap(it) })
            .setName(name)
            .build()
        val time = System.currentTimeMillis()
    }
}
