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

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.AbsoluteSizeSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View.OnClickListener
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import cc.ioctl.util.LayoutHelper
import cc.ioctl.util.LayoutHelperViewScope
import cc.ioctl.util.ui.ThemeAttrUtils
import io.github.qauxv.R

class TextBannerCell @JvmOverloads constructor(
    context: Context,
    padding: Int = 21
) : FrameLayout(context), LayoutHelperViewScope {

    val textView: TextView
    private var topPadding = 10
    private var bottomPadding = 10
    private var fixedSize = 0
    private val paint = Paint()

    var lastDownX = 0
    var lastDownY = 0
    var lastUpX = 0
    var lastUpY = 0

    var textColor: Int
        get() = textView.currentTextColor
        set(value) {
            textView.setTextColor(value)
        }
    var textLinkColor: Int
        get() = textView.currentTextColor
        set(value) {
            textView.setLinkTextColor(value)
        }
    var text: CharSequence? = null
        set(value) {
            if (!value.contentEquals(field)) {
                field = value
                if (value == null) {
                    textView.setPadding(0, 2.dp, 0, 0)
                } else {
                    textView.setPadding(0, topPadding.dp, 0, bottomPadding.dp)
                }
                var spannableString: SpannableString? = null
                if (value != null) {
                    var i = 0
                    val len = value.length
                    while (i < len - 1) {
                        if (value[i] == '\n' && value[i + 1] == '\n') {
                            if (spannableString == null) {
                                spannableString = SpannableString(value)
                            }
                            spannableString.setSpan(
                                AbsoluteSizeSpan(10, true), i + 1, i + 2,
                                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                        i++
                    }
                }
                textView.text = spannableString ?: value
            }
        }

    var textIsSelectable: Boolean
        get() = textView.isTextSelectable
        set(value) {
            textView.setTextIsSelectable(value)
        }

    var isCloseButtonVisible = false
        set(value) {
            field = value
            invalidate()
        }

    var isClosed: Boolean = false
        set(value) {
            field = value
            requestLayout()
        }

    var onCloseClickListener: ((TextBannerCell) -> Unit)? = null

    var onBodyClickListener: ((TextBannerCell) -> Unit)? = null

    fun handleClick(x: Int, y: Int) {
        if (isCloseButtonVisible && isInCloseArea(x, y)) {
            onCloseClickListener?.let { it(this) }
        } else if (isInViewArea(x, y)) {
            onBodyClickListener?.let { it(this) }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (isClosed) {
            super.onMeasure(
                MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY)
            )
            return
        }
        if (fixedSize != 0) {
            super.onMeasure(
                MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(fixedSize.dp, MeasureSpec.EXACTLY)
            )
        } else {
            super.onMeasure(
                MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )
        }
    }

    fun setTopPadding(topPadding: Int) {
        this.topPadding = topPadding
    }

    fun setBottomPadding(value: Int) {
        bottomPadding = value
    }

    fun setFixedSize(size: Int) {
        fixedSize = size
    }

    val length: Int
        get() = textView.text?.length ?: 0

    fun setEnabled(value: Boolean, animators: ArrayList<Animator?>?) {
        if (animators != null) {
            animators.add(ObjectAnimator.ofFloat(textView, ALPHA, if (value) 1f else 0.5f))
        } else {
            textView.alpha = if (value) 1f else 0.5f
        }
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = TextView::class.java.name
        info.text = text
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_POINTER_DOWN) {
            lastDownX = event.x.toInt()
            lastDownY = event.y.toInt()
        } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_POINTER_UP) {
            lastUpX = event.x.toInt()
            lastUpY = event.y.toInt()
        }
        return super.onInterceptTouchEvent(event);
    }

    override fun onDraw(canvas: Canvas) {
        if (!isClosed) {
            if (isCloseButtonVisible) {
                val color = resources.getColor(R.color.thirdTextColor, context.theme)
                paint.color = color
                paint.strokeWidth = 1.dp.toFloat()
                val size = 14.dp.toFloat()
                val boxMarginRight = 16.dp.toFloat()
                val centerX = width - size / 2 - boxMarginRight
                val centerY = height / 2
                paint.strokeCap = Paint.Cap.SQUARE
                canvas.drawLine(centerX - size / 2, centerY - size / 2, centerX + size / 2, centerY + size / 2, paint)
                canvas.drawLine(centerX - size / 2, centerY + size / 2, centerX + size / 2, centerY - size / 2, paint)
            }

            val accentColor = ThemeAttrUtils.resolveColorOrDefaultColorRes(context, androidx.appcompat.R.attr.colorAccent, R.color.colorAccent)

            // draw background 8% accent color
            val color5 = (accentColor and 0x00ffffff) or (0x14000000)
            paint.color = color5
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            // draw top and bottom border with 20% accent color
            val color15 = (accentColor and 0x00ffffff) or (0x33000000)
            paint.color = color15
            paint.strokeWidth = 1.dp.toFloat()
            canvas.drawLine(0f, 0f, width.toFloat(), 0f, paint)
            canvas.drawLine(0f, height.toFloat() - 1, width.toFloat(), height.toFloat() - 1, paint)
        }
    }

    fun isInCloseArea(x: Int, y: Int): Boolean {
        if (!isCloseButtonVisible) {
            return false
        }
        val size = 14.dp.toFloat()
        val boxMarginRight = 16.dp.toFloat()
        val centerX = width - size / 2 - boxMarginRight
        val centerY = height / 2
        return isInViewArea(x, y) && (x >= centerX - size / 2)
    }

    fun isInViewArea(x: Int, y: Int): Boolean {
        return x >= 0 && x <= width && y >= 0 && y <= height
    }

    private val mTextViewOnClickListener = OnClickListener {
        handleClick(lastDownX, lastDownY)
    }

    init {
        textView = AppCompatTextView(context)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        textView.gravity = Gravity.START
        textView.setPadding(0, 10.dp, 0, 17.dp)
        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.setTextColor(textColor)
        textView.setLinkTextColor(textLinkColor)
        textView.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        textColor = ResourcesCompat.getColor(context.resources, R.color.secondTextColor, context.theme)
        textLinkColor = ThemeAttrUtils.resolveColorOrDefaultColorRes(context, androidx.appcompat.R.attr.colorAccent, R.color.colorAccent)
        addView(
            textView, LayoutHelper.newFrameLayoutParamsAbs(
                LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                Gravity.START or Gravity.TOP, padding.dp, 0, padding.dp, 0
            )
        )
        textView.setOnClickListener(mTextViewOnClickListener)
        background = null
    }
}
