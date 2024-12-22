package io.github.duzhaokun123.util

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi

fun Window.blurBackground(br: Int, bd: Float) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
    attributes.blurBehindRadius = br
    val blurEnableListener = { _: Boolean ->
        setDimAmount(bd)
        setBackgroundBlurRadius(br)
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
