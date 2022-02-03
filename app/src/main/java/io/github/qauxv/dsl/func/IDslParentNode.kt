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

interface IDslParentNode : IDslItemNode {
    val children: List<IDslItemNode>

    fun findChildById(id: String): IDslItemNode?

    fun getChildAt(index: Int): IDslItemNode

    fun addChild(child: IDslItemNode, index: Int = -1)

    fun removeChild(child: IDslItemNode)

    fun removeChildAt(index: Int)

    fun removeAllChildren()

    fun lookupHierarchy(ids: Array<String>): IDslItemNode?

    fun findLocationByIdentifier(identifier: String): Array<String>?
}
