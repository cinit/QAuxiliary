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

package io.github.qauxv.util.hotupdate

import io.github.qauxv.config.ConfigManager

object HotUpdateManager {

    const val KEY_HOT_UPDATE_CHANNEL = "KEY_HOT_UPDATE_CHANNEL"

    const val CHANNEL_DISABLED = 0
    const val CHANNEL_STABLE = 1
    const val CHANNEL_BETA = 3
    const val CHANNEL_CANARY = 4

    var currentChannel: Int
        get() = ConfigManager.getDefaultConfig().getIntOrDefault(KEY_HOT_UPDATE_CHANNEL, CHANNEL_DISABLED)
        set(value) {
            check(value in CHANNEL_DISABLED..CHANNEL_CANARY)
            ConfigManager.getDefaultConfig().putInt(KEY_HOT_UPDATE_CHANNEL, value)
        }

    val isHotUpdateEnabled: Boolean
        get() = currentChannel > 0

}
