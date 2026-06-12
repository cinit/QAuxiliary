/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2026 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is an opensource software: you can redistribute it
 * and/or modify it under the terms of the General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the General Public License for more details.
 *
 * You should have received a copy of the General Public License
 * along with this software.
 * If not, see
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package me.hd.util

import io.github.qauxv.base.IDynamicHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.DexKitTarget
import io.github.qauxv.util.xpcompat.XC_MethodHook.MethodHookParam
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.hookBefore
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Modifier

internal fun String.toClass(
    clsLoader: ClassLoader
) = clsLoader.loadClass(this)

internal fun String.toHostClass(
) = toClass(Initiator.getHostClassLoader())

internal fun DexKitTarget.toHostClass(
) = DexKit.requireClassFromCache(this)

internal fun DexKitTarget.toHostMethod(
) = DexKit.requireMethodFromCache(this)


internal fun Class<*>.singleConstructor(
    condition: Constructor<*>.() -> Boolean
) = declaredConstructors.single {
    it.condition()
}.apply { isAccessible = true }

internal fun Any.singleConstructor(
    condition: Constructor<*>.() -> Boolean
) = javaClass.singleConstructor(condition)

internal fun Class<*>.singleMethod(
    condition: Method.() -> Boolean
) = declaredMethods.single {
    it.condition()
}.apply { isAccessible = true }

internal fun Any.singleMethod(
    condition: Method.() -> Boolean
) = javaClass.singleMethod(condition)

internal fun Class<*>.singleField(
    condition: Field.() -> Boolean
) = declaredFields.single {
    it.condition()
}.apply { isAccessible = true }

internal fun Any.singleField(
    condition: Field.() -> Boolean
) = javaClass.singleField(condition)


internal val Member.isPublic
    get() = Modifier.isPublic(modifiers)

internal val Member.isPrivate
    get() = Modifier.isPrivate(modifiers)

internal val Member.isStatic
    get() = Modifier.isStatic(modifiers)


internal fun Constructor<*>.parameterCount(
    count: Int
) = parameterTypes.size == count

internal fun Constructor<*>.parameters(
    vararg types: Class<*>?
) = parameterCount(types.size) && parameterTypes.indices.all { index ->
    types[index] == null || parameterTypes[index] == types[index]
}

internal fun Method.returnType(
    type: Class<*>
) = returnType == type

internal fun Method.name(
    name: String
) = this.name == name

internal fun Method.parameterCount(
    count: Int
) = parameterTypes.size == count

internal fun Method.parameters(
    vararg types: Class<*>?
) = parameterCount(types.size) && parameterTypes.indices.all { index ->
    types[index] == null || parameterTypes[index] == types[index]
}


internal fun Member.hookBeforeIfEnabled(
    baseHook: IDynamicHook,
    hooker: (MethodHookParam) -> Unit
) = hookBefore(baseHook, hooker)

internal fun Member.hookAfterIfEnabled(
    baseHook: IDynamicHook,
    hooker: (MethodHookParam) -> Unit
) = hookAfter(baseHook, hooker)
