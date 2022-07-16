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

package me.ketal.util

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.forEach
import com.afollestad.materialdialogs.MaterialDialog

fun ViewGroup.findViewByText(text: String, contains: Boolean = false) =
    this.findViewByCondition {
        it.javaClass == TextView::class.java && if (!contains) (it as TextView).text == text else (it as TextView).text.contains(
            text
        )
    }

fun ViewGroup.findViewByType(clazz: Class<*>) =
    this.findViewByCondition {
        it.javaClass.isAssignableFrom(clazz)
    }

fun ViewGroup.findViewByCondition(condition: (view: View) -> Boolean): View? {
    this.forEach {
        if (condition(it))
            return it
        val ret = if (it is ViewGroup) {
            it.findViewByCondition(condition)
        } else null
        ret?.let {
            return ret
        }
    }
    return null
}

@Suppress("unused")
fun MaterialDialog.ignoreResult() {
    // do nothing here, just ignore the result to make the lint happy
}
