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

    private const val LEGACY_CONFIG_KEY = "MessagingStyleNotification.disableConversationNotificationAndBubble"
    private const val DISABLE_SUB_CHANNEL_CONFIG_KEY = "MessagingStyleNotification.disableConversationSubChannel"
    private const val DISABLE_BUBBLE_CONFIG_KEY = "MessagingStyleNotification.disableBubble"

    var disableConversationSubChannel: Boolean
        get() {
            val config = getConfig()
            migrateLegacyConfigIfNeeded(config)
            return config.getBooleanOrDefault(DISABLE_SUB_CHANNEL_CONFIG_KEY, false)
        }
        set(value) {
            val config = getConfig()
            migrateLegacyConfigIfNeeded(config)
            config.putBoolean(DISABLE_SUB_CHANNEL_CONFIG_KEY, value)
        }

    var disableBubble: Boolean
        get() {
            val config = getConfig()
            migrateLegacyConfigIfNeeded(config)
            return config.getBooleanOrDefault(DISABLE_BUBBLE_CONFIG_KEY, false)
        }
        set(value) {
            val config = getConfig()
            migrateLegacyConfigIfNeeded(config)
            config.putBoolean(DISABLE_BUBBLE_CONFIG_KEY, value)
        }

    private fun getConfig(): ConfigManager {
        return ConfigManager.getDefaultConfig()
    }

    private fun migrateLegacyConfigIfNeeded(config: ConfigManager) {
        if (!config.containsKey(LEGACY_CONFIG_KEY)) {
            return
        }
        val legacyValue = config.getBooleanOrDefault(LEGACY_CONFIG_KEY, false)
        if (!config.containsKey(DISABLE_SUB_CHANNEL_CONFIG_KEY)) {
            config.putBoolean(DISABLE_SUB_CHANNEL_CONFIG_KEY, legacyValue)
        }
        if (!config.containsKey(DISABLE_BUBBLE_CONFIG_KEY)) {
            config.putBoolean(DISABLE_BUBBLE_CONFIG_KEY, legacyValue)
        }
        config.edit().remove(LEGACY_CONFIG_KEY).apply()
    }
}
