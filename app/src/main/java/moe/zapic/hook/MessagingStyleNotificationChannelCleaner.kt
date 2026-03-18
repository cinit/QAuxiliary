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

import android.app.NotificationManager
import android.os.Build
import cc.chenhe.qqnotifyevo.utils.NotifyChannel
import cc.chenhe.qqnotifyevo.utils.getChannelId
import io.github.qauxv.util.hostInfo

internal object MessagingStyleNotificationChannelCleaner {

    private val conversationParentChannelIds = setOf(
        getChannelId(NotifyChannel.FRIEND),
        getChannelId(NotifyChannel.FRIEND_SPECIAL),
        getChannelId(NotifyChannel.GROUP)
    )

    fun cleanupIfNeeded(disabled: Boolean) {
        if (!disabled) {
            return
        }
        cleanup()
    }

    fun cleanup() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }
        val notificationManager = hostInfo.application.getSystemService(NotificationManager::class.java)
        notificationManager.notificationChannels
            .asSequence()
            .filter { it.parentChannelId in conversationParentChannelIds }
            .forEach { notificationManager.deleteNotificationChannel(it.id) }
    }
}
