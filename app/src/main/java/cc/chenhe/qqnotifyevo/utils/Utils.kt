package cc.chenhe.qqnotifyevo.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import java.io.File

//-----------------------------------------------------------
// Intent Action
//-----------------------------------------------------------

// Android O+ 通知渠道 id
const val NOTIFY_FRIEND_CHANNEL_ID = "QQ_Friend"
const val NOTIFY_FRIEND_SPECIAL_CHANNEL_ID = "QQ_Friend_Special"
const val NOTIFY_GROUP_CHANNEL_ID = "QQ_Group"
const val NOTIFY_QZONE_CHANNEL_ID = "QQ_Zone"

fun getChannelId(channel: NotifyChannel): String = when (channel) {
    NotifyChannel.FRIEND -> NOTIFY_FRIEND_CHANNEL_ID
    NotifyChannel.FRIEND_SPECIAL -> NOTIFY_FRIEND_SPECIAL_CHANNEL_ID
    NotifyChannel.GROUP -> NOTIFY_GROUP_CHANNEL_ID
    NotifyChannel.QZONE -> NOTIFY_QZONE_CHANNEL_ID
}

/**
 * 创建通知渠道。仅创建渠道实例，未注册到系统。
 */
@RequiresApi(Build.VERSION_CODES.O)
fun getNotificationChannels(): List<NotificationChannel> {
    val att = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    val friendChannel = NotificationChannel(
        NOTIFY_FRIEND_CHANNEL_ID,
        "联系人消息",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        group = "qq_evolution"
        description = "QQ 私聊消息通知"
        setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), att)
        enableVibration(true)
        enableLights(true)
    }

    val friendSpecialChannel = NotificationChannel(
        NOTIFY_FRIEND_SPECIAL_CHANNEL_ID,
        "特别关心消息",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        group = "qq_evolution"
        description = "QQ 特别关心好友私聊消息通知"
        setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), att)
        enableVibration(true)
        enableLights(true)
    }

    val groupChannel = NotificationChannel(
        NOTIFY_GROUP_CHANNEL_ID,
        "群消息",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        group = "qq_evolution"
        description = "QQ 群消息通知"
        setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), att)
        enableVibration(true)
        enableLights(true)
    }

    val qzoneChannel = NotificationChannel(
        NOTIFY_QZONE_CHANNEL_ID,
        "空间动态",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        group = "qq_evolution"
        description = "QQ 空间动态通知"
        setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), att)
        enableLights(true)
    }

    return listOf(friendChannel, friendSpecialChannel, groupChannel, qzoneChannel)
}

private fun getCacheDir(context: Context): File {
    return if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
        || !Environment.isExternalStorageRemovable()
    ) {
        context.externalCacheDir ?: context.cacheDir
    } else {
        context.cacheDir
    }
}

fun getAvatarDiskCacheDir(context: Context): File {
    return File(getCacheDir(context), "conversion_icon")
}

