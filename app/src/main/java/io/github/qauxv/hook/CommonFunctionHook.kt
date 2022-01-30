/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
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

package io.github.qauxv.hook

import android.content.Context
import android.view.View
import io.github.qauxv.base.ISwitchCellAgent
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.Invalidatable

/**
 * A function that only has a enable/disable switch function.
 */
abstract class CommonFunctionHook(
    hookKey: String? = null,
    defaultEnabled: Boolean = true
) : BaseFunctionHook(hookKey, defaultEnabled) {

    /**
     * Name of the function.
     */
    abstract val name: String

    /**
     * Description of the function.
     */
    open val description: String? = null

    override val uiItemAgent: IUiItemAgent by lazy {
        object : IUiItemAgent {
            override val titleProvider: (IUiItemAgent, Context) -> String = { _, _ -> name }
            override val summaryProvider: (IUiItemAgent, Context) -> String? = { _, _ -> description }
            override val valueProvider: ((IUiItemAgent, Context) -> String?)? = null
            override val validator: ((IUiItemAgent, Context) -> Boolean) = { _, _ -> true }
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
            override val onClickListener: ((IUiItemAgent, Context, View, Invalidatable) -> Unit)? = null
            override val extraSearchKeywordProvider: ((IUiItemAgent, Context) -> List<String>?)? = null
        }
    }

    override val uiItemLocation: Array<String>
        get() = TODO("Not yet implemented")
}
