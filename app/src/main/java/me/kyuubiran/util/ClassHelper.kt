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

package me.kyuubiran.util

import com.github.kyuubiran.ezxhelper.utils.getFieldByClassOrObject

enum class ClassHelper(private val clzName: String, private val indexes: Array<Int>) {
    UpgradeController1("com.tencent.mobileqq.upgrade.UpgradeController", arrayOf(1, 2)),
    UpgradeController2("com.tencent.mobileqq.app.upgrade.UpgradeController", arrayOf(1, 2));

    private fun tryLoadOrNull(): Class<*>? {
        runCatching {
            return loadClass(this.clzName)
        }
        this.indexes.forEach { idx ->
            runCatching {
                return loadClass("${this.clzName}\$${idx}").getFieldByClassOrObject("this$0").type
            }
        }
        return null
    }

    val clz: Class<*>? by lazy { tryLoadOrNull() }
}
