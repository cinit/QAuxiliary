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

package me.ketal.util

import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedBridge
import io.github.qauxv.util.dexkit.DexFieldDescriptor
import io.github.qauxv.util.dexkit.DexMethodDescriptor
import io.github.qauxv.util.Initiator
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal fun String.findClass(classLoader: ClassLoader, init: Boolean = false): Class<*> =
    Class.forName(this, init, classLoader)

internal fun String.getMethod(classLoader: ClassLoader = Initiator.getHostClassLoader()) =
    try {
        DexMethodDescriptor(this).getMethodInstance(classLoader)
    } catch (e: Throwable) {
        null
    }

internal fun Array<String>.getMethod(classLoader: ClassLoader = Initiator.getHostClassLoader()): Method? {
    this.forEach {
        it.getMethod(classLoader)?.apply {
            return this
        }
    }
    return null
}

internal fun String.getField(classLoader: ClassLoader = Initiator.getHostClassLoader()) =
    try {
        DexFieldDescriptor(this).getFieldInstance(classLoader)
    } catch (e: Throwable) {
        null
    }

internal fun Array<String>.getField(classLoader: ClassLoader = Initiator.getHostClassLoader()): Field? {
    this.forEach {
        it.getField(classLoader)?.apply {
            return this
        }
    }
    return null
}

internal fun String.hookMethod(callback: XC_MethodHook) = getMethod()?.hookMethod(callback)

internal fun Method.hookMethod(callback: XC_MethodHook) {
    XposedBridge.hookMethod(this, callback)
}

class FieldDelegate <T> (
    private val obj: Any,
    private val initializer: (KProperty<*>) -> Field
) : ReadWriteProperty<Any?, T> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val field = initializer(property).apply { isAccessible = true }
        return field.get(obj) as T
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val field = initializer(property).apply { isAccessible = true }
        field.set(obj, value)
    }
}

internal fun <T> field(obj: Any) = FieldDelegate<T>(obj) { property->
    obj.javaClass.getDeclaredField(property.name)
}

internal fun <T> field(obj: Any, field: Field) = FieldDelegate<T>(obj) { field }
