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

import android.app.Activity
import android.content.Context
import android.view.View
import io.github.qauxv.base.IEntityAgent
import io.github.qauxv.base.ISwitchCellAgent
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.util.dexkit.DexKitTarget
import kotlinx.coroutines.flow.StateFlow

abstract class BaseSwitchFunctionDecorator(
        hookKey: String? = null,
        defaultEnabled: Boolean = false,
        dexDeobfIndexes: Array<DexKitTarget>? = null
) : BaseDecorator(hookKey, defaultEnabled, dexDeobfIndexes) {

    /**
     * Name of the function.
     */
    abstract val name: String

    /**
     * Description of the function.
     */
    open val description: CharSequence? = null

    open val extraSearchKeywords: Array<String>? = null

    override val uiItemAgent: IUiItemAgent by lazy { uiItemAgent() }

    private fun uiItemAgent() = object : IUiItemAgent {
        override val titleProvider: (IEntityAgent) -> String = { _ -> name }
        override val summaryProvider: (IEntityAgent, Context) -> CharSequence? = { _, _ -> description }
        override val valueState: StateFlow<String?>? = null
        override val validator: ((IUiItemAgent) -> Boolean) = { _ -> true }
        override val switchProvider: ISwitchCellAgent? by lazy {
            object : ISwitchCellAgent {
                override val isCheckable = true
                override var isChecked: Boolean
                    get() = isEnabled
                    set(value) {
                        if (value != isEnabled) {
                            isEnabled = value
                        }
                    }
            }
        }
        override val onClickListener: ((IUiItemAgent, Activity, View) -> Unit)? = null
        override val extraSearchKeywordProvider: ((IUiItemAgent, Context) -> Array<String>?)?
            get() = extraSearchKeywords?.let { { _, _ -> it } }
    }
}
