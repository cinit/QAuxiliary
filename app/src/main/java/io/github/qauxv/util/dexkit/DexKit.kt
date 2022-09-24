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

package io.github.qauxv.util.dexkit

import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Initiator.getHostClassLoader
import io.github.qauxv.util.Log
import java.lang.reflect.Method

object DexKit {
    private const val NO_SUCH_CLASS = "Lio/github/qauxv/util/DexKit\$NoSuchClass;"
    val NO_SUCH_METHOD = DexMethodDescriptor(NO_SUCH_CLASS, "a", "()V")

    fun tryFind(target: DexKitTarget) : Boolean {
        return kotlin.runCatching {
            when (target) {
                is DexKitTarget.UsingStr -> {
                    doFindClass(target) != null
                }
            }
        }.isSuccess
    }

    /**
     * Test whether we should run the dex deobfuscation.
     * <p>
     * Note that if a dex class is tried to deobfuscate before, but failed, its failed result will be cached,
     * which means that the same dex class will not be deobfuscated again.
     *
     * @param target the dex class target
     * @return true if time is required to deobfuscate the dex class, false if either the dex class is already
     * found or there was already a failed result.
     */
    fun isRunDexDeobfuscationRequired(target: DexKitTarget) : Boolean {
        when (target) {
            is DexKitTarget.UsingStr -> {
                if (target.findMethod) {
                    if (loadClassFromCache(target) != null) return false
                }
                return getMethodDescFromCache(target) == null
            }
        }
    }

    fun doFindClass(target: DexKitTarget): Class<*>? {
        loadClassFromCache(target)?.let { return it }
        return DexDeobfsProvider.getCurrentBackend().doFindClass(target)
    }

    fun doFindMethod(target: DexKitTarget): Method? {
        when (target) {
            is DexKitTarget.UsingStr -> {
                check(target.findMethod) { "$target attempted to access method!" }
                loadMethodFromCache(target)?.let { return it }
                return DexDeobfsProvider.getCurrentBackend().doFindMethod(target)
            }
        }
    }

    fun getMethodDescFromCache(target: DexKitTarget): DexMethodDescriptor? {
        return kotlin.runCatching {
            DexMethodDescriptor(target.descCache)
        }.getOrNull()
    }

    fun loadClassFromCache(target: DexKitTarget): Class<*>? {
        when (target) {
            is DexKitTarget.UsingStr -> {
                Initiator.load(target.declaringClass)?.let { return it }
                return getMethodDescFromCache(target)?.let { Initiator.load(it.declaringClass) }
            }
        }
    }

    fun loadMethodFromCache(target: DexKitTarget): Method? {
        when (target) {
            is DexKitTarget.UsingStr -> {
                check(target.findMethod) { "$target attempted to access method!" }
                val cache = getMethodDescFromCache(target) ?: return null
                if (NO_SUCH_METHOD.toString() == cache.toString()) return null
                if ("<init>" in cache.name || "<clinit>" in cache.name) {
                    // TODO: support constructors
                    Log.i("getMethodFromCache($target) methodName == ${cache.name} , return null")
                    return null
                }
                return kotlin.runCatching {
                    cache.getMethodInstance(getHostClassLoader())
                }.onFailure { t -> Log.e(t) }.getOrNull()
            }
        }
    }
}
