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

import androidx.annotation.CallSuper
import io.github.qauxv.base.IDynamicHook
import io.github.qauxv.hook.BaseFunctionHook
import io.github.qauxv.step.Step
import io.github.qauxv.util.dexkit.DexKitTarget

abstract class BaseDecorator(
        hookKey: String?,
        defaultEnabled: Boolean,
        dexDeobfIndexes: Array<DexKitTarget>?
) : BaseFunctionHook(hookKey, defaultEnabled, dexDeobfIndexes) {

    protected abstract val dispatcher: IDynamicHook

    @CallSuper
    override fun initOnce(): Boolean {
        return dispatcher.initialize()
    }

    override val isInitialized: Boolean get() = dispatcher.isInitialized

    override val isInitializationSuccessful: Boolean get() = dispatcher.isInitializationSuccessful

    override val isAvailable: Boolean get() = dispatcher.isAvailable

    override val targetProcesses: Int get() = dispatcher.targetProcesses

    override val isPreparationRequired: Boolean get() = dispatcher.isPreparationRequired || super.isPreparationRequired

    private fun <T> mergeArray(a1: Array<T>?, a2: Array<T>?): Array<T>? {
        if (a1.isNullOrEmpty()) return a2
        if (a2.isNullOrEmpty()) return a1
        return a1 + a2
    }

    override fun makePreparationSteps(): Array<Step>? {
        return mergeArray(super.makePreparationSteps(), dispatcher.makePreparationSteps())
    }
}
