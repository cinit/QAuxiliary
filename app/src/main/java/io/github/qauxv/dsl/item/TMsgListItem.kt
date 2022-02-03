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

interface TMsgListItem : DslTMsgListItemInflatable {

    val isEnabled: Boolean

    val isVoidBackground: Boolean

    fun createViewHolder(context: Context, parent: ViewGroup): RecyclerView.ViewHolder

    fun bindView(viewHolder: RecyclerView.ViewHolder, position: Int, context: Context)

    fun onItemClick(v: View, position: Int, x: Int, y: Int)

    val isLongClickable: Boolean

    fun onLongClick(v: View, position: Int, x: Int, y: Int): Boolean

    override fun inflateTMsgListItems(context: Context): List<TMsgListItem> {
        return listOf(this)
    }
}
