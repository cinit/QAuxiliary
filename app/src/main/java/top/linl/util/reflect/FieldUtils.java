/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
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

package top.linl.util.reflect;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 反射字段工具
 */
public class FieldUtils {

    private static final HashMap<String, Field> FIELD_CACHE = new HashMap<>();

    /**
     * 设置字段内容
     *
     * @param target    运行时对象
     * @param fieldName 字段名称
     * @param value     设置后的字段内容
     */
    public static void setField(Object target, String fieldName, Object value) throws Exception {
        setField(target, target.getClass(), fieldName, value.getClass(), value);
    }

    /**
     * 设置字段内容
     *
     * @param target    运行时对象
     * @param fieldName 字段名称
     * @param fieldType 字段类型
     * @param value     设置后的字段内容
     */
    public static void setField(Object target, String fieldName, Class<?> fieldType, Object value) throws Exception {
        setField(target, target.getClass(), fieldName, fieldType, value);
    }

    /**
     * 设置字段内容
     *
     * @param targetObj 运行时对象
     * @param findClass 字段所处的类
     * @param fieldName 字段名称
     * @param fieldType 字段类型
     * @param value     设置后的字段内容
     */
    public static void setField(Object targetObj,
            Class<?> findClass,
            String fieldName,
            Class<?> fieldType,
            Object value) throws Exception {
        Field field = findField(findClass, fieldName, fieldType);
        field.set(targetObj, value);
    }

    /**
     * 设置首个此类型的字段内容
     *
     * @param targetObj 运行时对象
     * @param findClass 字段所处的类
     * @param fieldType 字段类型
     * @param value     设置后的字段内容
     */
    public static void setFirstField(Object targetObj, Class<?> findClass, Class<?> fieldType, Object value) throws Exception {
        Field field = findFirstField(findClass, fieldType);
        field.set(targetObj, value);
    }

    /**
     * 获取首个类型为fieldType的字段
     *
     * @param targetObj 运行时对象
     * @param fieldName 字段名称
     * @param fieldType 字段类型
     */
    public static <T> T getField(Object targetObj, String fieldName, Class<?> fieldType) throws Exception {
        return getField(targetObj, targetObj.getClass(), fieldName, fieldType);
    }

    /**
     * 获取首个类型为fieldType的字段
     *
     * @param targetObj 运行时对象
     * @param fieldType 字段类型
     */
    public static <T> T getFirstField(Object targetObj, Class<?> fieldType) throws Exception {
        return getFirstField(targetObj, targetObj.getClass(), fieldType);
    }

    /**
     * 获取首个类型为fieldType的字段
     *
     * @param targetObj 运行时对象
     * @param findClass 字段所处的类
     * @param fieldType 字段类型
     */
    public static <T> T getFirstField(Object targetObj, Class<?> findClass, Class<?> fieldType) throws Exception {
        Field field = findFirstField(findClass, fieldType);
        return (T) field.get(targetObj);
    }

    /**
     * 获取字段内容
     *
     * @param targetObj 运行时对象
     * @param findClass 字段所处的类
     * @param fieldName 字段名称
     * @param fieldType 字段类型
     */
    public static <T> T getField(Object targetObj, Class<?> findClass, String fieldName,
            Class<?> fieldType) throws Exception {
        Field field = findField(findClass, fieldName, fieldType);
        return (T) field.get(targetObj);
    }

    /**
     * 查找并获取未知类型的静态字段
     *
     * @param findClass 要查找的字段
     * @param findName  字段名称
     */
    public static <T> T getStaticFieId(Class<?> findClass, String findName) throws Exception {
        Field field = findUnknownTypeField(findClass, findName);
        return (T) field.get(null);
    }

    /**
     * 查找并获取静态字段
     *
     * @param findClass 要查找的字段
     * @param findName  字段名称
     * @param fieldType 字段类型
     */
    public static <T> T getStaticFieId(Class<?> findClass, String findName, Class<?> fieldType) throws Exception {
        Field field = findField(findClass, findName, fieldType);
        return (T) field.get(null);
    }

    /**
     * 获取未知类型的字段属性
     *
     * @param target    运行时对象
     * @param fieldName 字段名
     */
    public static <T> T getUnknownTypeField(Object target, String fieldName) throws Exception {
        Field field = findUnknownTypeField(target.getClass(), fieldName);
        return (T) field.get(target);
    }

    /**
     * 获取未知类型但有字段名的字段
     *
     * @param findClass 查找的类
     * @param fieldName 字段名
     */
    public static Field findUnknownTypeField(Class<?> findClass, String fieldName) throws Exception {
        String key = findClass.getName() + " " + fieldName;
        if (FIELD_CACHE.containsKey(key)) {
            return FIELD_CACHE.get(key);
        }
        Class<?> Check = findClass;
        while (Check != null) {
            for (Field f : Check.getDeclaredFields()) {
                if (f.getName().equals(fieldName)) {
                    f.setAccessible(true);
                    FIELD_CACHE.put(key, f);
                    return f;
                }
            }
            Check = Check.getSuperclass();
        }
        throw new ReflectException("查找不到未知类型但有字段名的字段 " + key);
    }

    /**
     * 查找首个此类型的字段
     *
     * @param findClass 查找的类
     * @param fieldType 查找类型
     */
    public static Field findFirstField(Class<?> findClass, Class<?> fieldType) {
        String fieldSignText = findClass.getName() + " type= " + fieldType.getName();
        if (FIELD_CACHE.containsKey(fieldSignText)) {
            return FIELD_CACHE.get(fieldSignText);
        }
        Class<?> FindClass = findClass;
        while (FindClass != null) {
            for (Field f : FindClass.getDeclaredFields()) {
                if (f.getType() == fieldType) {
                    f.setAccessible(true);
                    FIELD_CACHE.put(fieldSignText, f);
                    return f;
                }
            }
            FindClass = FindClass.getSuperclass();
        }
        throw new ReflectException("查找不到唯一此类型的字段 : " + fieldSignText);
    }

    /**
     * 查找字段
     *
     * @param findClass 查找的类
     * @param fieldName 字段名
     * @param fieldType 字段类型
     */
    public static Field findField(Class<?> findClass, String fieldName, Class<?> fieldType) {
        String fieldSignText = findClass.getName() + " " + fieldType.getName() + " " + fieldName;
        if (FIELD_CACHE.containsKey(fieldSignText)) {
            return FIELD_CACHE.get(fieldSignText);
        }
        Class<?> FindClass = findClass;
        while (FindClass != null) {
            for (Field f : FindClass.getDeclaredFields()) {
                if (f.getName().equals(fieldName) && CheckClassType.CheckClass(f.getType(), (fieldType))) {
                    f.setAccessible(true);
                    FIELD_CACHE.put(fieldSignText, f);
                    return f;
                }
            }
            FindClass = FindClass.getSuperclass();
        }
        throw new ReflectException("查找不到字段 : " + fieldSignText);
    }

    /**
     * 模糊查找字段
     *
     * @param findClass        要查找的类
     * @param lookupConditions 判断字段是否符合条件
     */
    public static Field[] fuzzyLookupFiled(Class<?> findClass, FuzzyLookupConditions lookupConditions) {
        List<Field> fieldList = new ArrayList<>();
        for (Class<?> currentFindClass = findClass; currentFindClass != Object.class; currentFindClass = currentFindClass.getSuperclass()) {
            for (Field field : currentFindClass.getDeclaredFields()) {
                if (lookupConditions.isItCorrect(field)) {
                    field.setAccessible(true);
                    fieldList.add(field);
                }
            }
        }
        if (fieldList.isEmpty()) {
            throw new ReflectException("模糊查找字段异常 : " + findClass.getName());
        }
        return fieldList.toArray(new Field[0]);
    }

    public interface FuzzyLookupConditions {

        boolean isItCorrect(Field currentField);
    }
}
