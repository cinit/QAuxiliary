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

package cc.ioctl.util

import android.content.Context
import android.content.res.Resources

interface LayoutHelperViewScope {
    fun getContext(): Context

    val Int.dp: Int get() = LayoutHelper.dip2px(getContext(), this.toFloat())
    val Float.dp: Int get() = LayoutHelper.dip2px(getContext(), this)
}

interface LayoutHelperContextScope {
    fun getResources(): Resources

    val Int.dp: Int
        get() {
            val scale: Float = getResources().displayMetrics.density
            return (this * scale + 0.5f).toInt()
        }
    val Float.dp: Int
        get() {
            val scale: Float = getResources().displayMetrics.density
            return (this * scale + 0.5f).toInt()
        }
}
