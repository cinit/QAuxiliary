package com.github.kyuubiran.ezxhelper.utils

import android.widget.Toast
import com.github.kyuubiran.ezxhelper.init.InitFields.appContext
import io.github.qauxv.util.xpcompat.XposedBridge

open class Logger {
    /**
     * 日志等级 低于等级的日志不会被打印出来
     * 可以配合BuildConfig.DEBUG / RELEASE来使用
     */
    var logLevel: Int = VERBOSE

    /**
     * 日志Tag
     */
    var logTag: String = "EZXHelper"


    /**
     * 是否输出日志到 Xposed
     */
    var logXp: Boolean = true
        internal set

    /**
     * Toast Tag
     */
    var toastTag: String? = null

    companion object LogLevel {
        const val VERBOSE = 0
        const val DEBUG = 1
        const val INFO = 2
        const val WARN = 3
        const val ERROR = 4
    }

    /**
     * 打印日志 等级: Info
     * @param msg 消息
     * @param thr 异常
     */
    open fun i(msg: String, thr: Throwable? = null) {
        if (logLevel > INFO) return
        android.util.Log.i(logTag, msg, thr)
    }

    /**
     * 打印日志 等级: Debug
     * @param msg 消息
     * @param thr 异常
     */
    open fun d(msg: String, thr: Throwable? = null) {
        if (logLevel > DEBUG) return
        android.util.Log.d(logTag, msg, thr)
    }


    /**
     * 打印日志 等级: Warn
     * @param msg 消息
     * @param thr 异常
     */
    open fun w(msg: String, thr: Throwable? = null) {
        if (logLevel > WARN) return
        android.util.Log.w(logTag, msg, thr)
    }


    /**
     * 打印日志 等级: Error
     * @param msg 消息
     * @param thr 异常
     */
    open fun e(msg: String, thr: Throwable? = null) {
        if (logLevel > ERROR) return
        android.util.Log.e(logTag, msg, thr)
    }


    /**
     * 打印日志到Xposed
     * @param level 等级
     * @param msg 消息
     * @param thr 异常
     */
    open fun px(levelInt: Int, level: String, msg: String, thr: Throwable?) {
        if (logLevel > levelInt) return
        if (logXp) XposedBridge.log("[$level/$logTag] $msg: ${thr?.stackTraceToString()}")
    }


    /**
     * 打印日志 等级: Info
     * @param thr 异常
     * @param msg 消息
     */
    fun i(thr: Throwable, msg: String = "") {
        i(msg, thr)
    }

    /**
     * 打印日志 等级: Debug
     * @param thr 异常
     * @param msg 消息
     */
    fun d(thr: Throwable, msg: String = "") {
        d(msg, thr)
    }

    /**
     * 打印日志 等级: Warn
     * @param thr 异常
     * @param msg 消息
     */
    fun w(thr: Throwable, msg: String = "") {
        w(msg, thr)
    }

    /**
     * 打印日志 等级: Error
     * @param thr 异常
     * @param msg 消息
     */
    fun e(thr: Throwable, msg: String = "") {
        e(msg, thr)
    }


    /**
     * 打印日志到Xposed 等级: Info
     * @param msg 消息
     * @param thr 异常
     */
    fun ix(msg: String, thr: Throwable? = null) {
        i(msg, thr)
        px(INFO, "I", msg, thr)
    }


    /**
     * 打印日志到Xposed 等级: Info
     * @param thr 异常
     * @param msg 消息
     */
    fun ix(thr: Throwable, msg: String = "") {
        ix(msg, thr)
    }


    /**
     * 打印日志到Xposed 等级: Warn
     * @param msg 消息
     * @param thr 异常
     */
    fun wx(msg: String, thr: Throwable? = null) {
        w(msg, thr)
        px(WARN, "W", msg, thr)
    }


    /**
     * 打印日志到Xposed 等级: Warn
     * @param thr 异常
     * @param msg 消息
     */
    fun wx(thr: Throwable, msg: String = "") {
        wx(msg, thr)
    }


    /**
     * 打印日志到Xposed 等级: Debug
     * @param msg 消息
     * @param thr 异常
     */
    fun dx(msg: String, thr: Throwable? = null) {
        d(msg, thr)
        px(DEBUG, "D", msg, thr)
    }


    /**
     * 打印日志到Xposed 等级: Debug
     * @param thr 异常
     * @param msg 消息
     */
    fun dx(thr: Throwable, msg: String = "") {
        dx(msg, thr)
    }


    /**
     * 打印日志到Xposed 等级: Error
     * @param msg 消息
     * @param thr 异常
     */
    fun ex(msg: String, thr: Throwable? = null) {
        e(msg, thr)
        px(ERROR, "E", msg, thr)
    }


    /**
     * 打印日志到Xposed 等级: Error
     * @param thr 异常
     * @param msg 消息
     */
    fun ex(thr: Throwable, msg: String = "") {
        ex(msg, thr)
    }
}

object Log {
    private val defaultLogger = Logger()
    private var logger: Logger? = null

    var currentLogger: Logger
        get() = logger ?: defaultLogger
        set(value) {
            logger = value
        }

    /**
     * 如果显示Toast时上一个Toast还没消失，设置是否取消上一个Toast，并显示本次Toast
     */
    var cancelLastToast: Boolean = false

    private var toast: Toast? = null

    fun i(msg: String, thr: Throwable? = null) {
        currentLogger.i(msg, thr)
    }

    fun d(msg: String, thr: Throwable? = null) {
        currentLogger.d(msg, thr)
    }

    fun w(msg: String, thr: Throwable? = null) {
        currentLogger.w(msg, thr)
    }

    fun e(msg: String, thr: Throwable? = null) {
        currentLogger.e(msg, thr)
    }

    fun ix(msg: String, thr: Throwable? = null) {
        currentLogger.ix(msg, thr)
    }

    fun wx(msg: String, thr: Throwable? = null) {
        currentLogger.wx(msg, thr)
    }

    fun dx(msg: String, thr: Throwable? = null) {
        currentLogger.dx(msg, thr)
    }

    fun ex(msg: String, thr: Throwable? = null) {
        currentLogger.ex(msg, thr)
    }

    fun i(thr: Throwable, msg: String = "") {
        currentLogger.i(thr, msg)
    }

    fun d(thr: Throwable, msg: String = "") {
        currentLogger.d(thr, msg)
    }

    fun w(thr: Throwable, msg: String = "") {
        currentLogger.w(thr, msg)
    }

    fun e(thr: Throwable, msg: String = "") {
        currentLogger.e(thr, msg)
    }

    fun ix(thr: Throwable, msg: String = "") {
        currentLogger.ix(thr, msg)
    }

    fun wx(thr: Throwable, msg: String = "") {
        currentLogger.wx(thr, msg)
    }

    fun dx(thr: Throwable, msg: String = "") {
        currentLogger.dx(thr, msg)
    }

    fun ex(thr: Throwable, msg: String = "") {
        currentLogger.ex(thr, msg)
    }

    /**
     * 显示一个Toast
     *
     * 需要先初始化appContext才能使用
     *
     * 如果不设置TOAST_TAG
     * 则不显示前缀
     * @see setToastTag
     */
    fun toast(msg: String, duration: Int = Toast.LENGTH_SHORT) = runOnMainThread {
        if (cancelLastToast) toast?.cancel()
        toast = null
        toast = Toast.makeText(appContext, null, duration).apply {
            setText(if (currentLogger.toastTag != null) "${currentLogger.toastTag}: $msg" else msg)
            show()
        }
    }

    fun toast(msg: String, vararg formats: String, duration: Int = Toast.LENGTH_SHORT) =
        toast(msg.format(*formats), duration)

    /**
     * 扩展函数 配合runCatching使用
     * 如果抛出异常 则调用 Log.i 记录
     * @param msg 消息
     * @param then 发生异常时执行的函数
     * @see runCatching
     * @see i
     */
    inline fun <R> Result<R>.logiIfThrow(msg: String = "", then: ((Throwable) -> Unit) = {}) =
        this.exceptionOrNull()?.let {
            currentLogger.i(it, msg)
            then(it)
        }

    /**
     * 扩展函数 配合runCatching使用
     * 如果抛出异常 则调用 Log.ix 记录
     * @param msg 消息
     * @param then 发生异常时执行的函数
     * @see runCatching
     * @see ix
     */
    inline fun <R> Result<R>.logixIfThrow(msg: String = "", then: ((Throwable) -> Unit) = {}) =
        this.exceptionOrNull()?.let {
            currentLogger.i(it, msg)
            then(it)
        }

    /**
     * 扩展函数 配合 runCatching 使用
     * 如果抛出异常 则调用 Log.d 记录
     * @param msg 消息
     * @param then 发生异常时执行的函数
     * @see runCatching
     * @see d
     */
    inline fun <R> Result<R>.logdIfThrow(msg: String = "", then: (Throwable) -> Unit = {}) =
        this.exceptionOrNull()?.let {
            currentLogger.d(it, msg)
            then(it)
        }

    /**
     * 扩展函数 配合 runCatching 使用
     * 如果抛出异常 则调用 Log.dx 记录
     * @param msg 消息
     * @param then 发生异常时执行的函数
     * @see runCatching
     * @see dx
     */
    inline fun <R> Result<R>.logdxIfThrow(msg: String = "", then: (Throwable) -> Unit = {}) =
        this.exceptionOrNull()?.let {
            currentLogger.dx(it, msg)
            then(it)
        }

    /**
     * 扩展函数 配合 runCatching 使用
     * 如果抛出异常 则调用 Log.w 记录
     * @param msg 消息
     * @param then 发生异常时执行的函数
     * @see runCatching
     * @see w
     */
    inline fun <R> Result<R>.logwIfThrow(msg: String = "", then: (Throwable) -> Unit = {}) =
        this.exceptionOrNull()?.let {
            currentLogger.w(it, msg)
            then(it)
        }

    /**
     * 扩展函数 配合 runCatching 使用
     * 如果抛出异常 则调用 Log.w 记录
     * @param msg 消息
     * @param then 发生异常时执行的函数
     * @see runCatching
     * @see wx
     */
    inline fun <R> Result<R>.logwxIfThrow(msg: String = "", then: (Throwable) -> Unit = {}) =
        this.exceptionOrNull()?.let {
            currentLogger.wx(it, msg)
            then(it)
        }

    /**
     * 扩展函数 配合 runCatching 使用
     * 如果抛出异常 则调用 Log.e 记录
     * @param msg 消息
     * @param then 发生异常时执行的函数
     * @see runCatching
     * @see e
     */
    inline fun <R> Result<R>.logeIfThrow(msg: String = "", then: (Throwable) -> Unit = {}) =
        this.exceptionOrNull()?.let {
            currentLogger.e(it, msg)
            then(it)
        }

    /**
     * 扩展函数 配合 runCatching 使用
     * 如果抛出异常 则调用 Log.ex 记录
     * @param msg 消息
     * @param then 发生异常时执行的函数
     * @see runCatching
     * @see ex
     */
    inline fun <R> Result<R>.logexIfThrow(msg: String = "", then: (Throwable) -> Unit = {}) =
        this.exceptionOrNull()?.let {
            currentLogger.ex(it, msg)
            then(it)
        }
}