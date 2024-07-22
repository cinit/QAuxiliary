package com.github.kyuubiran.ezxhelper.utils

import com.github.kyuubiran.ezxhelper.init.InitFields
import io.github.qauxv.loader.hookapi.IHookBridge
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XC_MethodHook.Unhook
import io.github.qauxv.util.xpcompat.XC_MethodReplacement
import io.github.qauxv.util.xpcompat.XposedBridge
import java.lang.reflect.Constructor
import java.lang.reflect.Method

typealias Hooker = (param: XC_MethodHook.MethodHookParam) -> Unit
typealias ReplaceHooker = (param: XC_MethodHook.MethodHookParam) -> Any?

/**
 * 扩展函数 hook方法/构造
 * @param hookCallback [XC_MethodHook]
 * @return unhook [XC_MethodHook.Unhook]
 */
fun Method.hookMethod(hookCallback: XC_MethodHook): XC_MethodHook.Unhook {
    return XposedBridge.hookMethod(this, hookCallback)
}

fun Constructor<*>.hookMethod(hookCallback: XC_MethodHook): XC_MethodHook.Unhook {
    return XposedBridge.hookMethod(this, hookCallback)
}

inline fun hooker(crossinline hookCallback: Hooker): Hooker = object : Hooker {
    override fun invoke(param: XC_MethodHook.MethodHookParam) {
        hookCallback(param)
    }
}

inline fun replaceHooker(crossinline hookCallback: ReplaceHooker):
        ReplaceHooker = object : ReplaceHooker {
    override fun invoke(param: XC_MethodHook.MethodHookParam): Any? {
        return hookCallback(param)
    }
}


/**
 * 扩展函数 hook方法执行前
 * @param priority 优先级 默认50
 * @param hook [Hooker] hook具体实现
 * @return unhook [XC_MethodHook.Unhook]
 */
fun Method.hookBefore(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hook: Hooker
): XC_MethodHook.Unhook {
    return this.hookMethod(object : XC_MethodHook(priority) {
        override fun beforeHookedMethod(param: MethodHookParam) = try {
            hook(param)
        } catch (thr: Throwable) {
            Log.ex(thr)
        }
    })
}

fun Method.hookBefore(hooker: Hooker) = this.hookBefore(
    IHookBridge.PRIORITY_DEFAULT,
    hooker
)

/**
 * 扩展函数 hook多个方法执行前
 * @param priority 优先级 默认50
 * @param hooker [Hooker] hook具体实现
 * @return unhooks Array<[XC_MethodHook.Unhook]>
 */
fun Array<Method>.hookBefore(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hooker: Hooker
): Array<XC_MethodHook.Unhook> {
    return this.map { it.hookBefore(priority, hooker) }.toTypedArray()
}

fun Array<Method>.hookBefore(hooker: Hooker) = this.hookBefore(
    IHookBridge.PRIORITY_DEFAULT,
    hooker
)

fun Iterable<Method>.hookBefore(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hooker: Hooker
): List<XC_MethodHook.Unhook> {
    return this.map { it.hookBefore(priority, hooker) }
}

fun Iterable<Method>.hookBefore(hooker: Hooker) = this.hookBefore(
    IHookBridge.PRIORITY_DEFAULT,
    hooker
)

/**
 * 扩展函数 hook构造执行前
 * @param priority 优先级 默认50
 * @param hook [Hooker] hook具体实现
 * @return unhook [XC_MethodHook.Unhook]
 */
fun Constructor<*>.hookBefore(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hook: Hooker
): XC_MethodHook.Unhook {
    return this.hookMethod(object : XC_MethodHook(priority) {
        override fun beforeHookedMethod(param: MethodHookParam) = try {
            hook(param)
        } catch (thr: Throwable) {
            Log.ex(thr)
        }
    })
}

fun Constructor<*>.hookBefore(hooker: Hooker) = this.hookBefore(
    IHookBridge.PRIORITY_DEFAULT,
    hooker
)

/**
 * 扩展函数 hook多个构造执行前
 * @param priority 优先级 默认50
 * @param hooker [Hooker] hook具体实现
 * @return unhooks Array<[XC_MethodHook.Unhook]>
 */
fun Array<Constructor<*>>.hookBefore(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hooker: Hooker
): Array<XC_MethodHook.Unhook> {
    return this.map { it.hookBefore(priority, hooker) }.toTypedArray()
}

fun Array<Constructor<*>>.hookBefore(hooker: Hooker) = this.hookBefore(
    IHookBridge.PRIORITY_DEFAULT,
    hooker
)

@JvmName("hookConstructorBefore")
fun Iterable<Constructor<*>>.hookBefore(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hooker: Hooker
): List<XC_MethodHook.Unhook> {
    return this.map { it.hookBefore(priority, hooker) }
}

@JvmName("hookConstructorBefore")
fun Iterable<Constructor<*>>.hookBefore(hooker: Hooker) = this.hookBefore(
    IHookBridge.PRIORITY_DEFAULT,
    hooker
)

/**
 * 扩展函数 hook方法执行后
 * @param priority 优先级 默认50
 * @param hooker [Hooker] hook具体实现
 * @return unhook [XC_MethodHook.Unhook]
 */
fun Method.hookAfter(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hooker: Hooker
): XC_MethodHook.Unhook {
    return this.hookMethod(object : XC_MethodHook(priority) {
        override fun afterHookedMethod(param: MethodHookParam) = try {
            hooker(param)
        } catch (thr: Throwable) {
            Log.ex(thr)
        }
    })
}

fun Method.hookAfter(hooker: Hooker) = this.hookAfter(
    IHookBridge.PRIORITY_DEFAULT,
    hooker
)

/**
 * 扩展函数 hook多个方法执行后
 * @param priority 优先级 默认50
 * @param hooker [Hooker] hook具体实现
 * @return unhooks Array<[XC_MethodHook.Unhook]>
 */
fun Array<Method>.hookAfter(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hooker: Hooker
): Array<XC_MethodHook.Unhook> {
    return this.map { it.hookAfter(priority, hooker) }.toTypedArray()
}

fun Array<Method>.hookAfter(hooker: Hooker) = this.hookAfter(
    IHookBridge.PRIORITY_DEFAULT,
    hooker
)

fun Iterable<Method>.hookAfter(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hooker: Hooker
): List<XC_MethodHook.Unhook> {
    return this.map { it.hookAfter(priority, hooker) }
}

fun Iterable<Method>.hookAfter(hooker: Hooker) = this.hookAfter(
    IHookBridge.PRIORITY_DEFAULT,
    hooker
)

/**
 * 扩展函数 hook构造执行后
 * @param priority 优先级 默认50
 * @param hooker [Hooker] hook具体实现
 * @return unhook [XC_MethodHook.Unhook]
 */
fun Constructor<*>.hookAfter(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hooker: Hooker
): XC_MethodHook.Unhook {
    return this.hookMethod(object : XC_MethodHook(priority) {
        override fun afterHookedMethod(param: MethodHookParam) = try {
            hooker(param)
        } catch (thr: Throwable) {
            Log.ex(thr)
        }
    })
}

fun Constructor<*>.hookAfter(hooker: Hooker) = this.hookAfter(
    IHookBridge.PRIORITY_DEFAULT,
    hooker
)

/**
 * 扩展函数 hook多个构造执行后
 * @param priority 优先级 默认50
 * @param hooker [Hooker] hook具体实现
 * @return unhooks Array<[XC_MethodHook.Unhook]>
 */
fun Array<Constructor<*>>.hookAfter(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hooker: Hooker
): Array<XC_MethodHook.Unhook> {
    return this.map { it.hookAfter(priority, hooker) }.toTypedArray()
}

fun Array<Constructor<*>>.hookAfter(hooker: Hooker) = this.hookAfter(
    IHookBridge.PRIORITY_DEFAULT,
    hooker
)

@JvmName("hookConstructorAfter")
fun Iterable<Constructor<*>>.hookAfter(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hooker: Hooker
): List<XC_MethodHook.Unhook> {
    return this.map { it.hookAfter(priority, hooker) }
}

@JvmName("hookConstructorAfter")
fun Iterable<Constructor<*>>.hookAfter(hooker: Hooker) = this.hookAfter(
    IHookBridge.PRIORITY_DEFAULT,
    hooker
)

/**
 * 扩展函数 替换方法
 * @param priority 优先级 默认50
 * @param hook [Hooker] hook具体实现
 * @return unhook [XC_MethodHook.Unhook]
 */
fun Method.hookReplace(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hook: ReplaceHooker
): XC_MethodHook.Unhook {
    return this.hookMethod(object : XC_MethodReplacement(priority) {
        override fun replaceHookedMethod(param: MethodHookParam): Any? = try {
            hook(param)
        } catch (thr: Throwable) {
            Log.ex(thr)
        }
    })
}

fun Method.hookReplace(hooker: ReplaceHooker) = this.hookReplace(
    IHookBridge.PRIORITY_DEFAULT,
    hooker
)

/**
 * 扩展函数 替换多个方法
 *
 * 注意: 会忽略hooker的返回值
 * @param priority 优先级 默认50
 * @param hooker [Hooker] hook具体实现
 * @return unhooks Array<[XC_MethodHook.Unhook]>
 */
fun Array<Method>.hookReplace(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hooker: ReplaceHooker
): Array<XC_MethodHook.Unhook> {
    return this.map { it.hookReplace(priority, hooker) }.toTypedArray()
}

fun Array<Method>.hookReplace(hooker: ReplaceHooker) = this.hookReplace(
    IHookBridge.PRIORITY_DEFAULT,
    hooker
)

fun Iterable<Method>.hookReplace(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hooker: ReplaceHooker
): List<XC_MethodHook.Unhook> {
    return this.map { it.hookReplace(priority, hooker) }
}

fun Iterable<Method>.hookReplace(hooker: ReplaceHooker) = this.hookReplace(
    IHookBridge.PRIORITY_DEFAULT,
    hooker
)

/**
 * 扩展函数 替换构造
 * @param priority 优先级 默认50
 * @param hooker [Hooker] hook具体实现
 * @return unhook [XC_MethodHook.Unhook]
 */
fun Constructor<*>.hookReplace(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hooker: ReplaceHooker
): XC_MethodHook.Unhook {
    return this.hookMethod(object : XC_MethodReplacement(priority) {
        override fun replaceHookedMethod(param: MethodHookParam): Any? = try {
            hooker(param)
        } catch (thr: Throwable) {
            Log.ex(thr)
        }
    })
}

fun Constructor<*>.hookReplace(hooker: ReplaceHooker) = this.hookReplace(
    IHookBridge.PRIORITY_DEFAULT,
    hooker
)

/**
 * 扩展函数 替换多个构造
 *
 * 注意: 会忽略hooker的返回值
 * @param priority 优先级 默认50
 * @param hooker [Hooker] hook具体实现
 * @return unhooks Array<[XC_MethodHook.Unhook]>
 */
fun Array<Constructor<*>>.hookReplace(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hooker: ReplaceHooker
): Array<XC_MethodHook.Unhook> {
    return this.map { it.hookReplace(priority, hooker) }.toTypedArray()
}

fun Array<Constructor<*>>.hookReplace(hooker: ReplaceHooker) = this.hookReplace(
    IHookBridge.PRIORITY_DEFAULT,
    hooker
)

@JvmName("hookConstructorReplace")
fun Iterable<Constructor<*>>.hookReplace(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hooker: ReplaceHooker
): List<XC_MethodHook.Unhook> {
    return this.map { it.hookReplace(priority, hooker) }
}

@JvmName("hookConstructorReplace")
fun Iterable<Constructor<*>>.hookReplace(hooker: ReplaceHooker) = this.hookReplace(
    IHookBridge.PRIORITY_DEFAULT,
    hooker
)


/**
 * 扩展函数 hook类的所有构造前
 * @param priority 优先级 默认50
 * @param hooker [Hooker] hook具体实现
 * @return unhooks Array<[XC_MethodHook.Unhook]>
 */
fun Class<*>.hookAllConstructorBefore(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hooker: Hooker
): Array<XC_MethodHook.Unhook> {
    return this.declaredConstructors.hookBefore(priority, hooker)
}

fun Class<*>.hookAllConstructorBefore(hooker: Hooker) = this.hookAllConstructorBefore(
    IHookBridge.PRIORITY_DEFAULT,
    hooker
)

/**
 * 扩展函数 hook类的所有构造后
 * @param priority 优先级 默认50
 * @param hooker [Hooker] hook具体实现
 * @return unhooks Array<[XC_MethodHook.Unhook]>
 */
fun Class<*>.hookAllConstructorAfter(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hooker: Hooker
): Array<XC_MethodHook.Unhook> {
    return this.declaredConstructors.hookAfter(priority, hooker)
}

fun Class<*>.hookAllConstructorAfter(hooker: Hooker) = this.hookAllConstructorAfter(
    IHookBridge.PRIORITY_DEFAULT,
    hooker
)

/**
 * 扩展函数 替换类的所有构造
 * @param priority 优先级 默认50
 * @param hooker [Hooker] hook具体实现
 * @return unhooks Array<[XC_MethodHook.Unhook]>
 */
fun Class<*>.hookAllConstructorReplace(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hooker: ReplaceHooker
): Array<XC_MethodHook.Unhook> {
    return this.declaredConstructors.hookReplace(priority, hooker)
}

fun Class<*>.hookAllConstructorReplace(hooker: ReplaceHooker) = this.hookAllConstructorReplace(
    IHookBridge.PRIORITY_DEFAULT,
    hooker
)

/**
 * hook类的所有构造前
 * @param clzName 类名
 * @param priority 优先级 默认50
 * @param hooker [Hooker] hook具体实现
 * @return unhooks Array<[XC_MethodHook.Unhook]>
 */
fun hookAllConstructorBefore(
    clzName: String,
    clzLoader: ClassLoader = InitFields.ezXClassLoader,
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hooker: Hooker
): Array<XC_MethodHook.Unhook> {
    return loadClass(clzName, clzLoader).declaredConstructors.hookBefore(priority, hooker)
}

/**
 * hook类的所有构造后
 * @param clzName 类名
 * @param priority 优先级 默认50
 * @param hooker [Hooker] hook具体实现
 * @return unhooks Array<[XC_MethodHook.Unhook]>
 */
fun hookAllConstructorAfter(
    clzName: String,
    clzLoader: ClassLoader = InitFields.ezXClassLoader,
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hooker: Hooker
): Array<XC_MethodHook.Unhook> {
    return loadClass(clzName, clzLoader).declaredConstructors.hookAfter(priority, hooker)
}

/**
 * 替换类的所有构造
 * @param clzName 类名
 * @param priority 优先级 默认50
 * @param hooker [Hooker] hook具体实现
 * @return unhooks Array<[XC_MethodHook.Unhook]>
 */
fun hookAllConstructorReplace(
    clzName: String,
    clzLoader: ClassLoader = InitFields.ezXClassLoader,
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hooker: Hooker
): Array<XC_MethodHook.Unhook> {
    return loadClass(clzName, clzLoader).declaredConstructors.hookReplace(priority, hooker)
}

/**
 * 扩展函数 hook方法 使其直接返回一个值
 * @param priority 优先级 默认50
 * @param obj 要返回的值
 * @return unhook [XC_MethodHook.Unhook]
 */
fun Method.hookReturnConstant(priority: Int = IHookBridge.PRIORITY_DEFAULT, obj: Any?): Unhook =
    XposedBridge.hookMethod(this, XC_MethodReplacement.returnConstant(priority, obj))

fun Method.hookReturnConstant(obj: Any?) = this.hookReturnConstant(IHookBridge.PRIORITY_DEFAULT, obj)

/**
 * 扩展函数 hook方法数组中的所有方法 使其直接返回一个值
 * @param priority 优先级 默认50
 * @param obj 要返回的值
 * @return unhooks Array<[XC_MethodHook.Unhook]>
 */
fun Array<Method>.hookReturnConstant(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    obj: Any?
): Array<Unhook> =
    this.map { XposedBridge.hookMethod(it, XC_MethodReplacement.returnConstant(priority, obj)) }
        .toTypedArray()

fun Array<Method>.hookReturnConstant(obj: Any?) =
    this.hookReturnConstant(IHookBridge.PRIORITY_DEFAULT, obj)

fun List<Method>.hookReturnConstant(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    obj: Any?
): List<Unhook> =
    this.map { XposedBridge.hookMethod(it, XC_MethodReplacement.returnConstant(priority, obj)) }

fun List<Method>.hookReturnConstant(obj: Any?) =
    this.hookReturnConstant(IHookBridge.PRIORITY_DEFAULT, obj)

/**
 * 扩展函数 hook构造 使其直接返回一个值
 * @param priority 优先级 默认50
 * @param obj 要返回的值
 * @return unhook [XC_MethodHook.Unhook]
 */
fun Constructor<*>.hookReturnConstant(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    obj: Any?
): Unhook =
    XposedBridge.hookMethod(this, XC_MethodReplacement.returnConstant(priority, obj))

fun Constructor<*>.hookReturnConstant(obj: Any?) =
    this.hookReturnConstant(IHookBridge.PRIORITY_DEFAULT, obj)

fun Array<Constructor<*>>.hookReturnConstant(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    obj: Any?
): Array<Unhook> =
    this.map { XposedBridge.hookMethod(it, XC_MethodReplacement.returnConstant(priority, obj)) }
        .toTypedArray()

fun Array<Constructor<*>>.hookReturnConstant(obj: Any?) =
    this.hookReturnConstant(IHookBridge.PRIORITY_DEFAULT, obj)

@JvmName("hookConstructorReturnConstant")
fun List<Constructor<*>>.hookReturnConstant(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    obj: Any?
): List<Unhook> =
    this.map { XposedBridge.hookMethod(it, XC_MethodReplacement.returnConstant(priority, obj)) }

@JvmName("hookConstructorReturnConstant")
fun List<Constructor<*>>.hookReturnConstant(obj: Any?) =
    this.hookReturnConstant(IHookBridge.PRIORITY_DEFAULT, obj)

/**
 * Hook工厂类
 */
class XposedHookFactory(priority: Int = IHookBridge.PRIORITY_DEFAULT) : XC_MethodHook(priority) {
    private var beforeMethod: Hooker? = null
    private var afterMethod: Hooker? = null

    override fun beforeHookedMethod(param: MethodHookParam) {
        beforeMethod?.invoke(param)
    }

    override fun afterHookedMethod(param: MethodHookParam) {
        afterMethod?.invoke(param)
    }

    /**
     * hook方法执行前
     */
    fun before(before: Hooker) {
        this.beforeMethod = before
    }

    /**
     * hook方法执行后
     */
    fun after(after: Hooker) {
        this.afterMethod = after
    }
}

/**
 * 扩展函数 hook方法
 * 直接以
 *
 * before { }
 *
 * after { }
 *
 * 的形式进行hook 两者均为可选
 * @param priority 优先级 默认50
 * @param hook 传递的XposedHookFactory
 * @return Unhook
 * @see XposedHookFactory
 */
fun Method.hookMethod(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hook: XposedHookFactory.() -> Unit
): XC_MethodHook.Unhook {
    val factory = XposedHookFactory(priority)
    hook(factory)
    return this.hookMethod(factory)
}

/**
 * 扩展函数 hook构造
 * 直接以
 *
 * before { }
 *
 * after { }
 *
 * 的形式进行hook 两者均为可选
 * @param priority 优先级 默认50
 * @param hook 传递的XposedHookFactory
 * @return Unhook
 * @see XposedHookFactory
 */
fun Constructor<*>.hookMethod(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hook: XposedHookFactory.() -> Unit
): XC_MethodHook.Unhook {
    val factory = XposedHookFactory(priority)
    hook(factory)
    return this.hookMethod(factory)
}

/**
 * 扩展函数 hook多个方法
 * 直接以
 *
 * before { }
 *
 * after { }
 *
 * 的形式进行hook 两者均为可选
 * @param priority 优先级 默认50
 * @param hook 传递的XposedHookFactory
 * @return Array<Unhook>
 * @see XposedHookFactory
 */
fun Array<Method>.hookMethod(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hook: XposedHookFactory.() -> Unit
): Array<XC_MethodHook.Unhook> {
    return this.map { it.hookMethod(priority, hook) }.toTypedArray()
}

fun Iterable<Method>.hookMethod(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hook: XposedHookFactory.() -> Unit
): List<XC_MethodHook.Unhook> {
    return this.map { it.hookMethod(priority, hook) }
}

/**
 * 扩展函数 hook多个构造
 * 直接以
 *
 * before { }
 *
 * after { }
 *
 * 的形式进行hook 两者均为可选
 * @param priority 优先级 默认50
 * @param hooker 传递的XposedHookFactory
 * @return Array<Unhook>
 * @see XposedHookFactory
 */
fun Array<Constructor<*>>.hookMethod(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hooker: XposedHookFactory.() -> Unit
): Array<XC_MethodHook.Unhook> {
    return this.map { it.hookMethod(priority, hooker) }.toTypedArray()
}

@JvmName("hookConstructor")
fun Iterable<Constructor<*>>.hookMethod(
    priority: Int = IHookBridge.PRIORITY_DEFAULT,
    hooker: XposedHookFactory.() -> Unit
): List<XC_MethodHook.Unhook> {
    return this.map { it.hookMethod(priority, hooker) }
}

/**
 * 执行数组中所有的unhook
 */
fun Array<XC_MethodHook.Unhook>.unhookAll() {
    this.forEach { it.unhook() }
}

fun Iterable<XC_MethodHook.Unhook>.unhookAll() {
    this.forEach { it.unhook() }
}
