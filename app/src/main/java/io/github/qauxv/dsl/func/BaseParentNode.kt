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

abstract class BaseParentNode : IDslParentNode {

    protected open val mChildren: MutableList<IDslItemNode> = mutableListOf()
    override val children: List<IDslItemNode> get() = mChildren

    override val isSearchable: Boolean = true

    override fun findChildById(id: String): IDslItemNode? {
        return mChildren.find { it.identifier == id }
    }

    override fun getChildAt(index: Int): IDslItemNode {
        return mChildren[index]
    }

    override fun addChild(child: IDslItemNode, index: Int) {
        // check if child with same id exists
        if (mChildren.find { it.identifier == child.identifier } != null) {
            throw IllegalArgumentException("child with id '${child.identifier}' already exists")
        }
        if (index < 0 || index > mChildren.size) {
            mChildren.add(child)
        } else {
            mChildren.add(index, child)
        }
    }

    override fun removeChild(child: IDslItemNode) {
        mChildren.remove(child)
    }

    override fun removeChildAt(index: Int) {
        mChildren.removeAt(index)
    }

    override fun removeAllChildren() {
        mChildren.clear()
    }

    override fun lookupHierarchy(ids: Array<String>): IDslItemNode? {
        if (ids.isEmpty() || (ids.size == 1 && ids[0] == "")) {
            return this
        }
        val child = findChildById(ids[0]) ?: return null
        if (ids.size == 1) {
            return child
        }
        // recursive lookup
        if (child is IDslParentNode) {
            return child.lookupHierarchy(ids.copyOfRange(1, ids.size))
        }
        return null
    }

    override fun findLocationByIdentifier(identifier: String): Array<String>? {
        if (identifier == this.identifier) {
            // self, return empty array
            return arrayOf()
        }
        for (i in 0 until mChildren.size) {
            val child = mChildren[i]
            if (child.identifier == identifier) {
                // found
                return arrayOf(child.identifier)
            }
            if (child is IDslParentNode) {
                val location = child.findLocationByIdentifier(identifier)
                if (location != null) {
                    // found
                    return arrayOf(child.identifier, *location)
                }
            }
        }
        // not found
        return null
    }
}
