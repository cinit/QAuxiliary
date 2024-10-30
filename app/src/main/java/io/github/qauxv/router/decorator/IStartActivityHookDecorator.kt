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
package io.github.qauxv.router.decorator

import android.content.Intent
import io.github.qauxv.base.ITraceableDynamicHook
import io.github.qauxv.base.RuntimeErrorTracer
import io.github.qauxv.router.dispacher.StartActivityHook
import io.github.qauxv.util.xpcompat.XC_MethodHook

interface IStartActivityHookDecorator : ITraceableDynamicHook {

    /**
     * Called before when [android.content.Context.startActivity] is called with [Intent]
     * @param intent the [Intent] to start activity
     * @param param the [XC_MethodHook] hook parameter
     * @return true if you don't want to call the other hooks
     * Note: you need to call [XC_MethodHook.MethodHookParam.setResult] to prevent the original method to be called
     */
    @Throws(Throwable::class)
    fun onStartActivityIntent(intent: Intent, param: XC_MethodHook.MethodHookParam): Boolean

    override val runtimeErrorDependentComponents: List<RuntimeErrorTracer>?
        get() = listOf(StartActivityHook)

}
