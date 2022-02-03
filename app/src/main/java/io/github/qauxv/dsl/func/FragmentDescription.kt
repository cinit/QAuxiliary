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

package io.github.qauxv.dsl.func

import io.github.qauxv.base.IUiItemAgentProvider

open class FragmentDescription(
    override val identifier: String,
    override val name: String?,
    categoryTitleSearchable: Boolean = true,
    initializer: (FragmentDescription.() -> Unit)?
) : BaseParentNode() {

    init {
        initializer?.invoke(this)
    }

    override val isSearchable: Boolean = categoryTitleSearchable

    open fun category(
        identifier: String,
        name: String,
        categoryTitleSearchable: Boolean = true,
        initializer: (CategoryDescription.() -> Unit)? = null
    ): CategoryDescription = CategoryDescription(name, identifier, categoryTitleSearchable, initializer).also {
        addChild(it)
    }

    open fun fragment(
        identifier: String,
        name: String,
        categoryTitleSearchable: Boolean = true,
        initializer: (FragmentDescription.() -> Unit)? = null
    ): FragmentDescription = FragmentDescription(identifier, name, categoryTitleSearchable, initializer).also {
        addChild(it)
    }

    open fun agentItem(targetItem: IUiItemAgentProvider): UiItemAgentDescription = UiItemAgentDescription(targetItem).also {
        addChild(it)
    }

}
