package com.github.kyuubiran.ezxhelper.utils

import com.github.kyuubiran.ezxhelper.init.InitFields.ezXClassLoader
import java.io.Serializable
import java.lang.reflect.Field
import java.lang.reflect.Method

internal class DexDescriptor private constructor(sig: String, type: TYPE) :
    Serializable, Cloneable {
    private var name: String
    private var declaringClass: String
    private var signature: String

    init {
        when (type) {
            TYPE.FIELD -> {
                val retIdx: Int = sig.indexOf("->")
                val typeIdx: Int = sig.indexOf(':', retIdx)
                declaringClass = sig.substring(0, retIdx)
                name = sig.substring(retIdx + 2, typeIdx)
                signature = sig.substring(typeIdx + 1)
            }
            TYPE.METHOD -> {
                val retIdx: Int = sig.indexOf("->")
                val argsIdx: Int = sig.indexOf('(', retIdx)
                declaringClass = sig.substring(0, retIdx)
                name = sig.substring(retIdx + 2, argsIdx)
                signature = sig.substring(argsIdx)
            }
        }
    }

    companion object {
        private enum class TYPE {
            METHOD, FIELD
        }

        fun newMethodDesc(sig: String): DexDescriptor {
            return DexDescriptor(sig, TYPE.METHOD)
        }

        fun newFieldDesc(sig: String): DexDescriptor {
            return DexDescriptor(sig, TYPE.FIELD)
        }
    }


    override fun toString(): String {
        return "$declaringClass->$name$signature"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return if (other == null || javaClass != other.javaClass) false else toString() == other.toString()
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }

    private fun getDeclaringClassName(): String {
        return declaringClass.substring(1, declaringClass.length - 1).replace('/', '.')
    }

    private fun getTypeSig(type: Class<*>): String {
        if (type.isPrimitive) {
            return when (type.name) {
                Void.TYPE.name -> "V"
                Integer.TYPE.name -> "I"
                java.lang.Boolean.TYPE.name -> "Z"
                java.lang.Byte.TYPE.name -> "B"
                java.lang.Long.TYPE.name -> "L"
                java.lang.Float.TYPE.name -> "F"
                java.lang.Double.TYPE.name -> "D"
                Character.TYPE.name -> "C"
                java.lang.Short.TYPE.name -> "S"
                else -> throw IllegalStateException("Type: " + type.name + " is not a primitive type")
            }
        }
        return if (type.isArray) "[" + getTypeSig(type.componentType!!)
        else "L" + type.name.replace('.', '/') + ";"
    }

    private fun getMethodTypeDesc(method: Method): String {
        return buildString {
            append("(")
            method.parameterTypes.forEach {
                append(getTypeSig(it))
            }
            append(")")
            append(getTypeSig(method.returnType))
        }
    }

    internal fun getMethod(clzLoader: ClassLoader = ezXClassLoader): Method {
        try {
            var clz =
                loadClass(
                    declaringClass.substring(1, declaringClass.length - 1).replace('/', '.'),
                    clzLoader
                )
            clz.declaredMethods.forEach { m ->
                if (m.name == name && getMethodTypeDesc(m) == signature) return m
            }
            while (clz.superclass?.also { clz = it } != null) {
                clz.declaredMethods.forEach { m ->
                    if (m.isPrivate || m.isStatic) return@forEach
                    if (m.name == name && getMethodTypeDesc(m) == signature) return m
                }
            }
            throw NoSuchMethodException("$declaringClass->$name$signature")
        } catch (e: ClassNotFoundException) {
            throw NoSuchMethodException("$declaringClass->$name$signature").initCause(e)
        }
    }

    internal fun getField(clzLoader: ClassLoader = ezXClassLoader): Field {
        try {
            var clz =
                loadClass(
                    declaringClass.substring(1, declaringClass.length - 1).replace('/', '.'),
                    clzLoader
                )
            clz.declaredFields.forEach { f ->
                if (f.name == name && getTypeSig(f.type) == signature) return f
            }
            while (clz.superclass?.also { clz = it } != null) {
                clz.declaredFields.forEach { f ->
                    if (f.isPrivate || f.isStatic) return@forEach
                    if (f.name == name && getTypeSig(f.type) == signature) return f
                }
            }
            throw NoSuchFieldException("$declaringClass->$name$signature")
        } catch (e: ClassNotFoundException) {
            throw NoSuchFieldException("$declaringClass->$name$signature").initCause(e)
        }
    }
}