package com.github.kyuubiran.ezxhelper.utils

import android.app.Activity
import dalvik.system.BaseDexClassLoader
import java.util.*
import kotlin.system.exitProcess

/**
 * 尝试执行一块代码，如果成功返true，失败则返回false
 * @param block 执行的代码块
 * @return 成功为true，失败为false
 */
inline fun tryOrFalse(block: () -> Unit): Boolean = try {
    block()
    true
} catch (thr: Throwable) {
    false
}

/**
 * 尝试执行一块代码，如果失败则记录日志
 * @param block 执行的代码块
 */
inline fun tryOrLog(block: () -> Unit) = try {
    block()
} catch (thr: Throwable) {
    Log.e(thr)
}

/**
 * 尝试执行一块代码，如果成功返true，失败则返回false并且记录日志
 * @param block 执行的代码块
 * @return 成功为true，失败为false
 */
inline fun tryOrLogFalse(block: () -> Unit): Boolean = try {
    block()
    true
} catch (thr: Throwable) {
    Log.e(thr)
    false
}

/**
 * 尝试执行一块代码，如果成功返回代码块执行的结果，失败则返回null
 * @param block 执行的代码块
 * @return 成功返回代码块执行的返回值，失败返回null
 */
inline fun <T> tryOrNull(block: () -> T?): T? = try {
    block()
} catch (thr: Throwable) {
    null
}

/**
 * 尝试执行一块代码，如果成功返回代码块执行的结果，失败则返回null并且记录日志
 * @param block 执行的代码块
 * @return 成功返回代码块执行的返回值，失败返回null
 */
inline fun <T> tryOrLogNull(block: () -> T?): T? = try {
    block()
} catch (thr: Throwable) {
    Log.e(thr)
    null
}

/**
 * 扩展函数 保留可变列表中符合条件的元素
 * @param predicate 条件
 */
inline fun <E> MutableList<E>.retainIf(predicate: ((E) -> Boolean)) {
    this.filter { elem -> predicate(elem) }.forEach { this.remove(it) }
}

/**
 * 扩展函数 保留可变列表中符合条件的元素 并返回可变列表
 * @param predicate 条件
 * @return 保留符合条件的元素之后的可变列表
 */
inline fun <E> MutableList<E>.applyRetainIf(predicate: (E) -> Boolean): MutableList<E> {
    this.retainIf(predicate)
    return this
}

/**
 * 扩展函数 保留可变集合中符合条件的元素
 * @param predicate 条件
 */
inline fun <E> MutableSet<E>.retainIf(predicate: (E) -> Boolean) {
    this.filter { elem -> predicate(elem) }.forEach { this.remove(it) }
}

/**
 * 扩展函数 保留可变集合中符合条件的元素 并返回可变集合
 * @param predicate 条件
 * @return 保留符合条件的元素之后的可变集合
 */
inline fun <E> MutableSet<E>.applyRetainIf(predicate: (E) -> Boolean): MutableSet<E> {
    this.retainIf(predicate)
    return this
}

/**
 * 扩展函数 保留可变字典中符合条件的元素
 * @param predicate 条件
 */
inline fun <K, V> MutableMap<K, V>.retainIf(predicate: (K, V) -> Boolean) {
    this.filter { (key, value) -> predicate(key, value) }.forEach { this.remove(it.key) }
}

/**
 * 扩展函数 保留可变字典中符合条件的元素 并返回可变字典
 * @param predicate 条件
 * @return 保留符合条件的元素之后的可变字典
 */
inline fun <K, V> MutableMap<K, V>.applyRetainIf(predicate: (K, V) -> Boolean): MutableMap<K, V> {
    this.retainIf(predicate)
    return this
}

/**
 * 扩展函数 移除可变字典中符合条件的元素
 * @param predicate 条件
 */
inline fun <K, V> MutableMap<K, V>.removeIf(predicate: (K, V) -> Boolean) {
    this.filter { (key, value) -> predicate(key, value) }.forEach { this.remove(it.key) }
}

/**
 * 扩展函数 移除可变字典中符合条件的元素 并返回可变字典
 * @param predicate 条件
 * @return 移除符合条件的元素之后的可变字典
 */
inline fun <K, V> MutableMap<K, V>.applyRemoveIf(
    predicate: (K, V) -> Boolean
): MutableMap<K, V> {
    this.removeIf(predicate)
    return this
}

/**
 * 取自 哔哩漫游
 * 查找DexClassLoader
 * @see `https://github.com/yujincheng08/BiliRoaming`
 */
inline fun ClassLoader.findDexClassLoader(crossinline delegator: (BaseDexClassLoader) -> BaseDexClassLoader = { x -> x }): BaseDexClassLoader? {
    var classLoader = this
    while (classLoader !is BaseDexClassLoader) {
        if (classLoader.parent != null) classLoader = classLoader.parent
        else return null
    }
    return delegator(classLoader)
}

/**
 * 取自 哔哩漫游
 * 获取所有类名
 * @see `https://github.com/yujincheng08/BiliRoaming`
 */
fun ClassLoader.getAllClassesList(delegator: (BaseDexClassLoader) -> BaseDexClassLoader = { loader -> loader }): List<String> =
    findDexClassLoader(delegator)?.getObjectOrNull("pathList")
        ?.getObjectOrNullAs<Array<Any>>("dexElements")
        ?.flatMap {
            it.getObjectOrNull("dexFile")?.invokeMethodAutoAs<Enumeration<String>>("entries")?.toList()
                .orEmpty()
        }.orEmpty()

/**
 * 重新启动宿主App
 */
fun restartHostApp(activity: Activity) {
    val pm = activity.packageManager
    val intent = pm.getLaunchIntentForPackage(activity.packageName)
    activity.finishAffinity()
    activity.startActivity(intent)
    exitProcess(0)
}

/**
 * 扩展函数 判断类是否相同(用于判断参数)
 *
 * eg: fun foo(a: Boolean, b: Int) { }
 * foo.parameterTypes.sameAs(*array)
 * foo.parameterTypes.sameAs(Boolean::class.java, Int::class.java)
 * foo.parameterTypes.sameAs("boolean", "int")
 * foo.parameterTypes.sameAs(Boolean::class.java, "int")
 *
 * @param other 其他类(支持String或者Class<*>)
 * @return 是否相等
 */
fun Array<Class<*>>.sameAs(vararg other: Any): Boolean {
    if (this.size != other.size) return false
    for (i in this.indices) {
        when (val otherClazz = other[i]) {
            is Class<*> -> {
                if (this[i] != otherClazz) return false
            }
            is String -> {
                if (this[i].name != otherClazz) return false
            }
            else -> {
                throw IllegalArgumentException("Only support Class<*> or String")
            }
        }
    }
    return true
}

fun List<Class<*>>.sameAs(vararg other: Any): Boolean {
    if (this.size != other.size) return false
    for (i in this.indices) {
        when (val otherClazz = other[i]) {
            is Class<*> -> {
                if (this[i] != otherClazz) return false
            }
            is String -> {
                if (this[i].name != otherClazz) return false
            }
            else -> {
                throw IllegalArgumentException("Only support Class<*> or String")
            }
        }
    }
    return true
}
