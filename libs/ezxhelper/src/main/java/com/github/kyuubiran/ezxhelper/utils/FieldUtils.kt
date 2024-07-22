package com.github.kyuubiran.ezxhelper.utils

import com.github.kyuubiran.ezxhelper.init.InitFields
import java.lang.reflect.Field
import java.lang.reflect.Method

typealias FieldCondition = Field.() -> Boolean

/**
 * 通过条件查找类中的属性
 * @param clz 类
 * @param findSuper 是否查找父类
 * @param condition 条件
 * @return 符合条件的属性
 * @throws NoSuchFieldException
 */
fun findField(
    clz: Class<*>,
    findSuper: Boolean = false,
    condition: FieldCondition
): Field {
    return findFieldOrNull(clz, findSuper, condition) ?: throw NoSuchFieldException()
}

/**
 * 通过条件查找类中的属性
 * @param clz 类
 * @param findSuper 是否查找父类
 * @param condition 条件
 * @return 符合条件的属性 未找到时返回null
 */
fun findFieldOrNull(
    clz: Class<*>,
    findSuper: Boolean = false,
    condition: FieldCondition
): Field? {
    var c = clz
    c.declaredFields.firstOrNull { it.condition() }?.let {
        it.isAccessible = true;return it
    }
    if (findSuper) {
        while (c.superclass?.also { c = it } != null) {
            c.declaredFields.firstOrNull { it.condition() }
                ?.let { it.isAccessible = true;return it }
        }
    }
    return null
}

/**
 * 通过条件查找类中的属性
 * @param clzName 类名
 * @param classLoader 类加载器
 * @param findSuper 是否查找父类
 * @param condition 条件
 * @return 符合条件的属性
 * @throws NoSuchFieldException 未找到属性
 */
fun findField(
    clzName: String,
    classLoader: ClassLoader = InitFields.ezXClassLoader,
    findSuper: Boolean = false,
    condition: FieldCondition
): Field {
    return findField(loadClass(clzName, classLoader), findSuper, condition)
}

/**
 * 通过条件查找类中的属性
 * @param clzName 类名
 * @param findSuper 是否查找父类
 * @param condition 条件
 * @return 符合条件的属性 未找到时返回null
 */
fun findFieldOrNull(
    clzName: String,
    classLoader: ClassLoader = InitFields.ezXClassLoader,
    findSuper: Boolean = false,
    condition: FieldCondition
): Field? {
    return findFieldOrNull(loadClass(clzName, classLoader), findSuper, condition)
}

/**
 * 扩展函数 通过条件查找属性
 * @param condition 条件
 * @return 符合条件的属性
 * @throws NoSuchFieldException 未找到属性
 */
fun Array<Field>.findField(condition: FieldCondition): Field {
    return this.firstOrNull { it.condition() }?.apply { isAccessible = true }
        ?: throw NoSuchFieldException()
}

fun Iterable<Field>.findField(condition: FieldCondition): Field {
    return this.firstOrNull { it.condition() }?.apply { isAccessible = true }
        ?: throw NoSuchFieldException()
}

/**
 * 扩展函数 通过条件查找属性
 * @param condition 条件
 * @return 符合条件的属性 未找到时返回null
 */
fun Array<Field>.findFieldOrNull(condition: FieldCondition): Field? =
    this.firstOrNull { it.condition() }?.apply { isAccessible = true }


fun Iterable<Field>.findFieldOrNull(condition: FieldCondition): Field? =
    this.firstOrNull { it.condition() }?.apply { isAccessible = true }


/**
 * 扩展函数 通过遍历属性数组 返回符合条件的属性数组
 * @param condition 条件
 * @return 符合条件的属性数组
 */
fun Array<Field>.findAllFields(condition: FieldCondition): Array<Field> =
    this.filter { it.condition() }.onEach { it.isAccessible = true }.toTypedArray()


fun Iterable<Field>.findAllFields(condition: FieldCondition): List<Field> =
    this.filter { it.condition() }.map { it.isAccessible = true;it }


/**
 * 通过条件获取属性数组
 * @param clz 类
 * @param findSuper 是否查找父类
 * @param condition 条件
 * @return 符合条件的属性数组
 */
fun findAllFields(
    clz: Class<*>,
    findSuper: Boolean = false,
    condition: FieldCondition
): List<Field> {
    var c = clz
    val arr = ArrayList<Field>()
    arr.addAll(c.declaredFields.findAllFields(condition))
    if (findSuper) {
        while (c.superclass?.also { c = it } != null) {
            arr.addAll(c.declaredFields.findAllFields(condition))
        }
    }
    return arr
}

/**
 * 通过条件获取属性数组
 * @param clzName 类名
 * @param classLoader 类加载器
 * @param findSuper 是否查找父类
 * @param condition 条件
 * @return 符合条件的属性数组
 */
fun findAllFields(
    clzName: String,
    classLoader: ClassLoader = InitFields.ezXClassLoader,
    findSuper: Boolean = false,
    condition: FieldCondition
): List<Field> = findAllFields(loadClass(clzName, classLoader), findSuper, condition)

/**
 * 扩展函数 通过类或者对象获取单个属性
 * @param fieldName 属性名
 * @param isStatic 是否静态类型
 * @param fieldType 属性类型
 * @return 符合条件的属性
 * @throws IllegalArgumentException 属性名为空
 * @throws NoSuchFieldException 未找到属性
 */
fun Any.field(
    fieldName: String,
    isStatic: Boolean = false,
    fieldType: Class<*>? = null
): Field {
    if (fieldName.isBlank()) throw IllegalArgumentException("Field name must not be empty!")
    var c: Class<*> = if (this is Class<*>) this else this.javaClass
    do {
        c.declaredFields
            .filter { isStatic == it.isStatic }
            .firstOrNull { (fieldType == null || it.type == fieldType) && (it.name == fieldName) }
            ?.let { it.isAccessible = true;return it }
    } while (c.superclass?.also { c = it } != null)
    throw NoSuchFieldException("Name: $fieldName,Static: $isStatic, Type: ${if (fieldType == null) "ignore" else fieldType.name}")
}

/**
 * 扩展函数 通过类型获取属性
 * @param type 类型
 * @param isStatic 是否静态
 * @return 符合条件的属性
 * @throws NoSuchFieldException 未找到属性
 */
fun Any.getFieldByType(type: Class<*>, isStatic: Boolean = false): Field {
    var c: Class<*> = if (this is Class<*>) this else this.javaClass
    do {
        c.declaredFields
            .filter { isStatic == it.isStatic }
            .firstOrNull { it.type == type }
            ?.let { it.isAccessible = true;return it }
    } while (c.superclass?.also { c = it } != null)
    throw NoSuchFieldException()
}

fun Any.getStaticFieldByType(type: Class<*>): Field = this.getFieldByType(type, true)

/**
 * 扩展函数 通过类获取静态属性
 * @param fieldName 属性名称
 * @param type 属性类型
 * @return 符合条件的属性
 * @throws IllegalArgumentException 属性名为空
 * @throws NoSuchFieldException 未找到属性
 */
fun Class<*>.staticField(fieldName: String, type: Class<*>? = null): Field {
    if (fieldName.isBlank()) throw IllegalArgumentException("Field name must not be empty!")
    return this.field(fieldName, true, type)
}

/**
 * 扩展函数 获取静态对象 并转换为T?类型
 * @return 成功时返回获取到的对象 失败时返回null
 */
@Suppress("UNCHECKED_CAST")
fun <T> Field.getStaticAs(): T? = this.run {
    isAccessible = true
    get(null) as T?
}

/**
 * 扩展函数 获取非空对象
 * @param obj 对象
 * @return 成功时返回获取到的对象 失败时抛出异常
 */
fun Field.getNonNull(obj: Any?): Any = this.run {
    isAccessible = true
    get(obj)!!
}

/**
 * 扩展函数 获取非空对象 并转换为T类型
 * @param obj 对象
 * @return 成功时返回获取到的对象 失败时抛出异常
 */
@Suppress("UNCHECKED_CAST")
fun <T> Field.getNonNullAs(obj: Any?): T = this.run {
    isAccessible = true
    get(obj)!! as T
}

/**
 * 扩展函数 获取静态非空对象
 * @return 成功时返回获取到的对象 失败时抛出异常
 */
fun Field.getStaticNonNull(): Any = this.run {
    isAccessible = true
    get(null)!!
}

/**
 * 扩展函数 获取静态非空对象 并转换为T类型
 * @return 成功时返回获取到的对象 失败时抛出异常
 */
@Suppress("UNCHECKED_CAST")
fun <T> Field.getStaticNonNullAs(): T = this.run {
    isAccessible = true
    get(null)!! as T
}

/**
 * 扩展函数 获取对象 并转换为T?类型
 * @param obj 对象
 * @return 成功时返回获取到的对象 失败时返回null
 */
@Suppress("UNCHECKED_CAST")
fun <T> Field.getAs(obj: Any?): T? = this.run {
    isAccessible = true
    get(obj) as T?
}

/**
 * 扩展函数 获取静态对象
 * @return 成功时返回获取到的对象 失败时返回null
 */
fun Field.getStatic(): Any? = this.run {
    isAccessible = true
    get(null)
}

/**
 * 深拷贝一个对象
 * @param srcObj 源对象
 * @param newObj 新对象
 * @return 成功返回拷贝后的对象 失败返回null
 */
fun <T> fieldCpy(srcObj: T, newObj: T): T? = tryOrLogNull {
    var clz: Class<*> = srcObj!!::class.java
    var fields: Array<Field>
    while (Object::class.java != clz) {
        fields = clz.declaredFields
        for (f in fields) {
            f.isAccessible = true
            f.set(newObj, f.get(srcObj))
        }
        clz = clz.superclass
    }
    newObj
}

typealias ObjectCondition = Any?.() -> Boolean

/**
 * 强烈不推荐!!非常慢!!
 *
 * 扩展函数 遍历对象中的属性并返回符合条件的对象
 * @param condition 条件
 * @return 成功时返回找到的对象 失败时返回null
 */
fun Any.findObject(condition: ObjectCondition): Any? =
    this.javaClass.declaredFields.firstNotNullOfOrNull {
        it.isAccessible = true
        it.get(this)?.let { o -> o.condition() } ?: false
    }

/**
 * 强烈不推荐!!非常慢!!
 *
 * 扩展函数 遍历对象中的属性并返回符合条件的对象
 * @param fieldCond 属性条件
 * @param objCond 对象条件
 * @return 成功时返回找到的对象 失败时返回null
 */
fun Any.findObject(
    fieldCond: FieldCondition,
    objCond: ObjectCondition
): Any? = this.javaClass.declaredFields.firstNotNullOfOrNull f@{
    if (!it.fieldCond()) return@f false
    it.isAccessible = true
    it.get(this)?.let { o -> o.objCond() } ?: false
}

/**
 * 强烈不推荐!!非常慢!!
 *
 * 扩展函数 遍历类中的静态属性并返回符合条件的静态对象
 * @param condition 条件
 * @return 成功时返回找到的静态对象 失败时返回null
 */
fun Class<*>.findStaticObject(condition: ObjectCondition): Any? =
    this.declaredFields.firstNotNullOfOrNull {
        it.isAccessible = true
        it.get(null)?.let(condition) ?: false
    }

/**
 * 强烈不推荐!!非常慢!!
 *
 * 扩展函数 遍历类中的静态属性并返回符合条件的静态对象
 * @param fieldCond 属性条件
 * @param objCond 对象条件
 * @return 成功时返回找到的静态对象 失败时返回null
 */
fun Any.findStaticObject(
    fieldCond: FieldCondition,
    objCond: ObjectCondition
): Any? = this.javaClass.declaredFields.firstNotNullOfOrNull f@{
    if (!it.fieldCond()) return@f false
    it.isAccessible = true
    it.get(null)?.let(objCond) ?: false
}

/**
 * 扩展函数 获取实例化对象中的对象
 * @param objName 对象名称
 * @param type 类型
 * @return 成功时返回获取到的对象 失败时返回null
 * @throws IllegalArgumentException 目标对象名为空
 */
fun Any.getObjectOrNull(objName: String, type: Class<*>? = null): Any? {
    if (objName.isBlank()) throw java.lang.IllegalArgumentException("Object name must not be empty!")
    return tryOrLogNull { this.field(objName, fieldType = type).get(this) }
}

/**
 * 扩展函数 获取实例化对象中的对象
 * @param objName 对象名称
 * @param type 类型
 * @param T 转换的类型
 * @return 成功时返回获取到的对象 失败时返回null
 * @throws IllegalArgumentException 目标对象名为空
 */
@Suppress("UNCHECKED_CAST")
fun <T> Any.getObjectOrNullAs(objName: String, type: Class<*>? = null): T? {
    return this.getObjectOrNull(objName, type) as T?
}

/**
 * 扩展函数 获取实例化对象中的对象
 * @param objName 对象名称
 * @param type 类型
 * @return 成功时返回获取到的对象 失败时抛出异常
 * @throws IllegalArgumentException 目标对象名为空
 */
fun Any.getObject(objName: String, type: Class<*>? = null): Any {
    if (objName.isBlank()) throw IllegalArgumentException("Object name must not be empty!")
    return this.javaClass.field(objName, false, type).get(this)!!
}

/**
 * 扩展函数 获取实例化对象中的对象 并转化为类型T
 *
 * @param objName 对象名称
 * @param type 类型
 * @return 成功时返回获取到的对象 失败时抛出异常
 * @throws IllegalArgumentException 目标对象名为空
 */
@Suppress("UNCHECKED_CAST")
fun <T> Any.getObjectAs(objName: String, type: Class<*>? = null): T =
    this.getObject(objName, type) as T

@Suppress("UNCHECKED_CAST")
fun <T> Any.getObjectAs(field: Field): T = field.get(this) as T

/**
 * 扩展函数 获取实例化对象中的对象
 * @param field 属性
 * @return 成功时返回获取到的对象 失败时返回null
 */
fun Any.getObjectOrNull(field: Field): Any? = tryOrLogNull { field.let { it.isAccessible;it.get(this) } }

/**
 * 扩展函数 获取实例化对象中的对象 并且转换为T?类型
 *
 * 注意: 请勿对Class使用此函数
 * @param field 属性
 * @param T 转换的类型
 * @return 成功时返回获取到的对象 失败时返回null
 */
@Suppress("UNCHECKED_CAST")
fun <T> Any.getObjectOrNullAs(field: Field): T? = this.getObjectOrNull(field) as T?

/**
 * 扩展函数 通过类型 获取实例化对象中的对象
 *
 * 不推荐使用 此函数只会返回第一次匹配到的对象
 * @param type 类型
 * @return 成功时返回获取到的对象 失败时返回null
 */
fun Any.getObjectOrNullByType(type: Class<*>): Any? = tryOrLogNull {
    this.getFieldByType(type).get(this)
}

/**
 * 扩展函数 通过类型 获取实例化对象中的对象 并转换为T?类型
 *
 * 不推荐使用 此函数只会返回第一次匹配到的对象
 *
 * 注意: 请勿对Class使用此函数
 * @param type 类型
 * @return 成功时返回获取到的对象 失败时返回null
 */
@Suppress("UNCHECKED_CAST")
fun <T> Any.getObjectOrNullByTypeAs(type: Class<*>): T? = this.getObjectOrNullByType(type) as T?

/**
 * 扩展函数 通过类型 获取实例化对象中的对象
 *
 * 不推荐使用 此函数只会返回第一次匹配到的对象
 *
 * 注意: 请勿对Class使用此函数
 * @param type 类型
 * @return 成功时返回获取到的对象 失败时抛出异常
 */
fun Any.getObjectByType(type: Class<*>): Any = this.getFieldByType(type).get(this)!!

/**
 * 扩展函数 通过类型 获取实例化对象中的对象 并转换为T类型
 *
 * 不推荐使用 此函数只会返回第一次匹配到的对象
 *
 * 注意: 请勿对Class使用此函数
 * @param type 类型
 * @return 成功时返回获取到的对象 失败时抛出异常
 */
@Suppress("UNCHECKED_CAST")
fun <T> Any.getObjectByTypeAs(type: Class<*>): T = this.getObjectByType(type) as T


/**
 * 扩展函数 获取类中的静态对象
 * @param objName 需要获取的对象名
 * @param type 类型
 * @return 成功时返回获取到的对象 失败时返回null
 * @throws IllegalArgumentException 当名字为空时
 */
fun Class<*>.getStaticObjectOrNull(
    objName: String,
    type: Class<*>? = null
): Any? = tryOrLogNull {
    if (objName.isBlank()) throw IllegalArgumentException("Object name must not be empty!")
    tryOrNull { this.staticField(objName, type) }?.get(null)
}

/**
 * 扩展函数 获取类中的静态对象 并且转换为T?类型
 * @param objName 需要获取的对象名
 * @param type 类型
 * @param T 转换的类型
 * @return 成功时返回获取到的对象 失败时返回null
 * @throws IllegalArgumentException 当名字为空时
 */
@Suppress("UNCHECKED_CAST")
fun <T> Class<*>.getStaticObjectOrNullAs(
    objName: String,
    type: Class<*>? = null
): T? = this.getStaticObjectOrNull(objName, type) as T?

/**
 * 扩展函数 获取类中的静态对象
 * @param objName 需要获取的对象名
 * @param type 类型
 * @return 成功时返回获取到的对象 失败时抛出异常
 * @throws IllegalArgumentException 当名字为空时
 * @throws NoSuchFieldException 未找到属性
 */
fun Class<*>.getStaticObject(
    objName: String,
    type: Class<*>? = null
): Any {
    if (objName.isBlank()) throw IllegalArgumentException("Object name must not be empty!")
    return this.staticField(objName, type).get(this)!!
}

/**
 * 扩展函数 获取类中的静态对象 并转换为T类型
 * @param objName 需要获取的对象名
 * @param type 类型
 * @return 成功时返回获取到的对象 失败时抛出异常
 * @throws IllegalArgumentException 当名字为空时
 */
@Suppress("UNCHECKED_CAST")
fun <T> Class<*>.getStaticObjectAs(
    objName: String,
    type: Class<*>? = null
): T = this.getStaticObject(objName, type) as T

/**
 * 获取Field中的对象
 * @param field 属性
 * @return 返回获取到的对象(Nullable)
 */
fun getStaticObjectOrNull(field: Field): Any? = field.run {
    isAccessible = true
    get(null)
}

/**
 * 获取Field中的对象 并转换为T?类型
 * @param field 属性
 * @return 返回获取到的对象(Nullable)
 */
@Suppress("UNCHECKED_CAST")
fun <T> getStaticObjectOrNullAs(field: Field): T? = getStaticObjectOrNull(field) as T?

/**
 * 获取Field中的对象
 * @param field 属性
 * @return 成功时返回获取到的对象 失败时抛出异常
 */
fun getStaticObject(field: Field): Any = field.run {
    isAccessible = true
    get(null)!!
}

/**
 * 获取Field中的对象 并转换为T类型
 * @param field 属性
 * @return 成功时返回获取到的对象 失败时抛出异常
 */
@Suppress("UNCHECKED_CAST")
fun <T> getStaticObjectAs(field: Field): T = getStaticObject(field) as T

/**
 * 扩展函数 通过类型 获取类中的静态对象
 *
 * 不推荐使用 此函数只会返回第一次匹配到的对象
 * @param type 类型
 * @return 成功时返回获取到的对象 失败时抛出异常
 */
fun Class<*>.getStaticObjectByType(type: Class<*>): Any = this.getStaticFieldByType(type).get(null)!!

/**
 * 扩展函数 通过类型 获取类中的静态对象 并转换为T类型
 *
 * 不推荐使用 此函数只会返回第一次匹配到的对象
 * @param type 类型
 * @return 成功时返回获取到的对象 失败时抛出异常
 */
@Suppress("UNCHECKED_CAST")
fun <T> Class<*>.getStaticObjectByTypeAs(type: Class<*>): T = this.getStaticFieldByType(type).get(null) as T

/**
 * 扩展函数 通过类型 获取类中的静态对象
 *
 * 不推荐使用 此函数只会返回第一次匹配到的对象
 * @param type 类型
 * @return 成功时返回获取到的对象 失败时返回null
 */
fun Class<*>.getStaticObjectOrNullByType(type: Class<*>): Any? = tryOrLogNull {
    this.getStaticFieldByType(type).get(null)
}

/**
 * 扩展函数 通过类型 获取类中的静态对象 并转换为T？类型
 *
 * 不推荐使用 此函数只会返回第一次匹配到的对象
 * @param type 类型
 * @return 成功时返回获取到的对象 失败时返回null
 */
@Suppress("UNCHECKED_CAST")
fun <T> Class<*>.getStaticObjectOrNullByTypeAs(type: Class<*>): T? = this.getStaticFieldByType(type) as T?

/**
 * 扩展函数 设置对象中对象的值
 *
 * @param objName 需要设置的对象名称
 * @param value 值
 * @param fieldType 对象类型
 * @throws IllegalArgumentException 对象名为空
 */
fun Any.putObject(objName: String, value: Any?, fieldType: Class<*>? = null) {
    if (objName.isBlank()) throw IllegalArgumentException("Object name must not be empty!")
    tryOrLog { this.field(objName, false, fieldType).set(this, value) }
}

/**
 * 扩展函数 设置对象中对象的值
 * @param field 属性
 * @param value 值
 */
fun Any.putObject(field: Field, value: Any?) = tryOrLog {
    field.let {
        it.isAccessible = true
        it.set(this, value)
    }
}

/**
 * 扩展函数 通过类型设置值
 *
 * 不推荐使用 只会设置第一个类型符合的对象的值
 * @param value 值
 * @param type 类型
 */
fun Any.putObjectByType(value: Any?, type: Class<*>) = tryOrLog {
    this.getFieldByType(type).set(this, value)
}

/**
 * 扩展函数 通过类型设置类中的静态对象的值
 *
 * 不推荐使用 只会设置第一个类型符合的对象的值
 * @param value 值
 * @param type 类型
 */
fun Class<*>.putStaticObjectByType(value: Any?, type: Class<*>) = tryOrLog {
    this.getStaticFieldByType(type).set(null, value)
}

/**
 * 扩展函数 设置类中静态对象值
 * @param objName 需要设置的对象名称
 * @param value 值
 * @param fieldType 对象类型
 * @throws IllegalArgumentException 对象名为空
 */
fun Class<*>.putStaticObject(objName: String, value: Any?, fieldType: Class<*>? = null) = tryOrLog {
    if (objName.isBlank()) throw IllegalArgumentException("Object name must not be empty!")
    try {
        this.staticField(objName, fieldType)
    } catch (e: NoSuchFieldException) {
        return
    }.set(null, value)
}

/**
 * 扩展函数 查找符合条件的属性并获取对象
 * @param findSuper 是否查找父类
 * @param condition 条件
 * @return 符合条件的属性对象
 * @throws NoSuchFieldException 未找到符合的属性
 */
fun Any.findFieldObject(findSuper: Boolean = false, condition: FieldCondition): Any =
    this.javaClass.findField(findSuper, condition).get(this)!!

/**
 * 扩展函数 查找符合条件的属性并获取对象 并转化为T类型
 * @param findSuper 是否查找父类
 * @param condition 条件
 * @return 符合条件的属性对象
 * @throws NoSuchFieldException 未找到符合的属性
 */
@Suppress("UNCHECKED_CAST")
fun <T> Any.findFieldObjectAs(findSuper: Boolean = false, condition: FieldCondition): T =
    this.javaClass.findField(findSuper, condition).get(this) as T

/**
 * 扩展函数 查找符合条件的属性并获取对象
 * @param findSuper 是否查找父类
 * @param condition 条件
 * @return 符合条件的属性对象 未找到时返回null
 */
fun Any.findFieldObjectOrNull(findSuper: Boolean = false, condition: FieldCondition): Any? =
    this.javaClass.findFieldOrNull(findSuper, condition)?.get(this)

/**
 * 扩展函数 查找符合条件的属性并获取对象 并转化为T?类型
 * @param findSuper 是否查找父类
 * @param condition 条件
 * @return 符合条件的属性对象 未找到时返回null
 */
@Suppress("UNCHECKED_CAST")
fun <T> Any.findFieldObjectOrNullAs(findSuper: Boolean = false, condition: FieldCondition): T? =
    this.javaClass.findFieldOrNull(findSuper, condition)?.get(this) as T?

/**
 * 通过Descriptor获取属性
 * @param desc Descriptor
 * @param clzLoader 类加载器
 * @return 找到的属性
 * @throws NoSuchFieldException 未找到属性
 */
fun getFieldByDesc(desc: String, clzLoader: ClassLoader = InitFields.ezXClassLoader): Field =
    DexDescriptor.newFieldDesc(desc).getField(clzLoader).apply { isAccessible = true }

/**
 * 扩展函数 通过Descriptor获取属性
 * @param desc Descriptor
 * @return 找到的属性
 * @throws NoSuchFieldException 未找到属性
 */
fun ClassLoader.getFieldByDesc(desc: String): Field = getFieldByDesc(desc, this)

/**
 * 通过Descriptor获取属性
 * @param desc Descriptor
 * @param clzLoader 类加载器
 * @return 找到的属性 未找到则返回null
 */
fun getFieldByDescOrNull(
    desc: String,
    clzLoader: ClassLoader = InitFields.ezXClassLoader
): Field? = runCatching { getFieldByDesc(desc, clzLoader) }.getOrNull()

/**
 * 扩展函数 通过Descriptor获取属性
 * @param desc Descriptor
 * @return 找到的属性 未找到则返回null
 */
fun ClassLoader.getFieldByDescOrNull(desc: String): Field? = getFieldByDescOrNull(desc, this)
