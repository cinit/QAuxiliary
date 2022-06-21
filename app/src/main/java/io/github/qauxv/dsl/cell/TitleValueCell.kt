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

package io.github.qauxv.dsl.cell

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.res.ResourcesCompat
import cc.ioctl.util.LayoutHelper
import cc.ioctl.util.LayoutHelper.MATCH_PARENT
import cc.ioctl.util.LayoutHelper.WRAP_CONTENT
import cc.ioctl.util.LayoutHelperViewScope
import cc.ioctl.util.ui.ThemeAttrUtils
import io.github.qauxv.R

class TitleValueCell(
    context: Context,
) : FrameLayout(context), LayoutHelperViewScope {

    val titleView: TextView
    val summaryView: TextView
    val valueView: TextView
    val switchView: SwitchCompat

    private val dividerColor: Int
    private val dividerPaint by lazy {
        Paint().apply {
            strokeWidth = 1.dp.toFloat()
        }
    }

    private val mCenterVertical = LayoutHelper.newFrameLayoutParamsRel(MATCH_PARENT, WRAP_CONTENT,
            Gravity.CENTER_VERTICAL or Gravity.START, 21.dp, 0, 21.dp, 0)
    private val mCenterTop = LayoutHelper.newFrameLayoutParamsRel(MATCH_PARENT, WRAP_CONTENT,
            Gravity.TOP or Gravity.START, 21.dp, 10.dp, 21.dp, 0)

    init {
        minimumHeight = 50.dp
        dividerColor = ResourcesCompat.getColor(resources, R.color.divideColor, context.theme)
        // title text view
        titleView = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f);
            setTextColor(ResourcesCompat.getColor(resources, R.color.firstTextColor, context.theme))
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
        }.also {
            addView(it, mCenterVertical)
        }
        // summary text view
        summaryView = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f);
            setTextColor(ResourcesCompat.getColor(resources, R.color.thirdTextColor, context.theme))
            gravity = Gravity.START
            visibility = GONE
        }.also {
            addView(it, LayoutHelper.newFrameLayoutParamsRel(WRAP_CONTENT, WRAP_CONTENT,
                    Gravity.TOP or Gravity.START, 21.dp, 34.dp, 70.dp, 6.dp))
        }
        // value text view
        valueView = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f);
            setTextColor(ThemeAttrUtils.resolveColorOrDefaultColorRes(context, androidx.appcompat.R.attr.colorAccent, R.color.colorAccent))
            visibility = GONE
        }.also {
            addView(it, LayoutHelper.newFrameLayoutParamsRel(WRAP_CONTENT, WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL or Gravity.END, 22.dp, 0, 22.dp, 0))
        }
        // switch view
        switchView = SwitchCompat(context).apply {
            visibility = GONE
            // disable click for default because this behavior is managed by the recycler view,
            // but they can still set onCheckedChangeListener if they want
            isClickable = false
        }.also {
            addView(it, LayoutHelper.newFrameLayoutParamsRel(WRAP_CONTENT, WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL or Gravity.END, 22.dp, 0, 22.dp, 0))
        }
    }

    var title: String
        get() = titleView.text?.toString() ?: ""
        set(value) {
            titleView.text = value
        }

    var summary: CharSequence?
        get() = summaryView.text
        set(value) {
            summaryView.text = value
            summaryView.visibility = if (value.isNullOrEmpty()) GONE else VISIBLE
            titleView.layoutParams = if (value.isNullOrEmpty()) mCenterVertical else mCenterTop
            requestLayout()
        }

    var value: String?
        get() = valueView.text.toString()
        set(value) {
            valueView.text = value
            valueView.visibility = if (value.isNullOrEmpty()) GONE else VISIBLE
            if (!value.isNullOrEmpty()) {
                // value text and switch view are in the same position
                switchView.visibility = GONE
            }
            requestLayout()
        }

    var isHasSwitch: Boolean
        get() = switchView.visibility == VISIBLE
        set(value) {
            switchView.visibility = if (value) VISIBLE else GONE
            if (isHasSwitch) {
                // value text and switch view are in the same position
                valueView.visibility = GONE
            }
        }

    var isChecked: Boolean
        get() = switchView.isChecked
        set(value) {
            switchView.isChecked = value
            if (!isHasSwitch) {
                isHasSwitch = true
            }
        }

    var hasDivider: Boolean = true
        set(value) {
            var needInvalidate = false
            if (value != field) {
                needInvalidate = true
            }
            field = value
            if (needInvalidate) {
                invalidate()
            }
        }

    fun isClickOnSwitch(x: Int): Boolean {
        return isHasSwitch && (x >= switchView.left && x <= switchView.right)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (hasDivider) {
            dividerPaint.color = dividerColor
            canvas.drawLine(0f, measuredHeight.toFloat(), measuredWidth.toFloat(), measuredHeight.toFloat(), dividerPaint)
        }
    }
}
