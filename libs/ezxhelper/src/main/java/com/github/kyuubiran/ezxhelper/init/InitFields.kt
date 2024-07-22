package com.github.kyuubiran.ezxhelper.init

import android.content.Context
import android.content.res.Resources
import io.github.qauxv.util.hostInfo

object InitFields {
    /**
     * 宿主全局AppContext
     */
    val appContext: Context
        get() = hostInfo.application

    /**
     * 调用本库加载类函数时使用的类加载器
     */
    lateinit var ezXClassLoader: ClassLoader
        internal set

}
