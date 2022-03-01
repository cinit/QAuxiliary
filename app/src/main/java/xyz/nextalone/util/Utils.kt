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
package xyz.nextalone.util

import android.content.SharedPreferences
import io.github.qauxv.bridge.AppRuntimeHelper
import io.github.qauxv.util.Initiator
import mqq.app.AppRuntime
import java.text.DateFormat
import java.util.*

internal val isSimpleUi by lazy {
    try {
        val sharedPreferences =
            Initiator._ThemeUtil().getDeclaredMethod("getUinThemePreferences", AppRuntime::class.java).invoke(
                null,
                    AppRuntimeHelper.getAppRuntime()
            ) as SharedPreferences
        val bool = sharedPreferences.getBoolean("key_simple_ui_switch", false)
        bool
    } catch (e: Throwable) {
        false
    }
}

internal val Date.today: String
    get() = DateFormat.getDateInstance().format(this)
