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

package io.github.qauxv.dsl.item

import android.content.Context
import android.view.View
import io.github.qauxv.dsl.item.impl.HeaderItem
import io.github.qauxv.dsl.item.impl.SpacerItem

open class CategoryItem(
        val titleString: String,
        private val initializer: (CategoryItem.() -> Unit)?
) : DslTMsgListItemInflatable {

    private val dslItems = ArrayList<DslTMsgListItemInflatable>()
    private lateinit var listItems: ArrayList<TMsgListItem>
    private var isAfterBuild: Boolean = false

    override fun inflateTMsgListItems(context: Context): List<TMsgListItem> {
        if (!::listItems.isInitialized) {
            initializer?.invoke(this)
            isAfterBuild = true
            listItems = ArrayList()
            listItems.add(HeaderItem(titleString))
            dslItems.forEach {
                listItems.addAll(it.inflateTMsgListItems(context))
            }
            listItems.add(SpacerItem())
        }
        return listItems.toMutableList()
    }

    open fun description(
            text: CharSequence,
            isTextSelectable: Boolean = false,
    ) = DescriptionItem(text, isTextSelectable).also {
        checkState()
        dslItems.add(it)
    }

    open fun textItem(
            title: String,
            summary: String? = null,
            value: String? = null,
            onClick: View.OnClickListener? = null
    ) {
        checkState()
        dslItems.add(TextListItem(title, summary, value, onClick))
    }

    @JvmOverloads
    open fun add(item: DslTMsgListItemInflatable, index: Int = -1) {
        checkState()
        if (index < 0) {
            dslItems.add(item)
        } else {
            dslItems.add(index, item)
        }
    }

    open fun add(items: List<DslTMsgListItemInflatable>) {
        checkState()
        dslItems.addAll(items)
    }

    private fun checkState() {
        if (isAfterBuild) {
            throw IllegalStateException("you can't add item after build")
        }
    }
}
