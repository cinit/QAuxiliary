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

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import io.github.qauxv.util.hostInfo

internal val linearParams = LinearLayout.LayoutParams(0, 0)
internal val relativeParams = RelativeLayout.LayoutParams(0, 0)
internal val frameLayoutParams = FrameLayout.LayoutParams(0, 0)

internal fun View.hide() {
    this.visibility = View.GONE
    when (this.parent as ViewGroup) {
        is LinearLayout -> this.layoutParams = linearParams
        is RelativeLayout -> this.layoutParams = relativeParams
        is FrameLayout -> this.layoutParams = frameLayoutParams
    }
}

internal fun Any.hostId(name: String): Int? {
    return this.getIdentifier("id", name)
}

internal fun Any.hostLayout(name: String): Int? {
    return this.getIdentifier("layout", name)
}

internal fun Any.hostDrawable(name: String): Int? {
    return this.getIdentifier("drawable", name)
}

internal fun Any.getIdentifier(defType: String, name: String): Int? {
    return when (this) {
        is View -> this.resources.getIdentifier(name, defType, hostInfo.packageName)
        is Context -> this.resources.getIdentifier(name, defType, hostInfo.packageName)
        else -> null
    }
}

internal fun <T : View?> Any.findHostView(name: String): T? {
    return when (this) {
        is View -> this.hostId(name)?.let { this.findViewById<T>(it) }
        is Activity -> this.hostId(name)?.let { this.findViewById<T>(it) }
        is Dialog -> this.hostId(name)?.let { this.findViewById<T>(it) }
        else -> null
    }
}
