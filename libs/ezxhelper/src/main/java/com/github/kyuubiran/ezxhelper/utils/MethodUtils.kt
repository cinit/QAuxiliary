package com.github.kyuubiran.ezxhelper.utils

import com.github.kyuubiran.ezxhelper.init.InitFields
import io.github.qauxv.util.xpcompat.XposedHelpers
import java.lang.reflect.Constructor
import java.lang.reflect.Method

@JvmInline
value class Args(val args: Array<out Any?>)

@JvmInline
value class ArgTypes(val argTypes: Array<out Class<*>>)

@Suppress("NOTHING_TO_INLINE")
inline fun args(vararg args: Any?) = Args(args)

@Suppress("NOTHING_TO_INLINE")
inline fun argTypes(vararg argTypes: Class<*>) = ArgTypes(argTypes)

/**
 * 扩展函数 通过类或者对象获取单个方法
 * @param methodName 方法名
 * @param isStatic 是否为静态方法
 * @param returnType 方法返回值 填入null为无视返回值
 * @param argTypes 方法参数类型
 * @return 符合条件的方法
 * @throws IllegalArgumentException 方法名为空
 * @throws NoSuchMethodException 未找到方法
 */
fun Any.method(
    methodName: String,
    returnType: Class<*>? = null,
    isStatic: Boolean = false,
    argTypes: ArgTypes = argTypes()
): Method {
    if (methodName.isBlank()) throw IllegalArgumentException("Method name must not be empty!")
    var c = if (this is Class<*>) this else this::class.java
    do {
        c.declaredMethods.toList().asSequence()
            .filter { it.name == methodName }
            .filter { it.parameterTypes.size == argTypes.argTypes.size }
            .apply { if (returnType != null) filter { returnType == it.returnType } }
            .filter { it.parameterTypes.sameAs(*argTypes.argTypes) }
            .filter { it.isStatic == isStatic }
            .firstOrNull()?.let { it.isAccessible = true; return it }
    } while (c.superclass?.also { c = it } != null)
    throw NoSuchMethodException("Name:$methodName, Static: $isStatic, ArgTypes:${argTypes.argTypes.joinToString(",")}")
}

/**
 * 扩展函数 通过类获取单个静态方法
 * @param methodName 方法名
 * @param returnType 方法返回值 填入null为无视返回值
 * @param argTypes 方法参数类型
 * @throws IllegalArgumentException 方法名为空
 */
fun Class<*>.staticMethod(
    methodName: String,
    returnType: Class<*>? = null,
    argTypes: ArgTypes = argTypes()
): Method {
    if (methodName.isBlank()) throw IllegalArgumentException("Method name must not be empty!")
    return this.method(methodName, returnType, true, argTypes = argTypes)
}

typealias MethodCondition = Method.() -> Boolean

/**
 * 通过条件查找类中的方法
 * @param clz 类
 * @param findSuper 是否查找父类
 * @param condition 条件
 * @return 符合条件的方法
 * @throws NoSuchMethodException
 */
fun findMethod(
    clz: Class<*>,
    findSuper: Boolean = false,
    condition: MethodCondition
): Method {
    return findMethodOrNull(clz, findSuper, condition) ?: throw NoSuchMethodException()
}

/**
 * 通过条件查找类中的方法
 * @param clz 类
 * @param findSuper 是否查找父类
 * @param condition 条件
 * @return 符合条件的方法 未找到时返回null
 */
fun findMethodOrNull(
    clz: Class<*>,
    findSuper: Boolean = false,
    condition: MethodCondition
): Method? {
    var c = clz
    c.declaredMethods.firstOrNull { it.condition() }
        ?.let { it.isAccessible = true;return it }

    if (findSuper) {
        while (c.superclass?.also { c = it } != null) {
            c.declaredMethods
                .firstOrNull { it.condition() }
                ?.let { it.isAccessible = true;return it }
        }
    }
    return null
}

/**
 * 通过条件查找方法
 * @param clzName 类名
 * @param classLoader 类加载器
 * @param findSuper 是否查找父类
 * @param condition 条件
 * @return 符合条件的方法
 * @throws NoSuchMethodException 未找到方法
 */
fun findMethod(
    clzName: String,
    classLoader: ClassLoader = InitFields.ezXClassLoader,
    findSuper: Boolean = false,
    condition: MethodCondition
): Method {
    return findMethod(loadClass(clzName, classLoader), findSuper, condition)
}

/**
 * 通过条件查找类中的方法
 * @param clzName 类名
 * @param classLoader 类加载器
 * @param findSuper 是否查找父类
 * @param condition 条件
 * @return 符合条件的方法 未找到时返回null
 */
fun findMethodOrNull(
    clzName: String,
    classLoader: ClassLoader = InitFields.ezXClassLoader,
    findSuper: Boolean = false,
    condition: MethodCondition
): Method? {
    return findMethodOrNull(loadClass(clzName, classLoader), findSuper, condition)
}

/**
 *  扩展函数 通过条件查找方法
 *  @param condition 方法的条件
 *  @return 符合条件的方法
 *  @throws NoSuchMethodException 未找到方法
 */
fun Array<Method>.findMethod(condition: MethodCondition): Method {
    return this.firstOrNull { it.condition() }?.apply { isAccessible = true }
        ?: throw NoSuchMethodException()
}

fun Iterable<Method>.findMethod(condition: MethodCondition): Method {
    return this.firstOrNull { it.condition() }?.apply { isAccessible = true }
        ?: throw NoSuchMethodException()
}

/**
 *  扩展函数 通过条件查找方法
 *  @param condition 方法的条件
 *  @return 符合条件的方法 未找到时返回null
 */
fun Array<Method>.findMethodOrNull(condition: MethodCondition): Method? {
    return this.firstOrNull { it.condition() }?.apply { isAccessible = true }
}

fun Iterable<Method>.findMethodOrNull(condition: MethodCondition): Method? {
    return this.firstOrNull { it.condition() }?.apply { isAccessible = true }
}

/**
 * 扩展函数 通过条件查找方法 每个类只搜索一个方法
 * @param findSuper 是否查找父类
 * @param condition 方法条件
 * @return 方法数组
 */
fun Array<Class<*>>.findMethods(
    findSuper: Boolean = false,
    condition: MethodCondition
): Array<Method> = mapNotNull { it.findMethodOrNull(findSuper, condition) }.toTypedArray()

fun Iterable<Class<*>>.findMethods(
    findSuper: Boolean = false,
    condition: MethodCondition
): List<Method> = mapNotNull { it.findMethodOrNull(findSuper, condition) }

/**
 * 扩展函数 加载数组中的类并且通过条件查找方法 每个类只搜索一个方法
 * @param classLoader 类加载器
 * @param findSuper 是否查找父类
 * @param condition 方法条件
 * @return 方法数组
 */
fun Array<String>.loadAndFindMethods(
    classLoader: ClassLoader = InitFields.ezXClassLoader,
    findSuper: Boolean = false,
    condition: MethodCondition
): Array<Method> {
    return this.loadAllClasses(classLoader).findMethods(findSuper, condition)
}

fun Iterable<String>.loadAndFindMethods(
    classLoader: ClassLoader = InitFields.ezXClassLoader,
    findSuper: Boolean = false,
    condition: MethodCondition
): List<Method> {
    return this.loadAllClasses(classLoader).findMethods(findSuper, condition)
}

// Method condition pair
infix fun String.mcp(condition: MethodCondition) = this to condition
infix fun Class<*>.mcp(condition: MethodCondition) = this to condition

/**
 * 扩展函数 通过条件查找数组中对应的方法 每个类只搜索一个方法
 * @param findSuper 是否查找父类
 * @return 方法数组
 */
fun Array<Pair<Class<*>, MethodCondition>>.findMethods(
    findSuper: Boolean = false
): Array<Method> {
    return this.map { (k, v) -> findMethod(k, findSuper, v) }.toTypedArray()
}

fun Iterable<Pair<Class<*>, MethodCondition>>.findMethods(
    findSuper: Boolean = false
): List<Method> {
    return this.map { (k, v) -> findMethod(k, findSuper, v) }
}

/**
 * 扩展函数 加载数组中的类并且通过条件查找方法 每个类只搜索一个方法
 * @param classLoader 类加载器
 * @param findSuper 是否查找父类
 * @return 方法数组
 */
fun Array<Pair<String, MethodCondition>>.loadAndFindMethods(
    classLoader: ClassLoader = InitFields.ezXClassLoader,
    findSuper: Boolean = false
): Array<Method> {
    return this.map { (k, v) -> findMethod(loadClass(k, classLoader), findSuper, v) }.toTypedArray()
}

fun Iterable<Pair<String, MethodCondition>>.loadAndFindMethods(
    classLoader: ClassLoader = InitFields.ezXClassLoader,
    findSuper: Boolean = false
): List<Method> {
    return this.map { (k, v) -> findMethod(loadClass(k, classLoader), findSuper, v) }
}

/**
 * 扩展函数 通过条件搜索所有方法
 * @param findSuper 是否查找父类
 * @param condition 方法条件
 * @return 方法数组
 */
fun Array<Class<*>>.findAllMethods(
    findSuper: Boolean = false,
    condition: MethodCondition
): Array<Method> {
    return this.flatMap { c -> findAllMethods(c, findSuper, condition) }.toTypedArray()
}

fun Iterable<Class<*>>.findAllMethods(
    findSuper: Boolean = false,
    condition: MethodCondition
): List<Method> {
    return this.flatMap { c -> findAllMethods(c, findSuper, condition) }
}

/**
 * 扩展函数 加载数组中的类并且通过条件查找方法
 * @param findSuper 是否查找父类
 * @return 方法数组
 */
fun Array<Pair<Class<*>, MethodCondition>>.findAllMethods(
    findSuper: Boolean = false
): Array<Method> {
    return arrayListOf<Method>()
        .apply { this@findAllMethods.forEach { (k, v) -> addAll(findAllMethods(k, findSuper, v)) } }
        .toTypedArray()
}

fun Iterable<Pair<Class<*>, MethodCondition>>.findAllMethods(
    findSuper: Boolean = false
): List<Method> {
    return arrayListOf<Method>()
        .apply { this@findAllMethods.forEach { (k, v) -> addAll(findAllMethods(k, findSuper, v)) } }
}

/**
 * 扩展函数 加载数组中的类并且通过条件查找方法
 * @param classLoader 类加载器
 * @param findSuper 是否查找父类
 * @return 方法数组
 */
fun Array<Pair<String, MethodCondition>>.loadAndFindAllMethods(
    classLoader: ClassLoader = InitFields.ezXClassLoader,
    findSuper: Boolean = false
): Array<Method> {
    return this.map { (k, v) -> loadClass(k, classLoader) to v }.toTypedArray()
        .findAllMethods(findSuper)
}

fun Iterable<Pair<String, MethodCondition>>.loadAndFindAllMethods(
    classLoader: ClassLoader = InitFields.ezXClassLoader,
    findSuper: Boolean = false
): List<Method> {
    return this.map { (k, v) -> loadClass(k, classLoader) to v }.findAllMethods(findSuper)
}

/**
 * 扩展函数 加载数组中的类并且通过条件查找所有方法
 * @param classLoader 类加载器
 * @param findSuper 是否查找父类
 * @param condition 方法条件
 * @return 方法数组
 */
fun Array<String>.loadAndFindAllMethods(
    classLoader: ClassLoader = InitFields.ezXClassLoader,
    findSuper: Boolean = false,
    condition: MethodCondition
): Array<Method> {
    return this.loadAllClasses(classLoader).findAllMethods(findSuper, condition)
}

fun Iterable<String>.loadAndFindAllMethods(
    classLoader: ClassLoader = InitFields.ezXClassLoader,
    findSuper: Boolean = false,
    condition: MethodCondition
): List<Method> {
    return this.loadAllClasses(classLoader).findAllMethods(findSuper, condition)
}

typealias ConstructorCondition = Constructor<*>.() -> Boolean

/**
 *  扩展函数 通过条件查找构造方法
 *  @param condition 构造方法的条件
 *  @return 符合条件的构造方法
 *  @throws NoSuchMethodException 未找到构造方法
 */
fun Array<Constructor<*>>.findConstructor(condition: ConstructorCondition): Constructor<*> {
    return this.firstOrNull { it.condition() }?.apply { isAccessible = true }
        ?: throw NoSuchMethodException()
}

fun Iterable<Constructor<*>>.findConstructor(condition: ConstructorCondition): Constructor<*> {
    return this.firstOrNull { it.condition() }?.apply { isAccessible = true }
        ?: throw NoSuchMethodException()
}

/**
 *  扩展函数 通过条件查找构造方法
 *  @param condition 构造方法的条件
 *  @return 符合条件的构造方法 未找到时返回null
 */
fun Array<Constructor<*>>.findConstructorOrNull(condition: ConstructorCondition): Constructor<*>? {
    return this.firstOrNull { it.condition() }?.apply { isAccessible = true }
}

fun Iterable<Constructor<*>>.findConstructorOrNull(condition: ConstructorCondition): Constructor<*>? {
    return this.firstOrNull { it.condition() }?.apply { isAccessible = true }
}

/**
 * 通过条件查找构造方法
 * @param clz 类
 * @param condition 条件
 * @return 符合条件的构造方法
 * @throws NoSuchMethodException 未找到构造方法
 */
fun findConstructor(
    clz: Class<*>,
    condition: ConstructorCondition
): Constructor<*> {
    return clz.declaredConstructors.findConstructor(condition)
}

/**
 * 通过条件查找构造方法
 * @param clz 类
 * @param condition 条件
 * @return 符合条件的构造方法 未找到时返回null
 */
fun findConstructorOrNull(
    clz: Class<*>,
    condition: ConstructorCondition
): Constructor<*>? {
    return clz.declaredConstructors.firstOrNull { it.condition() }?.apply { isAccessible = true }
}

/**
 * 通过条件查找构造方法
 * @param clzName 类名
 * @param classLoader 类加载器
 * @param condition 条件
 * @return 符合条件的构造方法
 * @throws NoSuchMethodException 未找到构造方法
 */
fun findConstructor(
    clzName: String,
    classLoader: ClassLoader = InitFields.ezXClassLoader,
    condition: ConstructorCondition
): Constructor<*> {
    return loadClass(clzName, classLoader).declaredConstructors.findConstructor(condition)
}

/**
 * 通过条件查找构造方法
 * @param clzName 类名
 * @param classLoader 类加载器
 * @param condition 条件
 * @return 符合条件的构造方法 未找到时返回null
 */
fun findConstructorOrNull(
    clzName: String,
    classLoader: ClassLoader = InitFields.ezXClassLoader,
    condition: ConstructorCondition
): Constructor<*>? {
    return loadClass(clzName, classLoader).declaredConstructors.findConstructorOrNull(condition)
}

/**
 * 查找所有符合条件的构造方法
 * @param clzName 类名
 * @param classLoader 类加载器
 * @param condition 条件
 * @return 所有符合条件的构造方法
 */
fun findAllConstructors(
    clzName: String,
    classLoader: ClassLoader = InitFields.ezXClassLoader,
    condition: ConstructorCondition
): List<Constructor<*>> {
    return loadClass(clzName, classLoader).declaredConstructors.filter(condition)
}

/**
 * 查找所有符合条件的构造方法
 * @param clz 类
 * @param condition 条件
 * @return 所有符合条件的构造方法
 */
fun findAllConstructors(
    clz: Class<*>,
    condition: ConstructorCondition
): List<Constructor<*>> {
    return clz.declaredConstructors.filter(condition)
}

/**
 * 扩展函数 通过遍历方法数组 返回符合条件的方法数组
 * @param condition 条件
 * @return 符合条件的方法数组
 */
fun Array<Method>.findAllMethods(condition: MethodCondition): Array<Method> {
    return this.filter { it.condition() }.onEach { it.isAccessible = true }.toTypedArray()
}

fun Iterable<Method>.findAllMethods(condition: MethodCondition): List<Method> {
    return this.filter { it.condition() }.onEach { it.isAccessible = true }.toList()
}

/**
 * 通过条件获取方法数组
 * @param clz 类
 * @param findSuper 是否查找父类
 * @param condition 条件
 * @return 符合条件的方法数组
 */
fun findAllMethods(
    clz: Class<*>,
    findSuper: Boolean = false,
    condition: MethodCondition
): List<Method> {
    var c = clz
    val arr = ArrayList<Method>()
    arr.addAll(c.declaredMethods.findAllMethods(condition))
    if (findSuper) {
        while (c.superclass?.also { c = it } != null) {
            arr.addAll(c.declaredMethods.findAllMethods(condition))
        }
    }
    return arr
}

/**
 * 通过条件获取方法数组
 * @param clzName 类名
 * @param classLoader 类加载器
 * @param findSuper 是否查找父类
 * @param condition 条件
 * @return 符合条件的方法数组
 */
fun findAllMethods(
    clzName: String,
    classLoader: ClassLoader = InitFields.ezXClassLoader,
    findSuper: Boolean = false,
    condition: MethodCondition
): List<Method> {
    return findAllMethods(loadClass(clzName, classLoader), findSuper, condition)
}

/**
 * 扩展函数 调用对象中符合条件的方法
 * @param args 参数
 * @param condition 条件
 * @return 方法的返回值
 * @throws NoSuchMethodException 未找到方法
 */
fun Any.invokeMethod(
    vararg args: Any?,
    condition: MethodCondition
): Any? {
    this::class.java.declaredMethods.firstOrNull { it.condition() }
        ?.let { it.isAccessible = true;return it(this, *args) }
    throw NoSuchMethodException()
}

/**
 * 扩展函数 调用类中符合条件的静态方法
 * @param args 参数表
 * @param condition 条件
 * @return 方法的返回值
 * @throws NoSuchMethodException 未找到方法
 */
fun Class<*>.invokeStaticMethod(
    vararg args: Any?,
    condition: MethodCondition
): Any? {
    this.declaredMethods.firstOrNull { it.isStatic && it.condition() }
        ?.let { it.isAccessible = true;return it.invoke(null, *args) }
    throw NoSuchMethodException()
}

/**
 * 扩展函数 调用对象的方法
 *
 * @param methodName 方法名
 * @param args 形参表 可空
 * @param argTypes 形参类型 可空
 * @param returnType 返回值类型 为null时无视返回值类型
 * @return 函数调用后的返回值
 * @throws IllegalArgumentException 当方法名为空时
 * @throws IllegalArgumentException 当args的长度与argTypes的长度不符时
 * @throws IllegalArgumentException 当对象是一个Class时
 */
fun Any.invokeMethod(
    methodName: String,
    args: Args = args(),
    argTypes: ArgTypes = argTypes(),
    returnType: Class<*>? = null
): Any? {
    if (methodName.isBlank()) throw IllegalArgumentException("Object name must not be empty!")
    if (args.args.size != argTypes.argTypes.size) throw IllegalArgumentException("Method args size must equals argTypes size!")
    return tryOrNull {
        this.method(methodName, returnType, false, argTypes).invoke(this, *args.args)
    }
}

/**
 * 扩展函数 调用对象的方法 并且将返回值转换为T?类型
 *
 * 注意: 请勿对类使用此函数
 * @param methodName 方法名
 * @param args 形参表 可空
 * @param argTypes 形参类型 可空
 * @param returnType 返回值类型 为null时无视返回值类型
 * @param T 转换的类型
 * @return 函数调用后的返回值
 * @throws IllegalArgumentException 当方法名为空时
 * @throws IllegalArgumentException 当args的长度与argTypes的长度不符时
 * @throws IllegalArgumentException 当对象是一个Class时
 */
@Suppress("UNCHECKED_CAST")
fun <T> Any.invokeMethodAs(
    methodName: String,
    args: Args = args(),
    argTypes: ArgTypes = argTypes(),
    returnType: Class<*>? = null
): T? = this.invokeMethod(methodName, args, argTypes, returnType) as T?


/**
 * 扩展函数 调用对象与形参表最佳匹配的方法
 * @param methodName 方法名
 * @param args 形参
 * @return 函数调用时的返回值
 */
fun Any.invokeMethodAuto(
    methodName: String,
    vararg args: Any?
): Any? {
    return XposedHelpers.callMethod(this, methodName, *args)
}

/**
 * 扩展函数 调用对象与形参表最佳匹配的方法 并将返回值转换为T?类型
 * @param methodName 方法名
 * @param args 形参
 * @return 函数调用时的返回值
 */
@Suppress("UNCHECKED_CAST")
fun <T> Any.invokeMethodAutoAs(
    methodName: String,
    vararg args: Any?
): T? = XposedHelpers.callMethod(this, methodName, *args) as T?

/**
 * 扩展函数 调用类的静态方法
 * @param methodName 方法名
 * @param args 形参表 可空
 * @param argTypes 形参类型 可空
 * @param returnType 返回值类型 为null时无视返回值类型
 * @return 函数调用后的返回值
 * @throws IllegalArgumentException 当args的长度与argTypes的长度不符时
 */
fun Class<*>.invokeStaticMethod(
    methodName: String,
    args: Args = args(),
    argTypes: ArgTypes = argTypes(),
    returnType: Class<*>? = null
): Any? {
    if (args.args.size != argTypes.argTypes.size) throw IllegalArgumentException("Method args size must equals argTypes size!")
    return tryOrNull {
        this.method(methodName, returnType, true, argTypes).invoke(null, *args.args)
    }
}

/**
 * 扩展函数 调用类的静态方法 并且将返回值转换为T?类型
 * @param methodName 方法名
 * @param args 形参表 可空
 * @param argTypes 形参类型 可空
 * @param returnType 返回值类型 为null时无视返回值类型
 * @return 函数调用后的返回值
 * @throws IllegalArgumentException 当args的长度与argTypes的长度不符时
 */
@Suppress("UNCHECKED_CAST")
fun <T> Class<*>.invokeStaticMethodAs(
    methodName: String,
    args: Args = args(),
    argTypes: ArgTypes = argTypes(),
    returnType: Class<*>? = null
): T? = this.invokeStaticMethod(methodName, args, argTypes, returnType) as T?

/**
 * 扩展函数 调用类中与形参表最佳匹配的静态方法
 * @param methodName 方法名
 * @param args 形参
 * @return 函数调用时的返回值
 */
fun Class<*>.invokeStaticMethodAuto(
    methodName: String,
    vararg args: Any?
): Any? = XposedHelpers.callStaticMethod(this, methodName, *args)

/**
 * 扩展函数 调用类中与形参表最佳匹配的静态方法 并将返回值转换为T?类型
 * @param methodName 方法名
 * @param args 形参
 * @return 函数调用时的返回值
 */
@Suppress("UNCHECKED_CAST")
fun <T> Class<*>.invokeStaticMethodAutoAs(
    methodName: String,
    vararg args: Any?
): T? = XposedHelpers.callStaticMethod(this, methodName, *args) as T?

/**
 * 扩展函数 创建新的实例化对象
 * @param args 构造函数的形参表
 * @param argTypes 构造函数的形参类型
 * @return 成功时返回实例化的对象 失败时返回null
 * @throws IllegalArgumentException 当args的长度与argTypes的长度不符时
 */
fun Class<*>.newInstance(
    args: Args = args(),
    argTypes: ArgTypes = argTypes()
): Any? {
    if (args.args.size != argTypes.argTypes.size) throw IllegalArgumentException("Method args size must equals argTypes size!")
    return tryOrLogNull {
        this.getDeclaredConstructor(*argTypes.argTypes).apply {
            isAccessible = true
        }.newInstance(*args.args)
    }
}

/**
 * 扩展函数 创建新的实例化对象 并将对象转换为T?类型
 * @param args 构造函数的形参表
 * @param argTypes 构造函数的形参类型
 * @return 成功时返回实例化的对象 失败时返回null
 * @throws IllegalArgumentException 当args的长度与argTypes的长度不符时
 */
@Suppress("UNCHECKED_CAST")
fun <T> Class<*>.newInstanceAs(
    args: Args = args(),
    argTypes: ArgTypes = argTypes()
): T? = this.newInstance(args, argTypes) as T?

/**
 * 扩展函数 调用方法 并将返回值转换为T?类型
 * @param obj 被调用对象
 * @param args 形参表
 */
@Suppress("UNCHECKED_CAST")
fun <T> Method.invokeAs(
    obj: Any?,
    vararg args: Any?
): T? = this.run {
    isAccessible = true
    invoke(obj, *args) as T?
}

/**
 * 通过Descriptor获取方法
 * @param desc Descriptor
 * @param clzLoader 类加载器
 * @return 找到的方法
 * @throws NoSuchMethodException 未找到方法
 */
fun getMethodByDesc(
    desc: String,
    clzLoader: ClassLoader = InitFields.ezXClassLoader
): Method = DexDescriptor.newMethodDesc(desc).getMethod(clzLoader).apply { isAccessible = true }

/**
 * 通过Descriptor获取方法
 * @param desc Descriptor
 * @param clzLoader 类加载器
 * @return 找到的方法 未找到则返回null
 */
fun getMethodByDescOrNull(
    desc: String,
    clzLoader: ClassLoader = InitFields.ezXClassLoader
): Method? = runCatching { getMethodByDesc(desc, clzLoader) }.getOrNull()


/**
 * 扩展函数 通过Descriptor获取方法
 * @param desc Descriptor
 * @return 找到的方法
 * @throws NoSuchMethodException 未找到方法
 */
fun ClassLoader.getMethodByDesc(desc: String): Method = getMethodByDesc(desc, this)

/**
 * 扩展函数 通过Descriptor获取方法
 * @param desc Descriptor
 * @return 找到的方法 未找到则返回null
 */
fun ClassLoader.getMethodByDescOrNull(desc: String): Method? = getMethodByDescOrNull(desc, this)
