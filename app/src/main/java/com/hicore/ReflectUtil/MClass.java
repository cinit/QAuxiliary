package com.hicore.ReflectUtil;

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
        if (clz.isAssignableFrom(convert)) {
            return true;
        }
        return false;
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
