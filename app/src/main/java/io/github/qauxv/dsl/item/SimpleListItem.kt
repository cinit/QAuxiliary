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
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.qauxv.dsl.cell.TitleValueCell
import io.github.qauxv.dsl.func.IDslItemNode

class SimpleListItem(
        override val identifier: String,
        override val name: String,
        val description: String?,
) : IDslItemNode, TMsgListItem {

    override var isSearchable: Boolean = true
    override var isEnabled: Boolean = true
    override var isClickable: Boolean = isEnabled
    override var isVoidBackground: Boolean = false
    var onClickListener: ((v: View) -> Unit)? = null

    class HeaderViewHolder(cell: TitleValueCell) : RecyclerView.ViewHolder(cell)

    override fun createViewHolder(context: Context, parent: ViewGroup): RecyclerView.ViewHolder {
        return HeaderViewHolder(TitleValueCell(context))
    }

    override fun bindView(viewHolder: RecyclerView.ViewHolder, position: Int, context: Context) {
        val cell = viewHolder.itemView as TitleValueCell
        cell.title = name
        cell.summary = description
        cell.isHasSwitch = false
        cell.hasDivider = true
    }

    override fun onItemClick(v: View, position: Int, x: Int, y: Int) {
        onClickListener?.invoke(v)
    }

    override val isLongClickable: Boolean = false

    override fun onLongClick(v: View, position: Int, x: Int, y: Int): Boolean {
        // nop
        return false
    }
}
