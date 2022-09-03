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

import io.github.qauxv.base.IDynamicHook
import io.github.qauxv.hook.BaseFunctionHook
import io.github.qauxv.step.Step

abstract class BaseDecorator(
        hookKey: String?,
        defaultEnabled: Boolean,
        dexDeobfIndexes: IntArray?
) : BaseFunctionHook(hookKey, defaultEnabled, dexDeobfIndexes) {

    protected abstract val dispatcher: IDynamicHook

    override fun initOnce(): Boolean {
        return dispatcher.initialize()
    }

    override val isInitialized: Boolean get() = dispatcher.isInitialized

    override val isInitializationSuccessful: Boolean get() = dispatcher.isInitializationSuccessful

    override val runtimeErrors: List<Throwable>
        get() {
            val myErrors = super.runtimeErrors
            val dispatcherErrors = dispatcher.runtimeErrors
            return myErrors + dispatcherErrors
        }

    override val isAvailable: Boolean get() = dispatcher.isAvailable

    override val targetProcesses: Int get() = dispatcher.targetProcesses

    override val isPreparationRequired: Boolean get() = dispatcher.isPreparationRequired

    override fun makePreparationSteps(): Array<Step>? {
        return dispatcher.makePreparationSteps()
    }
}
