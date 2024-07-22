package com.github.kyuubiran.ezxhelper.utils

import java.lang.reflect.Constructor
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * 扩展属性 判断是否为Static
 */
val Member.isStatic: Boolean
    inline get() = Modifier.isStatic(this.modifiers)
val Member.isNotStatic: Boolean
    inline get() = !this.isStatic

val Class<*>.isStatic: Boolean
    inline get() = Modifier.isStatic(this.modifiers)
val Class<*>.isNotStatic: Boolean
    inline get() = !this.isStatic

/**
 * 扩展属性 判断是否为Public
 */
val Member.isPublic: Boolean
    inline get() = Modifier.isPublic(this.modifiers)
val Member.isNotPublic: Boolean
    inline get() = !this.isPublic

val Class<*>.isPublic: Boolean
    inline get() = Modifier.isPublic(this.modifiers)
val Class<*>.isNotPublic: Boolean
    inline get() = !this.isPublic

/**
 * 扩展属性 判断是否为Protected
 */
val Member.isProtected: Boolean
    inline get() = Modifier.isProtected(this.modifiers)
val Member.isNotProtected: Boolean
    inline get() = !this.isProtected

val Class<*>.isProtected: Boolean
    inline get() = Modifier.isProtected(this.modifiers)
val Class<*>.isNotProtected: Boolean
    inline get() = !this.isProtected

/**
 * 扩展属性 判断是否为Private
 */
val Member.isPrivate: Boolean
    inline get() = Modifier.isPrivate(this.modifiers)
val Member.isNotPrivate: Boolean
    inline get() = !this.isPrivate

val Class<*>.isPrivate: Boolean
    inline get() = Modifier.isPrivate(this.modifiers)
val Class<*>.isNotPrivate: Boolean
    inline get() = !this.isPrivate

/**
 * 扩展属性 判断是否为Final
 */
val Member.isFinal: Boolean
    inline get() = Modifier.isFinal(this.modifiers)
val Member.isNotFinal: Boolean
    inline get() = !this.isFinal

val Class<*>.isFinal: Boolean
    inline get() = Modifier.isFinal(this.modifiers)
val Class<*>.isNotFinal: Boolean
    inline get() = !this.isFinal

/**
 * 扩展属性 判断是否为Native
 */
val Member.isNative: Boolean
    inline get() = Modifier.isNative(this.modifiers)
val Member.isNotNative: Boolean
    inline get() = !this.isNative

/**
 * 扩展属性 判断是否为Synchronized
 */
val Member.isSynchronized: Boolean
    inline get() = Modifier.isSynchronized(this.modifiers)
val Member.isNotSynchronized: Boolean
    inline get() = !this.isSynchronized

/**
 * 扩展属性 判断是否为Abstract
 */
val Member.isAbstract: Boolean
    inline get() = Modifier.isAbstract(this.modifiers)
val Member.isNotAbstract: Boolean
    inline get() = !this.isAbstract

val Class<*>.isAbstract: Boolean
    inline get() = Modifier.isAbstract(this.modifiers)
val Class<*>.isNotAbstract: Boolean
    inline get() = !this.isAbstract

/**
 * 扩展属性 判断是否为Transient
 */
val Member.isTransient: Boolean
    inline get() = Modifier.isTransient(this.modifiers)
val Member.isNotTransient: Boolean
    inline get() = !this.isTransient

/**
 * 扩展属性 判断是否为Volatile
 */
val Member.isVolatile: Boolean
    inline get() = Modifier.isVolatile(this.modifiers)
val Member.isNotVolatile: Boolean
    inline get() = !this.isVolatile

/**
 * 扩展属性 获取方法的参数数量
 */
val Method.paramCount: Int
    inline get() = this.parameterTypes.size

/**
 * 扩展属性 获取构造方法的参数数量
 */
val Constructor<*>.paramCount: Int
    inline get() = this.parameterTypes.size

/**
 * 扩展属性 判断方法的参数是否为空
 */
val Method.emptyParam: Boolean
    inline get() = this.paramCount == 0
val Method.notEmptyParam: Boolean
    inline get() = this.paramCount != 0

/**
 * 扩展属性 判断构造方法的参数是否为空
 */
val Constructor<*>.emptyParam: Boolean
    inline get() = this.paramCount == 0
val Constructor<*>.notEmptyParam: Boolean
    inline get() = this.paramCount != 0
