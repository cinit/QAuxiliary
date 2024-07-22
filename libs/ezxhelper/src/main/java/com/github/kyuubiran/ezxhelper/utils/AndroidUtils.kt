package com.github.kyuubiran.ezxhelper.utils

import android.content.Context
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import android.widget.Toast

val mainHandler: Handler by lazy {
    Handler(Looper.getMainLooper())
}

val runtimeProcess: Runtime by lazy {
    Runtime.getRuntime()
}

/**
 * 扩展函数 将函数放到主线程执行 如UI更新、显示Toast等
 */
fun Runnable.postOnMainThread() {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        this.run()
    } else {
        mainHandler.post(this)
    }
}

fun runOnMainThread(runnable: Runnable) {
    runnable.postOnMainThread()
}

/**
 * 扩展函数 显示一个Toast
 * @param msg Toast显示的消息
 * @param length Toast显示的时长
 */
fun Context.showToast(msg: String, length: Int = Toast.LENGTH_SHORT) = runOnMainThread {
    Toast.makeText(this, msg, length).show()
}

/**
 * 扩展函数 显示一个Toast
 * @param msg Toast显示的消息
 * @param args 格式化的参数
 * @param length Toast显示的时长
 */
fun Context.showToast(msg: String, vararg args: Any?, length: Int = Toast.LENGTH_SHORT) = runOnMainThread {
    Toast.makeText(this, msg.format(args), length).show()
}
