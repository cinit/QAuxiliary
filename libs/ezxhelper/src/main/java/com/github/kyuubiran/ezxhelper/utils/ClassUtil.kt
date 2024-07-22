package com.github.kyuubiran.ezxhelper.utils

import com.github.kyuubiran.ezxhelper.init.InitFields
import io.github.qauxv.util.xpcompat.XposedHelpers

/**
 * 通过模块加载类
 * @param clzName 类名
 * @param clzLoader 类加载器
 * @return 被加载的类
 * @throws IllegalArgumentException 类名为空
 * @throws ClassNotFoundException 未找到类
 */
fun loadClass(clzName: String, clzLoader: ClassLoader = InitFields.ezXClassLoader): Class<*> {
    if (clzName.isBlank()) throw IllegalArgumentException("Class name must not be null or empty!")
    return clzLoader.loadClass(clzName)
}

/**
 * 尝试加载列表中的一个类
 * @param clzName 类名
 * @param clzLoader 类加载器
 * @return 第一个成功被加载的类
 */
fun loadClassAny(
    vararg clzName: String,
    clzLoader: ClassLoader = InitFields.ezXClassLoader
): Class<*> = clzName.firstNotNullOfOrNull { loadClassOrNull(it, clzLoader) }
    ?: throw ClassNotFoundException()

/**
 * 尝试加载列表中的一个类 失败则返回null
 * @param clzName 类名
 * @param clzLoader 类加载器
 * @return 第一个成功被加载的类或者null
 */
fun loadClassAnyOrNull(
    vararg clzName: String,
    clzLoader: ClassLoader = InitFields.ezXClassLoader
): Class<*>? = clzName.firstNotNullOfOrNull { loadClassOrNull(it, clzLoader) }

/**
 * 尝试加载一个类 如果失败则返回null
 * @param clzName 类名
 * @param clzLoader 类加载器
 * @return 被加载的类
 */
fun loadClassOrNull(
    clzName: String,
    clzLoader: ClassLoader = InitFields.ezXClassLoader
): Class<*>? {
    if (clzName.isBlank()) throw IllegalArgumentException("Class name must not be null or empty!")
    return XposedHelpers.findClassIfExists(clzName, clzLoader)
}

/**
 * 扩展函数 加载数组中的所有类
 * @param clzLoader 类加载器
 * @return 类数组
 */
fun Array<String>.loadAllClasses(clzLoader: ClassLoader = InitFields.ezXClassLoader): Array<Class<*>> {
    return Array(this.size) { i -> loadClass(this[i], clzLoader) }
}

fun Iterable<String>.loadAllClasses(clzLoader: ClassLoader = InitFields.ezXClassLoader): List<Class<*>> {
    return this.map { loadClass(it, clzLoader) }
}

/**
 * 扩展函数 尝试加载数组中的所有类
 * @param clzLoader 类加载器
 * @return 加载成功的类数组
 */
fun Array<String>.loadClassesIfExists(clzLoader: ClassLoader = InitFields.ezXClassLoader): Array<Class<*>> {
    return this.mapNotNull { loadClassOrNull(it, clzLoader) }.toTypedArray()
}

fun Iterable<String>.loadClassesIfExists(clzLoader: ClassLoader = InitFields.ezXClassLoader): List<Class<*>> {
    return this.mapNotNull { loadClassOrNull(it, clzLoader) }
}

/**
 * 尝试加载数组中的一个类
 * @param clzLoader 类加载器
 * @return 第一个成功被加载的类
 */
@JvmName("loadClassAnyFromArray")
fun Array<String>.loadClassAny(clzLoader: ClassLoader = InitFields.ezXClassLoader): Class<*> =
    this.firstNotNullOfOrNull { loadClassOrNull(it, clzLoader) } ?: throw ClassNotFoundException()

fun Iterable<String>.loadClassAny(clzLoader: ClassLoader = InitFields.ezXClassLoader): Class<*> =
    this.firstNotNullOfOrNull { loadClassOrNull(it, clzLoader) } ?: throw ClassNotFoundException()

/**
 * 尝试加载数组中的一个类 失败则返回null
 * @param clzLoader 类加载器
 * @return 第一个成功被加载的类或者null
 */
@JvmName("loadClassAnyOrFromList")
fun Array<String>.loadClassAnyOrNull(clzLoader: ClassLoader = InitFields.ezXClassLoader): Class<*>? =
    this.firstNotNullOfOrNull { loadClassOrNull(it, clzLoader) }

fun Iterable<String>.loadClassAnyOrNull(clzLoader: ClassLoader = InitFields.ezXClassLoader): Class<*>? =
    this.firstNotNullOfOrNull { loadClassOrNull(it, clzLoader) }

/**
 * 扩展函数 判断自身是否为某个类的子类
 * @param clzName 类名
 * @param clzLoader 类加载器
 * @return 是否为子类
 */
fun Class<*>.isChildClassOf(clzName: String, clzLoader: ClassLoader = InitFields.ezXClassLoader): Boolean =
    loadClass(clzName, clzLoader).isAssignableFrom(this)