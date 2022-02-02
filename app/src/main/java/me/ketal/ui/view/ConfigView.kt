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

package me.ketal.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.view.minusAssign
import androidx.core.view.plusAssign
import cc.ioctl.util.LayoutHelper.dip2px

class ConfigView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    val enable = CheckBox(context)
    var view: View? = null
        set(value) {
            value ?: return
            value.parent?.let {
                it as ViewGroup -= value
            }
            field?.let { this -= field!! }
            this += value
            value.isVisible = isChecked
            field = value
        }
    var isChecked: Boolean
        get() = enable.isChecked
        set(value) {
            enable.isChecked = value
            view?.isVisible = isChecked
        }

    init {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        orientation = VERTICAL
        this += LinearLayout(context).apply {
            addView(enable)
            setPadding(dip2px(context, 21f), 0, dip2px(context, 21f), 0)
        }
        enable.setOnCheckedChangeListener { _, isChecked -> view?.isVisible = isChecked }
    }

    fun setText(text: String) {
        enable.text = text
    }
}
