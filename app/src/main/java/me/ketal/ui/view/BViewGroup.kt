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

package me.ketal.ui.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import cc.ioctl.util.LayoutHelper.dip2px
import cc.ioctl.util.LayoutHelper.dip2sp


abstract class BViewGroup(context: Context) : ViewGroup(context) {
    private val MODE_SHIFT = 30
    private val MODE_MASK = 0x3 shl MODE_SHIFT
    protected fun View.defaultWidthMeasureSpec(parentView: ViewGroup): Int {
        return when (layoutParams.width) {
            ViewGroup.LayoutParams.MATCH_PARENT -> parentView.measuredWidth.toExactlyMeasureSpec()
            ViewGroup.LayoutParams.WRAP_CONTENT -> ViewGroup.LayoutParams.WRAP_CONTENT.toAtMostMeasureSpec()
            0 -> throw IllegalAccessException("Need special treatment for $this")
            else -> layoutParams.width.toExactlyMeasureSpec()
        }
    }

    protected fun View.defaultHeightMeasureSpec(parentView: ViewGroup): Int {
        return when (layoutParams.height) {
            ViewGroup.LayoutParams.MATCH_PARENT -> parentView.measuredHeight.toExactlyMeasureSpec()
            ViewGroup.LayoutParams.WRAP_CONTENT -> ViewGroup.LayoutParams.WRAP_CONTENT.toAtMostMeasureSpec()
            0 -> throw IllegalAccessException("Need special treatment for $this")
            else -> layoutParams.height.toExactlyMeasureSpec()
        }
    }

    protected fun Int.toExactlyMeasureSpec(): Int {
        return makeMeasureSpec(this, MeasureSpec.EXACTLY)
    }

    protected fun Int.toAtMostMeasureSpec(): Int {
        return makeMeasureSpec(this, MeasureSpec.AT_MOST)
    }

    protected fun View.autoMeasure() {
        measure(
            this.defaultWidthMeasureSpec(parentView = this@BViewGroup),
            this.defaultHeightMeasureSpec(parentView = this@BViewGroup)
        )
    }

    protected fun View.toHorizontalCenter(parentView: ViewGroup): Int {
        return (parentView.measuredWidth - measuredWidth) / 2
    }

    protected fun View.toVerticalCenter(parentView: ViewGroup): Int {
        return (parentView.measuredHeight - measuredHeight) / 2
    }

    protected fun View.toViewHorizontalCenter(targetView: View): Int {
        return targetView.left - (measuredWidth - targetView.measuredWidth) / 2
    }

    protected fun View.toViewVerticalCenter(targetView: View): Int {
        return targetView.top - (measuredHeight - targetView.measuredHeight) / 2
    }

    protected fun View.layout(x: Int, y: Int, fromRight: Boolean = false) {
        if (!fromRight) {
            layout(x, y, x + measuredWidth, y + measuredHeight)
        } else {
            layout(this@BViewGroup.measuredWidth - x - measuredWidth, y)
        }
    }

    protected val Int.dp: Int get() = dip2px(context, this.toFloat())
    protected val Int.dp2sp: Float get() = dip2sp(context, this.toFloat())
    protected val View.measuredWidthWithMargins get() = (measuredWidth + marginLeft + marginRight)
    protected val View.measuredHeightWithMargins get() = (measuredHeight + marginTop + marginBottom)
    protected fun makeMeasureSpec(size: Int, mode: Int): Int {
        return size and MODE_MASK.inv() or (mode and MODE_MASK)
    }

    protected class LayoutParams(width: Int, height: Int) : MarginLayoutParams(width, height)
}
