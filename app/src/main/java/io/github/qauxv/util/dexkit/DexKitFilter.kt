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

import com.github.kyuubiran.ezxhelper.utils.isAbstract
import com.github.kyuubiran.ezxhelper.utils.isStatic
import io.github.qauxv.util.Initiator

typealias dexkitFilter = (DexMethodDescriptor) -> Boolean

infix fun dexkitFilter.or(other: dexkitFilter) = { it: DexMethodDescriptor -> this(it) || other(it) }
infix fun dexkitFilter.and(other: dexkitFilter) = { it: DexMethodDescriptor -> this(it) && other(it) }

object DexKitFilter {
    val allStaticFields = filter@{ it: DexMethodDescriptor ->
        val clz = Initiator.load(it.declaringClass) ?: return@filter false
        !clz.isAbstract && clz.fields.all { it.isStatic }
    }

    val hasSuper = filter@{ it: DexMethodDescriptor ->
        val clz = Initiator.load(it.declaringClass) ?: return@filter false
        !clz.isEnum && !clz.isAbstract && clz.superclass != Any::class.java
    }

    val notHasSuper = filter@{ it: DexMethodDescriptor ->
        val clz = Initiator.load(it.declaringClass) ?: return@filter false
        !clz.isEnum && !clz.isAbstract && clz.superclass == Any::class.java
    }

    val allowAll = { _: DexMethodDescriptor -> true }

    val clinit = filter@{ it: DexMethodDescriptor ->
        it.name == "<clinit>"
    }

    val defpackage = filter@{ it: DexMethodDescriptor ->
        val clz = Initiator.load(it.declaringClass) ?: return@filter false
        !clz.name.contains(".")
    }

    fun strInClsName(str: String, fullMatch: Boolean = false): dexkitFilter = { it: DexMethodDescriptor ->
        val pattern = str.replace(".", "/")
        if (fullMatch) pattern == it.declaringClass else pattern in it.declaringClass
    }

    fun strInSig(str: String, fullMatch: Boolean = false): dexkitFilter = { it: DexMethodDescriptor ->
        if (fullMatch) str == it.signature else str in it.signature
    }

    fun filterByParams(filter: (Array<Class<*>>) -> Boolean): dexkitFilter = filter@{ it: DexMethodDescriptor ->
        val m = kotlin.runCatching { it.getMethodInstance(Initiator.getHostClassLoader()) }.getOrNull() ?: return@filter false
        filter(m.parameterTypes)
    }
}
