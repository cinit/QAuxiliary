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

package io.github.qauxv.util.hotupdate

import io.github.qauxv.config.ConfigManager

object HotUpdateManager {

    const val KEY_HOT_UPDATE_CHANNEL = "KEY_HOT_UPDATE_CHANNEL"

    const val CHANNEL_DISABLED = 0
    const val CHANNEL_STABLE = 1
    const val CHANNEL_BETA = 3
    const val CHANNEL_CANARY = 4

    const val ACTION_DISABLE = 0
    const val ACTION_QUERY = 1
    const val ACTION_AUTO_UPDATE_WITH_NOTIFICATION = 2
    const val ACTION_AUTO_UPDATE_WITHOUT_NOTIFICATION = 3

    var currentChannel: Int
        get() = ConfigManager.getDefaultConfig().getIntOrDefault(KEY_HOT_UPDATE_CHANNEL, CHANNEL_DISABLED)
        set(value) {
            check(value in CHANNEL_DISABLED..CHANNEL_CANARY)
            ConfigManager.getDefaultConfig().putInt(KEY_HOT_UPDATE_CHANNEL, value)
        }

    var currentAction: Int
        get() = ConfigManager.getDefaultConfig().getIntOrDefault("KEY_HOT_UPDATE_ACTION", ACTION_QUERY)
        set(value) {
            check(value in ACTION_DISABLE..ACTION_AUTO_UPDATE_WITHOUT_NOTIFICATION)
            ConfigManager.getDefaultConfig().putInt("KEY_HOT_UPDATE_ACTION", value)
        }

    val isHotUpdateEnabled: Boolean
        get() = currentChannel > 0 && currentAction > 0

}
