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

import io.github.qauxv.config.ConfigManager

internal object MessagingStyleNotificationConfig {

    private const val DISABLE_SUB_CHANNEL_CONFIG_KEY = "MessagingStyleNotification.disableConversationSubChannel"
    private const val DISABLE_BUBBLE_CONFIG_KEY = "MessagingStyleNotification.disableBubble"

    var disableConversationSubChannel: Boolean
        get() = getConfig().getBooleanOrDefault(DISABLE_SUB_CHANNEL_CONFIG_KEY, false)
        set(value) {
            getConfig().putBoolean(DISABLE_SUB_CHANNEL_CONFIG_KEY, value)
        }

    var disableBubble: Boolean
        get() = getConfig().getBooleanOrDefault(DISABLE_BUBBLE_CONFIG_KEY, false)
        set(value) {
            getConfig().putBoolean(DISABLE_BUBBLE_CONFIG_KEY, value)
        }

    private fun getConfig(): ConfigManager {
        return ConfigManager.getDefaultConfig()
    }
}
