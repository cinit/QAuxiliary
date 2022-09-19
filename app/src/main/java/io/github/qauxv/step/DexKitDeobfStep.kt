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

package io.github.qauxv.step

import com.github.kyuubiran.ezxhelper.utils.Log
import io.github.qauxv.core.HookInstaller
import io.github.qauxv.util.dexkit.DexDeobfsProvider
import io.github.qauxv.util.dexkit.DexKitFinder

class DexKitDeobfStep : Step {

    private val dexKitFinderHook = mutableListOf<DexKitFinder>()

    init {
        if (DexDeobfsProvider.isDexKitBackend) {
            HookInstaller.queryAllAnnotatedHooks().forEach {
                if (it is DexKitFinder && it.isEnabled && it.isAvailable && it.isNeedFind) {
                    dexKitFinderHook.add(it)
                }
            }
        }
    }

    override fun step(): Boolean {
        dexKitFinderHook.forEach {
            it.isNeedFind && it.doFind()
        }
        return true
    }

    override fun isDone(): Boolean {
        return dexKitFinderHook.all { !it.isNeedFind }
    }

    override fun getPriority() = 99

    override fun getDescription() = "DexKit deobfuscation"
}
