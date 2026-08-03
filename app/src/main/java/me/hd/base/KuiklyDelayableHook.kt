/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2026 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is an opensource software: you can redistribute it
 * and/or modify it under the terms of the General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the General Public License for more details.
 *
 * You should have received a copy of the General Public License
 * along with this software.
 * If not, see
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package me.hd.base

import com.github.kyuubiran.ezxhelper.utils.hookAfter
import io.github.qauxv.base.RuntimeErrorTracer
import io.github.qauxv.hook.BaseFunctionHook
import io.github.qauxv.util.Log
import io.github.qauxv.util.dexkit.DexKitTarget
import me.hd.util.name
import me.hd.util.parameters
import me.hd.util.singleMethod
import me.hd.util.toHostClass

abstract class KuiklyDelayableHook(
    hookKey: String? = null,
    defaultEnabled: Boolean = false,
    targets: Array<DexKitTarget>? = null
) : BaseFunctionHook(hookKey, defaultEnabled, targets = targets) {

    abstract val kuiklyName: String

    @Throws(Throwable::class)
    abstract fun startHook(classLoader: ClassLoader): Boolean

    override fun initOnce(): Boolean {
        try {
            "com.tencent.kuikly.core.render.android.context.KuiklyRenderDexContextHandler".toHostClass().singleMethod {
                name("getClassLoader")
                    && parameters(String::class.java)
            }.hookAfter { param ->
                val dexPath = param.args[0] as String
                Log.i("kuikly dexPath: $dexPath")
                val classLoader = param.result as ClassLoader
                if (dexPath.endsWith(kuiklyName)) {
                    Log.i("kuikly startHook: $dexPath")
                    startHook(classLoader)
                }
            }
        } catch (e: Exception) {
            traceError(e)
        }
        return true
    }

    override val runtimeErrorDependentComponents: List<RuntimeErrorTracer>?
        get() = null
}
