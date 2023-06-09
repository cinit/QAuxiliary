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

package cc.hicore.ReflectUtil;

import java.lang.reflect.Field;
import java.util.HashMap;

public class MField {

    private static final HashMap<String, Field> FieldCache = new HashMap<>();

    public static void SetField(Object CheckObj, String FieldName, Object Value) throws Exception {
        SetField(CheckObj, CheckObj.getClass(), FieldName, Value.getClass(), Value);
    }

    public static void SetField(Object CheckObj, String FieldName, Class<?> CheckClass, Object Value) throws Exception {
        SetField(CheckObj, CheckObj.getClass(), FieldName, CheckClass, Value);
    }

    public static <T> T GetField(Object CheckObj, String FieldName) throws Exception {
        Class<?> clz = CheckObj.getClass();
        String SignText = clz.getName() + ":" + FieldName;
        if (FieldCache.containsKey(SignText)) {
            Field f = FieldCache.get(SignText);
            return (T) f.get(CheckObj);
        }
        Class<?> Check = clz;
        while (Check != null) {
            for (Field f : Check.getDeclaredFields()) {
                if (f.getName().equals(FieldName)) {
                    f.setAccessible(true);
                    FieldCache.put(SignText, f);
                    return (T) f.get(CheckObj);
                }
            }
            Check = Check.getSuperclass();
        }
        throw new RuntimeException("Can't find field " + FieldName + " in class " + clz.getName());
    }

    public static <T> T GetField(Object CheckObj, String FieldName, Class<?> FieldType) throws Exception {
        return GetField(CheckObj, CheckObj.getClass(), FieldName, FieldType);
    }

    public static <T> T GetStaticField(Class<?> clz, String FieldName) {
        try {
            Class<?> checkClz = clz;
            while (checkClz != null) {
                for (Field f : clz.getDeclaredFields()) {
                    if (f.getName().equals(FieldName)) {
                        f.setAccessible(true);
                        return (T) f.get(null);
                    }
                }
                checkClz = checkClz.getSuperclass();
            }
        } catch (Exception ignored) {
        }
        throw new RuntimeException("Can't find field " + FieldName + " in class " + clz);
    }

    public static void SetField(Object CheckObj, Class<?> CheckClass, String FieldName, Class<?> FieldClass, Object Value) throws Exception {
        String SignText = CheckClass.getName() + ":" + FieldName + "(" + FieldClass.getName() + ")";
        if (FieldCache.containsKey(SignText)) {
            Field f = FieldCache.get(SignText);
            f.set(CheckObj, Value);
            return;
        }

        Class<?> Check = CheckClass;
        while (Check != null) {
            for (Field f : Check.getDeclaredFields()) {
                if (f.getName().equals(FieldName)) {
                    if (MClass.CheckClass(f.getType(), FieldClass)) {
                        f.setAccessible(true);
                        FieldCache.put(SignText, f);
                        f.set(CheckObj, Value);
                        return;
                    }
                }
            }
            Check = Check.getSuperclass();
        }
        throw new RuntimeException("Can't find field " + FieldName + "(" + FieldClass.getName() + ") in class " + CheckClass.getName());
    }

    public static <T> T GetField(Object CheckObj, Class<?> CheckClass, String FieldName, Class<?> FieldClass) throws Exception {
        String SignText = CheckClass.getName() + ":" + FieldName + "(" + FieldClass.getName() + ")";
        if (FieldCache.containsKey(SignText)) {
            Field f = FieldCache.get(SignText);
            return (T) f.get(CheckObj);
        }

        Class<?> Check = CheckClass;
        while (Check != null) {
            for (Field f : Check.getDeclaredFields()) {
                if (f.getName().equals(FieldName)) {
                    if (MClass.CheckClass(f.getType(), FieldClass)) {
                        f.setAccessible(true);
                        FieldCache.put(SignText, f);
                        return (T) f.get(CheckObj);
                    }
                }
            }
            Check = Check.getSuperclass();
        }
        throw new RuntimeException("Can't find field " + FieldName + "(" + FieldClass.getName() + ") in class " + CheckClass.getName());
    }

    public static <T> T GetFirstField(Object CheckObj, Class<?> CheckClass, Class<?> FieldClass) throws Exception {
        String SignText = CheckClass.getName() + ":!NoName!" + "(" + FieldClass.getName() + ")";
        if (FieldCache.containsKey(SignText)) {
            Field f = FieldCache.get(SignText);
            return (T) f.get(CheckObj);
        }

        Class<?> Check = CheckClass;
        while (Check != null) {
            for (Field f : Check.getDeclaredFields()) {
                if (FieldClass == f.getType()) {
                    f.setAccessible(true);
                    FieldCache.put(SignText, f);
                    return (T) f.get(CheckObj);
                }
            }
            Check = Check.getSuperclass();
        }
        throw new RuntimeException("Can't find field " + "(" + FieldClass.getName() + ") in class " + CheckClass.getName());
    }

    public static <T> T GetRoundField(Object CheckObj, Class<?> CheckClass, Class<?> FieldClass, int Round) throws Exception {
        int pos = 0;
        String SignText = CheckClass.getName() + ":!NoName!" + "(" + FieldClass.getName() + ")" + Round;
        if (FieldCache.containsKey(SignText)) {
            Field f = FieldCache.get(SignText);
            return (T) f.get(CheckObj);
        }

        Class<?> Check = CheckClass;
        while (Check != null) {
            for (Field f : Check.getDeclaredFields()) {
                if (MClass.CheckClass(f.getType(), FieldClass)) {
                    if (pos != Round) {
                        pos++;
                        continue;
                    }
                    f.setAccessible(true);
                    FieldCache.put(SignText, f);
                    return (T) f.get(CheckObj);
                }
            }
            Check = Check.getSuperclass();
        }
        throw new RuntimeException("Can't find field " + "(" + FieldClass.getName() + ") in class " + CheckClass.getName());
    }

    public static <T> T GetFirstField(Object CheckObj, Class<?> FieldClass) throws Exception {
        return GetFirstField(CheckObj, CheckObj.getClass(), FieldClass);
    }

    public static Field FindField(Class<?> ObjClass, String FieldName, Class<?> FieldType) {
        Class<?> FindClass = ObjClass;
        while (FindClass != null) {
            for (Field f : FindClass.getDeclaredFields()) {
                if (f.getName().equals(FieldName) && f.getType().equals(FieldType)) {
                    return f;
                }
            }
            FindClass = FindClass.getSuperclass();
        }
        return null;
    }
}
