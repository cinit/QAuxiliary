@file:Suppress("NOTHING_TO_INLINE")

package com.github.kyuubiran.ezxhelper.utils

/*
import com.github.kyuubiran.ezxhelper.init.InitFields.ezXClassLoader

inline fun String.loadClass(cl: ClassLoader = ezXClassLoader) = loadClass(this, cl)
inline fun String.loadClassOrNull(cl: ClassLoader = ezXClassLoader) = loadClassOrNull(this, cl)

inline fun String.getDeclaredMethods(cl: ClassLoader = ezXClassLoader) =
    getDeclaredMethods(this, cl)

inline fun String.findMethod(
    cl: ClassLoader = ezXClassLoader,
    findSuper: Boolean = false,
    noinline condition: MethodCondition
) = findMethod(this, cl, findSuper, condition)

inline fun String.findMethodOrNull(
    cl: ClassLoader = ezXClassLoader,
    findSuper: Boolean = false,
    noinline condition: MethodCondition
) = findMethodOrNull(this, cl, findSuper, condition)

inline fun String.findAllMethods(
    cl: ClassLoader = ezXClassLoader,
    findSuper: Boolean = false,
    noinline condition: MethodCondition
) = findAllMethods(this, cl, findSuper, condition)

inline fun String.findField(
    cl: ClassLoader = ezXClassLoader,
    findSuper: Boolean = false,
    noinline condition: FieldCondition
) = findField(this, cl, findSuper, condition)

inline fun String.findFieldOrNull(
    cl: ClassLoader = ezXClassLoader,
    findSuper: Boolean = false,
    noinline condition: FieldCondition
) = findFieldOrNull(this, cl, findSuper, condition)

inline fun String.findAllFields(
    cl: ClassLoader = ezXClassLoader,
    findSuper: Boolean = false,
    noinline condition: FieldCondition
) = findAllFields(this, cl, findSuper, condition)

inline fun String.findConstructor(
    cl: ClassLoader = ezXClassLoader,
    noinline condition: ConstructorCondition
) = findConstructor(this, cl, condition)

inline fun String.findConstructorOrNull(
    cl: ClassLoader = ezXClassLoader,
    noinline condition: ConstructorCondition
) = findConstructorOrNull(this, cl, condition)

inline fun String.findAllConstructors(
    cl: ClassLoader = ezXClassLoader,
    noinline condition: ConstructorCondition
) = findAllConstructors(this, cl, condition)

inline fun String.getMethodByDesc(cl: ClassLoader = ezXClassLoader) = getMethodByDesc(this, cl)
inline fun String.getFieldByDesc(cl: ClassLoader = ezXClassLoader) = getFieldByDesc(this, cl)
 */

inline fun Class<*>.findMethod(
    findSuper: Boolean = false,
    noinline condition: MethodCondition
) = findMethod(this, findSuper, condition)

inline fun Class<*>.findMethodOrNull(
    findSuper: Boolean = false,
    noinline condition: MethodCondition
) = findMethodOrNull(this, findSuper, condition)

inline fun Class<*>.findAllMethods(
    findSuper: Boolean = false,
    noinline condition: MethodCondition
) = findAllMethods(this, findSuper, condition)

inline fun Class<*>.findField(
    findSuper: Boolean = false,
    noinline condition: FieldCondition
) = findField(this, findSuper, condition)

inline fun Class<*>.findFieldOrNull(
    findSuper: Boolean = false,
    noinline condition: FieldCondition
) = findFieldOrNull(this, findSuper, condition)

inline fun Class<*>.findAllFields(
    findSuper: Boolean = false,
    noinline condition: FieldCondition
) = findAllFields(this, findSuper, condition)

inline fun Class<*>.findConstructor(
    noinline condition: ConstructorCondition
) = findConstructor(this, condition)

inline fun Class<*>.findConstructorOrNull(
    noinline condition: ConstructorCondition
) = findConstructorOrNull(this, condition)

inline fun Class<*>.findAllConstructors(
    noinline condition: ConstructorCondition
) = findAllConstructors(this, condition)
