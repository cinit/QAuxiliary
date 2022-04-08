package cc.chenhe.qqnotifyevo.core

import android.app.Notification
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import cc.chenhe.qqnotifyevo.utils.NotifyChannel
import cc.chenhe.qqnotifyevo.utils.Tag

/**
 * 通知处理器，直接创建并返回优化后的通知。
 */
class NevoNotificationProcessor(context: Context) : NotificationProcessor(context) {
    @RequiresApi(Build.VERSION_CODES.M)
    override fun renewQzoneNotification(context: Context, tag: Tag, conversation: Conversation, original: Notification): Notification {
        return createQZoneNotification(context, tag, conversation, original).apply {
            contentIntent = original.contentIntent
            deleteIntent = original.deleteIntent
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun renewConversionNotification(context: Context, tag: Tag, channel: NotifyChannel, conversation: Conversation, original: Notification): Notification {
        return createConversationNotification(context, tag, channel, conversation, original).apply {
            contentIntent = original.contentIntent
            deleteIntent = original.deleteIntent
        }
    }
}
