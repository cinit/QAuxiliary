package io.github.duzhaokun123.util

import android.os.Build
import android.util.TypedValue
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi

/**
 * 设置窗口背景模糊
 * @param br 模糊半径 (dp)
 * @param bd 背景变暗程度 (0.0 - 1.0)
 */
fun Window.blurBackground(br: Int, bd: Float) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val brPx = TypedValue.applyDimension(COMPLEX_UNIT_DIP, br.toFloat(), decorView.context.resources.displayMetrics).toInt()
    addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
    attributes.blurBehindRadius = brPx
    val blurEnableListener = { _: Boolean ->
        setDimAmount(bd)
        setBackgroundBlurRadius(brPx)
    }
    decorView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        @RequiresApi(Build.VERSION_CODES.S)
        override fun onViewAttachedToWindow(v: View) {
            windowManager.addCrossWindowBlurEnabledListener(blurEnableListener)
        }

        @RequiresApi(Build.VERSION_CODES.S)
        override fun onViewDetachedFromWindow(v: View) {
            windowManager.removeCrossWindowBlurEnabledListener(blurEnableListener)
        }

    })
    addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
}
