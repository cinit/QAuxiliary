/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
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

import io.github.qauxv.base.IDynamicHook
import io.github.qauxv.util.Log

internal fun Log.d(vararg msg: Any?) {
    Log.d("NA: ${msg.joinToString(", ")}")
}

internal fun logThrowable(msg: Throwable) {
    Log.d("Throwable: ${msg.stackTraceToString()}")
}

internal fun <T : IDynamicHook> T.logDetail(info: String?, vararg msg: Any?) {
    Log.d("${this.javaClass.simpleName}: $info, ${msg.joinToString(", ")}")
}

internal fun <T : IDynamicHook> T.logClass(clz: Class<*>? = null) {
    Log.d("$this: Class, ${clz?.name}")
}

internal fun <T : IDynamicHook> T.logStart() {
    Log.d("$this: Start")
}

internal fun <T : IDynamicHook> T.logBefore(msg: String? = "") {
    Log.d("$this: Before, $msg")
}

internal fun <T : IDynamicHook> T.logAfter(msg: String? = "") {
    Log.d("$this: After, $msg")
}
