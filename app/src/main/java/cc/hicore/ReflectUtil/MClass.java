/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the qwq233 Universal License
 * as published on https://github.com/qwq233/license; either
 * version 2 of the License, or any later version and our EULA as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See theqwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package cc.hicore.ReflectUtil;

import io.github.qauxv.util.Initiator;
import java.lang.reflect.Constructor;
import java.util.HashMap;

public class MClass {

    private static final HashMap<String, Class<?>> clzMap = new HashMap<>();

    public static Class<?> loadClass(String ClassName) {
        Class<?> clz = clzMap.get(ClassName);
        if (clz != null) {
            return clz;
        }
        try {
            clz = Initiator.loadClass(ClassName);
            clzMap.put(ClassName, clz);
            return clz;
        } catch (Throwable e) {
            return null;
        }
    }

    public static Constructor<?> findCons(Class<?> clz, Class<?>[] paramTypes) {
        Loop:
        for (Constructor<?> con : clz.getDeclaredConstructors()) {
            Class<?>[] CheckParam = con.getParameterTypes();
            if (CheckParam.length != paramTypes.length) {
                continue;
            }
            for (int i = 0; i < paramTypes.length; i++) {
                if (!CheckClass(CheckParam[i], paramTypes[i])) {
                    continue Loop;
                }
            }
            con.setAccessible(true);
            return con;
        }
        return null;
    }

    public static <T> T NewInstance(Class<?> clz, Class<?>[] paramTypes, Object... params) throws Exception {
        Loop:
        for (Constructor<?> con : clz.getDeclaredConstructors()) {
            Class<?>[] CheckParam = con.getParameterTypes();
            if (CheckParam.length != paramTypes.length) {
                continue;
            }
            for (int i = 0; i < paramTypes.length; i++) {
                if (!CheckClass(CheckParam[i], paramTypes[i])) {
                    continue Loop;
                }
            }
            con.setAccessible(true);
            return (T) con.newInstance(params);
        }
        throw new RuntimeException("No Instance for " + clz);
    }

    public static <T> T NewInstance(Class<?> clz, Object... params) throws Exception {
        Class<?>[] paramTypes = new Class[params.length];
        for (int i = 0; i < params.length; i++) {
            paramTypes[i] = params[i].getClass();
        }
        return NewInstance(clz, paramTypes, params);
    }

    public static boolean CheckClass(Class<?> clz, Class<?> convert) {
        if (clz.equals(convert)) {
            return true;
        }
        if (clz.equals(hasType(convert))) {
            return true;
        }
        return clz.isAssignableFrom(convert);
    }

    private static Class<?> hasType(Class<?> clz) {
        try {

            if (clz.equals(Boolean.class)) {
                return boolean.class;
            }
            if (clz.equals(Integer.class)) {
                return int.class;
            }
            if (clz.equals(Long.class)) {
                return long.class;
            }
            if (clz.equals(Byte.class)) {
                return byte.class;
            }
            if (clz.equals(Short.class)) {
                return short.class;
            }
            if (clz.equals(Float.class)) {
                return float.class;
            }
            if (clz.equals(Double.class)) {
                return double.class;
            }
            if (clz.equals(Character.class)) {
                return char.class;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

}
