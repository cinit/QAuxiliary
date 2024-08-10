/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
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

package cc.ioctl.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.github.qauxv.util.xpcompat.XposedBridge;
import io.github.qauxv.util.Natives;
import io.github.qauxv.util.dexkit.DexMethodDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.regex.Pattern;

public class Reflex {

    private Reflex() {
        throw new AssertionError("no instance for you!");
    }

    public static Object getStaticObjectOrNull(Class<?> clazz, String name, Class<?> type) {
        try {
            return getStaticObject(clazz, name, type);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    public static Object getStaticObjectOrNull(Class<?> clazz, String name) {
        return getStaticObjectOrNull(clazz, name, null);
    }

    public static Object getStaticObject(Class<?> clazz, String name) throws NoSuchFieldException {
        return getStaticObject(clazz, name, null);
    }

    public static Object getStaticObject(Class<?> clazz, String name, Class<?> type) throws NoSuchFieldException {
        Field f = findField(clazz, type, name);
        f.setAccessible(true);
        try {
            return f.get(null);
        } catch (IllegalAccessException e) {
            // should not happen
            throw new AssertionError(e);
        }
    }

    public static Object invokeVirtualAny(Object obj, Object... argsTypesAndReturnType)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IllegalArgumentException {
        Class clazz = obj.getClass();
        int argc = argsTypesAndReturnType.length / 2;
        Class[] argt = new Class[argc];
        Object[] argv = new Object[argc];
        Class returnType = null;
        if (argc * 2 + 1 == argsTypesAndReturnType.length) {
            returnType = (Class) argsTypesAndReturnType[argsTypesAndReturnType.length - 1];
        }
        int i, ii;
        Method[] m;
        Method method = null;
        Class[] _argt;
        for (i = 0; i < argc; i++) {
            argt[i] = (Class) argsTypesAndReturnType[argc + i];
            argv[i] = argsTypesAndReturnType[i];
        }
        do {
            m = clazz.getDeclaredMethods();
            loop:
            for (i = 0; i < m.length; i++) {
                _argt = m[i].getParameterTypes();
                if (_argt.length == argt.length) {
                    for (ii = 0; ii < argt.length; ii++) {
                        if (!argt[ii].equals(_argt[ii])) {
                            continue loop;
                        }
                    }
                    if (returnType != null && !returnType.equals(m[i].getReturnType())) {
                        continue;
                    }
                    if (method == null) {
                        method = m[i];
                        //here we go through this class
                    } else {
                        throw new NoSuchMethodException(
                                "Multiple methods found for __attribute__((any))" + paramsTypesToString(
                                        argt) + " in " + obj.getClass().getName());
                    }
                }
            }
        } while (method == null && !Object.class.equals(clazz = clazz.getSuperclass()));
        if (method == null) {
            throw new NoSuchMethodException(
                    "__attribute__((a))" + paramsTypesToString(argt) + " in " + obj.getClass()
                            .getName());
        }
        method.setAccessible(true);
        return method.invoke(obj, argv);
    }

    public static String paramsTypesToString(Class<?>... c) {
        if (c == null) {
            return null;
        }
        if (c.length == 0) {
            return "()";
        }
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < c.length; i++) {
            sb.append(c[i] == null ? "[null]" : c[i].getName());
            if (i != c.length - 1) {
                sb.append(",");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    public static Object invokeStaticAny(Class<?> clazz, Object... argsTypesAndReturnType)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IllegalArgumentException {
        int argc = argsTypesAndReturnType.length / 2;
        Class[] argt = new Class[argc];
        Object[] argv = new Object[argc];
        Class returnType = null;
        if (argc * 2 + 1 == argsTypesAndReturnType.length) {
            returnType = (Class) argsTypesAndReturnType[argsTypesAndReturnType.length - 1];
        }
        int i, ii;
        Method[] m;
        Method method = null;
        Class[] _argt;
        for (i = 0; i < argc; i++) {
            argt[i] = (Class) argsTypesAndReturnType[argc + i];
            argv[i] = argsTypesAndReturnType[i];
        }
        do {
            m = clazz.getDeclaredMethods();
            loop:
            for (i = 0; i < m.length; i++) {
                _argt = m[i].getParameterTypes();
                if (_argt.length == argt.length) {
                    for (ii = 0; ii < argt.length; ii++) {
                        if (!argt[ii].equals(_argt[ii])) {
                            continue loop;
                        }
                    }
                    if (returnType != null && !returnType.equals(m[i].getReturnType())) {
                        continue;
                    }
                    if (method == null) {
                        method = m[i];
                        //here we go through this class
                    } else {
                        throw new NoSuchMethodException("Multiple methods found for __attribute__((any))"
                                + paramsTypesToString(argt) + " in " + clazz.getName());
                    }
                }
            }
        } while (method == null && !Object.class.equals(clazz = clazz.getSuperclass()));
        if (method == null) {
            throw new NoSuchMethodException("__attribute__((a))" + paramsTypesToString(argt) + " in " + clazz.getName());
        }
        method.setAccessible(true);
        return method.invoke(null, argv);
    }

    public static Object invokeVirtualDeclaredModifierAny(Object obj, int requiredMask,
                                                          int excludedMask, Object... argsTypesAndReturnType)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IllegalArgumentException {
        Class clazz = obj.getClass();
        int argc = argsTypesAndReturnType.length / 2;
        Class[] argt = new Class[argc];
        Object[] argv = new Object[argc];
        Class returnType = null;
        if (argc * 2 + 1 == argsTypesAndReturnType.length) {
            returnType = (Class) argsTypesAndReturnType[argsTypesAndReturnType.length - 1];
        }
        int i, ii;
        Method[] m;
        Method method = null;
        Class[] _argt;
        for (i = 0; i < argc; i++) {
            argt[i] = (Class) argsTypesAndReturnType[argc + i];
            argv[i] = argsTypesAndReturnType[i];
        }
        m = clazz.getDeclaredMethods();
        loop:
        for (i = 0; i < m.length; i++) {
            _argt = m[i].getParameterTypes();
            if (_argt.length == argt.length) {
                for (ii = 0; ii < argt.length; ii++) {
                    if (!argt[ii].equals(_argt[ii])) {
                        continue loop;
                    }
                }
                if (returnType != null && !returnType.equals(m[i].getReturnType())) {
                    continue;
                }
                if ((m[i].getModifiers() & requiredMask) != requiredMask) {
                    continue;
                }
                if ((m[i].getModifiers() & excludedMask) != 0) {
                    continue;
                }
                if (method == null) {
                    method = m[i];
                    //here we go through this class
                } else {
                    throw new NoSuchMethodException("Multiple methods found for __attribute__((any))"
                            + paramsTypesToString(argt) + " in " + obj.getClass().getName());
                }
            }
        }
        if (method == null) {
            throw new NoSuchMethodException("__attribute__((a))" + paramsTypesToString(argt) + " in " + obj.getClass().getName());
        }
        method.setAccessible(true);
        return method.invoke(obj, argv);
    }

    /**
     * DO NOT USE, it's fragile instance methods are counted, both public/private, static methods are EXCLUDED, count from 0
     *
     * @param obj
     * @param ordinal                the ordinal of instance method meeting the signature
     * @param expected               how many instance methods are expected there
     * @param argsTypesAndReturnType
     * @return
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public static Object invokeVirtualDeclaredOrdinal(Object obj, int ordinal, int expected,
                                                      boolean strict, Object... argsTypesAndReturnType)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IllegalArgumentException {
        Class clazz = obj.getClass();
        int argc = argsTypesAndReturnType.length / 2;
        Class[] argt = new Class[argc];
        Object[] argv = new Object[argc];
        Class returnType = null;
        if (argc * 2 + 1 == argsTypesAndReturnType.length) {
            returnType = (Class) argsTypesAndReturnType[argsTypesAndReturnType.length - 1];
        }
        int i, ii;
        Method[] m;
        Method[] candidates = new Method[expected];
        int count = 0;
        Class[] _argt;
        for (i = 0; i < argc; i++) {
            argt[i] = (Class) argsTypesAndReturnType[argc + i];
            argv[i] = argsTypesAndReturnType[i];
        }
        m = clazz.getDeclaredMethods();
        loop:
        for (i = 0; i < m.length; i++) {
            _argt = m[i].getParameterTypes();
            if (_argt.length == argt.length) {
                for (ii = 0; ii < argt.length; ii++) {
                    if (!argt[ii].equals(_argt[ii])) {
                        continue loop;
                    }
                }
                if (returnType != null && !returnType.equals(m[i].getReturnType())) {
                    continue;
                }
                if (Modifier.isStatic(m[i].getModifiers())) {
                    continue;
                }
                if (count < expected) {
                    candidates[count++] = m[i];
                } else {
                    if (!strict) {
                        break;
                    }
                    throw new NoSuchMethodException("More methods than expected(" + expected + ") at "
                            + paramsTypesToString(argt) + " in " + obj.getClass().getName());
                }
            }
        }
        if (strict && count != expected) {
            throw new NoSuchMethodException("Less methods(" + count + ") than expected(" + expected + ") at "
                    + paramsTypesToString(argt) + " in " + obj.getClass().getName());
        }
        Arrays.sort(candidates, (o1, o2) -> {
            if (o1 == null && o2 == null) {
                return 0;
            }
            if (o1 == null) {
                return 1;
            }
            if (o2 == null) {
                return -1;
            }
            return strcmp(o1.getName(), o2.getName());
        });
        candidates[ordinal].setAccessible(true);
        return candidates[ordinal].invoke(obj, argv);
    }

    public static int strcmp(String stra, String strb) {
        int len = Math.min(stra.length(), strb.length());
        for (int i = 0; i < len; i++) {
            char a = stra.charAt(i);
            char b = strb.charAt(i);
            if (a != b) {
                return a - b;
            }
        }
        return stra.length() - strb.length();
    }

    /**
     * DO NOT USE, it's fragile instance methods are counted, both public/private, static methods are EXCLUDED, count from 0
     *
     * @param obj
     * @param fixed                  which class
     * @param ordinal                the ordinal of instance method meeting the signature
     * @param expected               how many instance methods are expected there
     * @param argsTypesAndReturnType
     * @return
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public static Object invokeVirtualDeclaredFixedModifierOrdinal(Object obj, int requiredMask, int excludedMask,
                                                                   Class<?> fixed, int ordinal, int expected,
                                                                   boolean strict,
                                                                   Object... argsTypesAndReturnType)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IllegalArgumentException {
        int argc = argsTypesAndReturnType.length / 2;
        Class[] argt = new Class[argc];
        Object[] argv = new Object[argc];
        Class returnType = null;
        if (argc * 2 + 1 == argsTypesAndReturnType.length) {
            returnType = (Class) argsTypesAndReturnType[argsTypesAndReturnType.length - 1];
        }
        int i, ii;
        Method[] m;
        Method[] candidates = new Method[expected];
        int count = 0;
        Class[] _argt;
        for (i = 0; i < argc; i++) {
            argt[i] = (Class) argsTypesAndReturnType[argc + i];
            argv[i] = argsTypesAndReturnType[i];
        }
        m = fixed.getDeclaredMethods();
        loop:
        for (i = 0; i < m.length; i++) {
            _argt = m[i].getParameterTypes();
            if (_argt.length == argt.length) {
                for (ii = 0; ii < argt.length; ii++) {
                    if (!argt[ii].equals(_argt[ii])) {
                        continue loop;
                    }
                }
                if (returnType != null && !returnType.equals(m[i].getReturnType())) {
                    continue;
                }
                if (Modifier.isStatic(m[i].getModifiers())) {
                    continue;
                }
                if ((m[i].getModifiers() & requiredMask) != requiredMask) {
                    continue;
                }
                if ((m[i].getModifiers() & excludedMask) != 0) {
                    continue;
                }
                if (count < expected) {
                    candidates[count++] = m[i];
                } else {
                    if (!strict) {
                        break;
                    }
                    throw new NoSuchMethodException("More methods than expected(" + expected + ") at "
                            + paramsTypesToString(argt) + " in " + obj.getClass().getName());
                }
            }
        }
        if (strict && count != expected) {
            throw new NoSuchMethodException("Less methods(" + count + ") than expected(" + expected + ") at "
                    + paramsTypesToString(argt) + " in " + obj.getClass().getName());
        }
        Arrays.sort(candidates, (o1, o2) -> {
            if (o1 == null && o2 == null) {
                return 0;
            }
            if (o1 == null) {
                return 1;
            }
            if (o2 == null) {
                return -1;
            }
            return strcmp(o1.getName(), o2.getName());
        });
        candidates[ordinal].setAccessible(true);
        return candidates[ordinal].invoke(obj, argv);
    }

    /**
     * DO NOT USE, it's fragile instance methods are counted, both public/private, static methods are EXCLUDED, count from 0
     *
     * @param obj
     * @param ordinal                the ordinal of instance method meeting the signature
     * @param expected               how many instance methods are expected there
     * @param argsTypesAndReturnType
     * @return
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @Deprecated
    public static Object invokeVirtualDeclaredOrdinalModifier(Object obj, int ordinal, int expected,
                                                              boolean strict, int requiredMask, int excludedMask,
                                                              Object... argsTypesAndReturnType)
            throws NoSuchMethodException, InvocationTargetException, IllegalArgumentException {
        Class clazz = obj.getClass();
        int argc = argsTypesAndReturnType.length / 2;
        Class[] argt = new Class[argc];
        Object[] argv = new Object[argc];
        Class returnType = null;
        if (argc * 2 + 1 == argsTypesAndReturnType.length) {
            returnType = (Class) argsTypesAndReturnType[argsTypesAndReturnType.length - 1];
        }
        int i, ii;
        Method[] m;
        Method[] candidates = new Method[expected];
        int count = 0;
        Class[] _argt;
        for (i = 0; i < argc; i++) {
            argt[i] = (Class) argsTypesAndReturnType[argc + i];
            argv[i] = argsTypesAndReturnType[i];
        }
        m = clazz.getDeclaredMethods();
        loop:
        for (i = 0; i < m.length; i++) {
            _argt = m[i].getParameterTypes();
            if (_argt.length == argt.length) {
                for (ii = 0; ii < argt.length; ii++) {
                    if (!argt[ii].equals(_argt[ii])) {
                        continue loop;
                    }
                }
                if (returnType != null && !returnType.equals(m[i].getReturnType())) {
                    continue;
                }
                if (Modifier.isStatic(m[i].getModifiers())) {
                    continue;
                }
                if ((m[i].getModifiers() & requiredMask) != requiredMask) {
                    continue;
                }
                if ((m[i].getModifiers() & excludedMask) != 0) {
                    continue;
                }
                if (count < expected) {
                    candidates[count++] = m[i];
                } else {
                    if (!strict) {
                        break;
                    }
                    throw new NoSuchMethodException("More methods than expected(" + expected + ") at "
                            + paramsTypesToString(argt) + " in " + obj.getClass().getName());
                }
            }
        }
        if (strict && count != expected) {
            throw new NoSuchMethodException("Less methods(" + count + ") than expected(" + expected + ") at "
                    + paramsTypesToString(argt) + " in " + obj.getClass().getName());
        }
        Arrays.sort(candidates, (o1, o2) -> {
            if (o1 == null && o2 == null) {
                return 0;
            }
            if (o1 == null) {
                return 1;
            }
            if (o2 == null) {
                return -1;
            }
            return strcmp(o1.getName(), o2.getName());
        });
        candidates[ordinal].setAccessible(true);
        try {
            return candidates[ordinal].invoke(obj, argv);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * DO NOT USE, it's fragile static methods are counted, both public/private, instance methods are EXCLUDED, count from 0
     *
     * @param clazz
     * @param ordinal                the ordinal of instance method meeting the signature
     * @param expected               how many instance methods are expected there
     * @param argsTypesAndReturnType
     * @return
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @Deprecated
    public static Object invokeStaticDeclaredOrdinalModifier(Class<?> clazz, int ordinal, int expected,
                                                             boolean strict, int requiredMask, int excludedMask,
                                                             Object... argsTypesAndReturnType)
            throws NoSuchMethodException, InvocationTargetException, IllegalArgumentException {
        int argc = argsTypesAndReturnType.length / 2;
        Class[] argt = new Class[argc];
        Object[] argv = new Object[argc];
        Class returnType = null;
        if (argc * 2 + 1 == argsTypesAndReturnType.length) {
            returnType = (Class) argsTypesAndReturnType[argsTypesAndReturnType.length - 1];
        }
        int i, ii;
        Method[] m;
        Method[] candidates = new Method[expected];
        int count = 0;
        Class[] _argt;
        for (i = 0; i < argc; i++) {
            argt[i] = (Class) argsTypesAndReturnType[argc + i];
            argv[i] = argsTypesAndReturnType[i];
        }
        m = clazz.getDeclaredMethods();
        loop:
        for (i = 0; i < m.length; i++) {
            _argt = m[i].getParameterTypes();
            if (_argt.length == argt.length) {
                for (ii = 0; ii < argt.length; ii++) {
                    if (!argt[ii].equals(_argt[ii])) {
                        continue loop;
                    }
                }
                if (returnType != null && !returnType.equals(m[i].getReturnType())) {
                    continue;
                }
                if (!Modifier.isStatic(m[i].getModifiers())) {
                    continue;
                }
                if ((m[i].getModifiers() & requiredMask) != requiredMask) {
                    continue;
                }
                if ((m[i].getModifiers() & excludedMask) != 0) {
                    continue;
                }
                if (count < expected) {
                    candidates[count++] = m[i];
                } else {
                    if (!strict) {
                        break;
                    }
                    throw new NoSuchMethodException("More methods than expected(" + expected + ") at "
                            + paramsTypesToString(argt) + " in " + clazz.getName());
                }
            }
        }
        if (strict && count != expected) {
            throw new NoSuchMethodException("Less methods(" + count + ") than expected(" + expected + ") at "
                    + paramsTypesToString(argt) + " in " + clazz.getName());
        }
        Arrays.sort(candidates, (o1, o2) -> {
            if (o1 == null && o2 == null) {
                return 0;
            }
            if (o1 == null) {
                return 1;
            }
            if (o2 == null) {
                return -1;
            }
            return strcmp(o1.getName(), o2.getName());
        });
        candidates[ordinal].setAccessible(true);
        try {
            return candidates[ordinal].invoke(null, argv);
        } catch (IllegalAccessException e) {
            // should not happen
            throw new AssertionError(e);
        }
    }

    /**
     * DO NOT USE, it's fragile static methods are counted, both public/private, instance methods are EXCLUDED, count from 0
     *
     * @param clazz
     * @param ordinal                the ordinal of instance method meeting the signature
     * @param expected               how many instance methods are expected there
     * @param argsTypesAndReturnType
     * @return
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @Deprecated
    public static Object invokeStaticDeclaredOrdinal(Class clazz, int ordinal, int expected,
                                                     boolean strict, Object... argsTypesAndReturnType)
            throws NoSuchMethodException, InvocationTargetException, IllegalArgumentException {
        int argc = argsTypesAndReturnType.length / 2;
        Class[] argt = new Class[argc];
        Object[] argv = new Object[argc];
        Class returnType = null;
        if (argc * 2 + 1 == argsTypesAndReturnType.length) {
            returnType = (Class) argsTypesAndReturnType[argsTypesAndReturnType.length - 1];
        }
        int i, ii;
        Method[] m;
        Method[] candidates = new Method[expected];
        int count = 0;
        Class[] _argt;
        for (i = 0; i < argc; i++) {
            argt[i] = (Class) argsTypesAndReturnType[argc + i];
            argv[i] = argsTypesAndReturnType[i];
        }
        m = clazz.getDeclaredMethods();
        loop:
        for (i = 0; i < m.length; i++) {
            _argt = m[i].getParameterTypes();
            if (_argt.length == argt.length) {
                for (ii = 0; ii < argt.length; ii++) {
                    if (!argt[ii].equals(_argt[ii])) {
                        continue loop;
                    }
                }
                if (returnType != null && !returnType.equals(m[i].getReturnType())) {
                    continue;
                }
                if (!Modifier.isStatic(m[i].getModifiers())) {
                    continue;
                }
                if (count < expected) {
                    candidates[count++] = m[i];
                } else {
                    if (!strict) {
                        break;
                    }
                    throw new NoSuchMethodException("More methods than expected(" + expected + ") at "
                            + paramsTypesToString(argt) + " in " + clazz.getName());
                }
            }
        }
        if (strict && count != expected) {
            throw new NoSuchMethodException("Less methods(" + count + ") than expected(" + expected + ") at "
                    + paramsTypesToString(argt) + " in " + clazz.getName());
        }
        Arrays.sort(candidates, (o1, o2) -> {
            if (o1 == null && o2 == null) {
                return 0;
            }
            if (o1 == null) {
                return 1;
            }
            if (o2 == null) {
                return -1;
            }
            return strcmp(o1.getName(), o2.getName());
        });
        candidates[ordinal].setAccessible(true);
        try {
            return candidates[ordinal].invoke(null, argv);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public static Object invokeVirtual(Object obj, String name, Object... argsTypesAndReturnType)
            throws ReflectiveOperationException {
        Class clazz = obj.getClass();
        int argc = argsTypesAndReturnType.length / 2;
        Class[] argt = new Class[argc];
        Object[] argv = new Object[argc];
        Class returnType = null;
        if (argc * 2 + 1 == argsTypesAndReturnType.length) {
            returnType = (Class) argsTypesAndReturnType[argsTypesAndReturnType.length - 1];
        }
        int i, ii;
        Method[] m;
        Method method = null;
        Class[] _argt;
        for (i = 0; i < argc; i++) {
            argt[i] = (Class) argsTypesAndReturnType[argc + i];
            argv[i] = argsTypesAndReturnType[i];
        }
        loop_main:
        do {
            m = clazz.getDeclaredMethods();
            loop:
            for (i = 0; i < m.length; i++) {
                if (m[i].getName().equals(name)) {
                    _argt = m[i].getParameterTypes();
                    if (_argt.length == argt.length) {
                        for (ii = 0; ii < argt.length; ii++) {
                            if (!argt[ii].equals(_argt[ii])) {
                                continue loop;
                            }
                        }
                        if (returnType != null && !returnType.equals(m[i].getReturnType())) {
                            continue;
                        }
                        method = m[i];
                        break loop_main;
                    }
                }
            }
        } while (!Object.class.equals(clazz = clazz.getSuperclass()));
        if (method == null) {
            throw new NoSuchMethodException(
                    name + paramsTypesToString(argt) + " in " + obj.getClass().getName());
        }
        method.setAccessible(true);
        return method.invoke(obj, argv);
    }

    public static Object invokeVirtualOriginal(Object obj, String name, Object... argsTypesAndReturnType)
            throws ReflectiveOperationException {
        Class clazz = obj.getClass();
        int argc = argsTypesAndReturnType.length / 2;
        Class[] argt = new Class[argc];
        Object[] argv = new Object[argc];
        Class returnType = null;
        if (argc * 2 + 1 == argsTypesAndReturnType.length) {
            returnType = (Class) argsTypesAndReturnType[argsTypesAndReturnType.length - 1];
        }
        int i, ii;
        Method[] m;
        Method method = null;
        Class[] _argt;
        for (i = 0; i < argc; i++) {
            argt[i] = (Class) argsTypesAndReturnType[argc + i];
            argv[i] = argsTypesAndReturnType[i];
        }
        loop_main:
        do {
            m = clazz.getDeclaredMethods();
            loop:
            for (i = 0; i < m.length; i++) {
                if (m[i].getName().equals(name)) {
                    _argt = m[i].getParameterTypes();
                    if (_argt.length == argt.length) {
                        for (ii = 0; ii < argt.length; ii++) {
                            if (!argt[ii].equals(_argt[ii])) {
                                continue loop;
                            }
                        }
                        if (returnType != null && !returnType.equals(m[i].getReturnType())) {
                            continue;
                        }
                        method = m[i];
                        break loop_main;
                    }
                }
            }
        } while (!Object.class.equals(clazz = clazz.getSuperclass()));
        if (method == null) {
            throw new NoSuchMethodException(name + " in " + obj.getClass().getName());
        }
        method.setAccessible(true);
        Object ret;
        boolean needPatch = false;
        try {
            ret = XposedBridge.invokeOriginalMethod(method, obj, argv);
            return ret;
        } catch (IllegalStateException e) {
            //For SandHook-EdXp: Method not hooked.
            needPatch = true;
        } catch (InvocationTargetException | NullPointerException e) {
            //For TaiChi
            Throwable cause;
            if (e instanceof InvocationTargetException) {
                cause = e.getCause();
            } else {
                cause = e;
            }
            if (cause instanceof NullPointerException) {
                String tr = android.util.Log.getStackTraceString(cause);
                if (tr.indexOf("ExposedBridge.invokeOriginalMethod") != 0
                        || Pattern.compile("me\\.[.a-zA-Z]+\\.invokeOriginalMethod").matcher(tr).find()) {
                    needPatch = true;
                }
            }
            if (!needPatch) {
                throw e;
            }
        }
        //here needPatch is always true
        ret = method.invoke(obj, argv);
        return ret;
    }

    public static Object invokeStatic(Class<?> staticClass, String name, Object... argsTypesAndReturnType)
            throws NoSuchMethodException, InvocationTargetException, IllegalArgumentException {
        Class clazz = staticClass;
        int argc = argsTypesAndReturnType.length / 2;
        Class[] argt = new Class[argc];
        Object[] argv = new Object[argc];
        Class returnType = null;
        if (argc * 2 + 1 == argsTypesAndReturnType.length) {
            returnType = (Class) argsTypesAndReturnType[argsTypesAndReturnType.length - 1];
        }
        int i, ii;
        Method[] m;
        Method method = null;
        Class[] _argt;
        for (i = 0; i < argc; i++) {
            argt[i] = (Class) argsTypesAndReturnType[argc + i];
            argv[i] = argsTypesAndReturnType[i];
        }
        loop_main:
        do {
            m = clazz.getDeclaredMethods();
            loop:
            for (i = 0; i < m.length; i++) {
                if (m[i].getName().equals(name)) {
                    _argt = m[i].getParameterTypes();
                    if (_argt.length == argt.length) {
                        for (ii = 0; ii < argt.length; ii++) {
                            if (!argt[ii].equals(_argt[ii])) {
                                continue loop;
                            }
                        }
                        if (returnType != null && !returnType.equals(m[i].getReturnType())) {
                            continue;
                        }
                        method = m[i];
                        break loop_main;
                    }
                }
            }
        } while (!Object.class.equals(clazz = clazz.getSuperclass()));
        if (method == null) {
            throw new NoSuchMethodException(name + paramsTypesToString(argt));
        }
        method.setAccessible(true);
        try {
            return method.invoke(null, argv);
        } catch (IllegalAccessException e) {
            // should not happen
            throw new AssertionError(e);
        }
    }

    /**
     * Invoke an instance method non-virtually (i.e. without calling the overridden method).
     * <p>
     * Note that only instance methods can be invoked, static methods not allowed.
     *
     * @param obj  the object to invoke the method on
     * @param m    the method to invoke, the method must be declared in the class which you want to invoke
     * @param args the arguments to pass to the method, may be null if the method has no arguments
     * @return the return value of the method
     * @throws IllegalArgumentException  if the arguments are not compatible with the method
     * @throws InvocationTargetException if the method throws an exception
     */
    @Nullable
    public static Object invokeNonVirtual(@NonNull Object obj, @NonNull Method m, Object... args)
            throws IllegalArgumentException, InvocationTargetException {
        Objects.requireNonNull(obj, "obj is null");
        Objects.requireNonNull(m, "m is null");
        // check flags, not abstract, not static
        if ((m.getModifiers() & (Modifier.ABSTRACT | Modifier.STATIC)) != 0) {
            throw new IllegalArgumentException("invalid method modifiers: " + Modifier.toString(m.getModifiers()));
        }
        // check cast
        if (!m.getDeclaringClass().isInstance(obj)) {
            throw new IllegalArgumentException("obj of class " + obj.getClass().getName() + "is not an instance of " + m.getDeclaringClass().getName());
        }
        // check args
        Class<?>[] argt = m.getParameterTypes();
        if (argt.length != 0) {
            if (args == null) {
                throw new IllegalArgumentException("args is null, expected " + paramsTypesToString(argt));
            }
            if (argt.length != args.length) {
                throw new IllegalArgumentException("args length is " + args.length + ", expected " + paramsTypesToString(argt));
            }
            for (int i = 0; i < argt.length; i++) {
                Class<?> c = argt[i];
                if (c.isPrimitive()) {
                    if (args[i] == null) {
                        throw new IllegalArgumentException("args[" + i + "] is null, expected " + c.getName());
                    } else if (!getWrapperClassForPrimitive(c).isInstance(args[i])) {
                        throw new IllegalArgumentException("args[" + i + "] of class " + args[i].getClass().getName()
                                + " is not an instance of " + c.getName());
                    }
                } else {
                    if (args[i] != null && !c.isInstance(args[i])) {
                        throw new IllegalArgumentException("args[" + i + "] of class " + args[i].getClass().getName()
                                + " is not an instance of " + c.getName());
                    }
                }
            }
        }
        Class<?> declaringClass = m.getDeclaringClass();
        String methodSignature = DexMethodDescriptor.getMethodTypeSig(m);
        return Natives.invokeNonVirtualImpl(declaringClass, m.getName(), methodSignature, obj, args);
    }

    /**
     * Allocate a object instance of the specified class without calling the constructor.
     *
     * @param clazz the class to allocate
     * @return the allocated object
     * @throws IllegalArgumentException if the class is not instantiable
     */
    @NonNull
    public static <T> T allocateInstance(@NonNull Class<T> clazz) {
        Objects.requireNonNull(clazz, "clazz is null");
        if (clazz.isPrimitive()) {
            throw new IllegalArgumentException("cannot allocate instance of primitive type: " + clazz.getName());
        }
        if (clazz.isArray()) {
            throw new IllegalArgumentException("cannot allocate instance of array type: " + clazz.getName());
        }
        if (clazz.isInterface()) {
            throw new IllegalArgumentException("cannot allocate instance of interface type: " + clazz.getName());
        }
        return (T) Natives.allocateInstanceImpl(clazz);
    }

    public static Object getInstanceObjectOrNull(Object obj, String name) {
        return getInstanceObjectOrNull(obj, name, null);
    }

    public static <T> T getInstanceObjectOrNull(Object obj, String name, Class<T> type) {
        try {
            Class clazz = obj.getClass();
            Field f = findField(clazz, type, name);
            f.setAccessible(true);
            return (T) f.get(obj);
        } catch (Exception e) {
        }
        return null;
    }

    public static <T> T getInstanceObject(Object obj, String name, Class<T> type) throws NoSuchFieldException {
        Class<?> clazz = obj.getClass();
        Field f = findField(clazz, type, name);
        f.setAccessible(true);
        try {
            return (T) f.get(obj);
        } catch (IllegalAccessException e) {
            // should not happen
            throw new AssertionError(e);
        }
    }

    public static void setInstanceObjectSilently(@NonNull Object obj, @NonNull String name, @Nullable Object value) {
        try {
            setInstanceObject(obj, name, null, value);
        } catch (NoSuchFieldException e) {
        }
    }

    public static void setInstanceObjectSilently(@NonNull Object obj, @NonNull String name,
                                                 @NonNull Class<?> type, @Nullable Object value) {
        try {
            setInstanceObject(obj, name, type, value);
        } catch (NoSuchFieldException e) {
        }
    }

    public static void setInstanceObject(@NonNull Object obj, @NonNull String name, @Nullable Object value) throws NoSuchFieldException {
        setInstanceObject(obj, name, null, value);
    }

    public static void setInstanceObject(@NonNull Object obj, @NonNull String name, @Nullable Class<?> type,
                                         @Nullable Object value) throws NoSuchFieldException {
        Class<?> clazz = obj.getClass();
        Field f = findField(clazz, type, name);
        f.setAccessible(true);
        try {
            f.set(obj, value);
        } catch (IllegalAccessException e) {
            // should not happen
            throw new AssertionError(e);
        }
    }

    public static void setStaticObjectSilently(Class<?> clz, String name, Object value) {
        setStaticObjectSilently(clz, name, null, value);
    }

    public static void setStaticObjectSilently(Class<?> clazz, String name, Class<?> type, Object value) {
        try {
            setStaticObject(clazz, name, type, value);
        } catch (NoSuchFieldException ignored) {
        }
    }

    public static void setStaticObject(Class<?> clz, String name, Object value) throws NoSuchFieldException {
        setStaticObject(clz, name, null, value);
    }

    public static void setStaticObject(Class<?> clazz, String name, Class<?> type, Object value) throws NoSuchFieldException {
        Field f = findField(clazz, type, name);
        f.setAccessible(true);
        try {
            f.set(null, value);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    @NonNull
    public static Method findMethod(@NonNull Class<?> clazz, @Nullable Class<?> returnType,
                                    @NonNull String name, @NonNull Class<?>... paramTypes) throws NoSuchMethodException {
        Objects.requireNonNull(clazz, "clazz == null");
        Objects.requireNonNull(name, "name == null");
        int argc = paramTypes.length;
        Class<?> current = clazz;
        do {
            Method[] methods = current.getDeclaredMethods();
            loop:
            for (Method value : methods) {
                if (value.getName().equals(name)) {
                    Class<?>[] argt = value.getParameterTypes();
                    if (argt.length == argc) {
                        for (int ii = 0; ii < argt.length; ii++) {
                            if (!argt[ii].equals(paramTypes[ii])) {
                                continue loop;
                            }
                        }
                        if (returnType != null && !returnType.equals(value.getReturnType())) {
                            continue;
                        }
                        Method method = value;
                        method.setAccessible(true);
                        return method;
                    }
                }
            }
            current = current.getSuperclass();
        } while (current != null && current != Object.class);
        throw new NoSuchMethodException("No method " + clazz.getName() + "." + name + Arrays.toString(paramTypes) +
                (returnType == null ? "" : " with return type " + returnType.getName()) + " found");
    }

    @NonNull
    public static Method findMethod(@NonNull Class<?> clazz, @NonNull String name,
                                    @NonNull Class<?>... paramTypes) throws NoSuchMethodException {
        return findMethod(clazz, null, name, paramTypes);
    }

    @Nullable
    public static Method findMethodOrNull(@NonNull Class<?> clazz, @Nullable Class<?> returnType,
                                          @NonNull String name, @NonNull Class<?>... paramTypes) {
        try {
            return findMethod(clazz, returnType, name, paramTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @Nullable
    public static Method findMethodOrNull(@NonNull Class<?> clazz, @NonNull String name,
                                          @NonNull Class<?>... paramTypes) {
        return findMethodOrNull(clazz, null, name, paramTypes);
    }

    /**
     * Finds a method with the given return type and parameter types.
     *
     * @param clazz      the class to search in
     * @param returnType the return type of the method, or null to match any return type
     * @param withSuper  whether to search in superclasses
     * @param paramTypes the parameter types of the method
     * @return the method
     * @throws NoSuchMethodException if no matching method is found, or more than one matching method is found
     */
    @NonNull
    public static Method findSingleMethod(@NonNull Class<?> clazz, @Nullable Class<?> returnType,
                                          boolean withSuper, @NonNull Class<?>... paramTypes) throws NoSuchMethodException {
        Objects.requireNonNull(clazz, "clazz == null");
        int argc = paramTypes.length;
        Class<?> current = clazz;
        Method candidateMethod = null;
        do {
            Method[] methods = current.getDeclaredMethods();
            loop:
            for (Method method : methods) {
                if (returnType == null || returnType == method.getReturnType()) {
                    Class<?>[] argt = method.getParameterTypes();
                    if (argt.length == argc) {
                        for (int ii = 0; ii < argt.length; ii++) {
                            if (!argt[ii].equals(paramTypes[ii])) {
                                continue loop;
                            }
                        }
                        if (candidateMethod != null) {
                            throw new NoSuchMethodException("Multiple methods " + clazz.getName() + ".*" + Arrays.toString(paramTypes) +
                                    (returnType == null ? "" : " with return type " + returnType.getName()) + " found");
                        } else {
                            candidateMethod = method;
                            candidateMethod.setAccessible(true);
                        }
                    }
                }
            }
            current = current.getSuperclass();
        } while ((candidateMethod == null && withSuper) && current != null && current != Object.class);
        if (candidateMethod == null) {
            throw new NoSuchMethodException("No method " + clazz.getName() + ".*" + Arrays.toString(paramTypes) +
                    (returnType == null ? "" : " with return type " + returnType.getName()) + " found");
        }
        return candidateMethod;
    }

    @NonNull
    public static Field findSingleField(@NonNull Class<?> clazz, @NonNull Class<?> type, boolean withSuper) throws NoSuchFieldException {
        Objects.requireNonNull(clazz, "clazz == null");
        Objects.requireNonNull(type, "type == null");
        Class<?> clz = clazz;
        Field candidateField = null;
        do {
            for (Field field : clz.getDeclaredFields()) {
                if (field.getType() == type) {
                    if (candidateField != null) {
                        throw new NoSuchFieldException("Multiple fields of type " + type.getName() + " found in " + clazz.getName());
                    } else {
                        candidateField = field;
                    }
                }
            }
        } while ((candidateField == null && withSuper) && (clz = clz.getSuperclass()) != null);
        if (candidateField == null) {
            throw new NoSuchFieldException("No field of type " + type.getName() + " found in " + clazz.getName());
        }
        return candidateField;
    }

    /**
     * Finds a method with the given return type and parameter types.
     *
     * @param clazz      the class to search in
     * @param returnType the return type of the method, or null to match any return type
     * @param withSuper  whether to search in superclasses
     * @param paramTypes the parameter types of the method
     * @return the method, or null if no matching method is found
     */
    @Nullable
    public static Method findSingleMethodOrNull(@NonNull Class<?> clazz, @Nullable Class<?> returnType,
                                                boolean withSuper, @NonNull Class<?>... paramTypes) {
        try {
            return findSingleMethod(clazz, returnType, withSuper, paramTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @NonNull
    public static Object newInstance(@NonNull Class<?> clazz, Object... argsAndTypes) throws ReflectiveOperationException {
        int argc = argsAndTypes.length / 2;
        Class<?>[] argt = new Class[argc];
        Object[] argv = new Object[argc];
        int i;
        Constructor<?> m;
        for (i = 0; i < argc; i++) {
            argt[i] = (Class<?>) argsAndTypes[argc + i];
            argv[i] = argsAndTypes[i];
        }
        m = clazz.getDeclaredConstructor(argt);
        m.setAccessible(true);
        try {
            return m.newInstance(argv);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public static Field findField(Class<?> clazz, Class<?> type, String name) throws NoSuchFieldException {
        Objects.requireNonNull(clazz, "clazz == null");
        Objects.requireNonNull(name, "name == null");
        Class<?> clz = clazz;
        do {
            for (Field field : clz.getDeclaredFields()) {
                if ((type == null || field.getType().equals(type)) && field.getName().equals(name)) {
                    field.setAccessible(true);
                    return field;
                }
            }
        } while ((clz = clz.getSuperclass()) != null);
        String errMsg = type == null ? ("field '" + name + "' not found in " + clazz.getName())
                : ("field '" + name + "' of type " + type.getName() + " not found in " + clazz.getName());
        throw new NoSuchFieldException(errMsg);
    }

    @Nullable
    public static Field findFieldOrNull(Class<?> clazz, Class<?> type, String name) {
        Objects.requireNonNull(clazz, "clazz == null");
        Objects.requireNonNull(name, "name == null");
        Class<?> clz = clazz;
        do {
            for (Field field : clz.getDeclaredFields()) {
                if ((type == null || field.getType().equals(type)) && field.getName().equals(name)) {
                    field.setAccessible(true);
                    return field;
                }
            }
        } while ((clz = clz.getSuperclass()) != null);
        return null;
    }

    @Nullable
    public static Field findFirstDeclaredInstanceFieldByTypeOrNull(@NonNull Class<?> clazz, @NonNull Class<?> type) {
        Objects.requireNonNull(clazz, "clazz == null");
        Objects.requireNonNull(type, "type == null");
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getType() == type && !Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }

    @NonNull
    public static Field findFirstDeclaredInstanceFieldByType(@NonNull Class<?> clazz, @NonNull Class<?> type) throws NoSuchFieldException {
        Objects.requireNonNull(clazz, "clazz == null");
        Objects.requireNonNull(type, "type == null");
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getType() == type && !Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new NoSuchFieldException("field of type " + type.getName() + " not found in " + clazz.getName());
    }

    @Nullable
    public static Field findFirstDeclaredStaticFieldByTypeOrNull(@NonNull Class<?> clazz, @NonNull Class<?> type) {
        Objects.requireNonNull(clazz, "clazz == null");
        Objects.requireNonNull(type, "type == null");
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getType() == type && Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }

    public static Method findMethodByTypes_1(Class<?> clazz, Class returnType, Class... argt)
            throws NoSuchMethodException {
        Method method = null;
        Method[] m;
        Class[] _argt;
        do {
            m = clazz.getDeclaredMethods();
            loop:
            for (Method value : m) {
                _argt = value.getParameterTypes();
                if (_argt.length == argt.length) {
                    for (int ii = 0; ii < argt.length; ii++) {
                        if (!argt[ii].equals(_argt[ii])) {
                            continue loop;
                        }
                    }
                    if (returnType != null && !returnType.equals(value.getReturnType())) {
                        continue;
                    }
                    if (method == null) {
                        method = value;
                        //here we go through this class
                    } else {
                        throw new NoSuchMethodException("Multiple methods found for __attribute__((any))"
                                + paramsTypesToString(argt) + " in " + clazz.getName());
                    }
                }
            }
        } while (method == null && !Object.class.equals(clazz = clazz.getSuperclass()));
        if (method == null) {
            throw new NoSuchMethodException("__attribute__((a))" + paramsTypesToString(argt) + " in " + clazz.getName());
        }
        method.setAccessible(true);
        return method;
    }

    public static Field hasField(Object obj, String name) throws ReflectiveOperationException {
        return hasField(obj, name, null);
    }

    public static Field hasField(Object obj, String name, Class<?> type) throws ReflectiveOperationException {
        if (obj == null) {
            throw new NullPointerException("obj/class == null");
        }
        Class<?> clazz;
        if (obj instanceof Class) {
            clazz = (Class<?>) obj;
        } else {
            clazz = obj.getClass();
        }
        return findField(clazz, type, name);
    }

    public static <T> T getFirstByType(Object obj, Class<T> type) throws NoSuchFieldException {
        if (obj == null) {
            throw new NullPointerException("obj == null");
        }
        if (type == null) {
            throw new NullPointerException("type == null");
        }
        Class<?> clz = obj.getClass();
        while (clz != null && !clz.equals(Object.class)) {
            for (Field f : clz.getDeclaredFields()) {
                if (!f.getType().equals(type)) {
                    continue;
                }
                f.setAccessible(true);
                try {
                    return (T) f.get(obj);
                } catch (IllegalAccessException ignored) {
                    // should not happen
                }
            }
            clz = clz.getSuperclass();
        }
        throw new NoSuchFieldException("No field of type " + type.getName() + " found in "
                + obj.getClass().getName() + " and superclasses");
    }

    public static <T> T getFirstByTypeOrNull(Object obj, Class<T> type) {
        try {
            return getFirstByType(obj, type);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    /**
     * NSF: Neither Static nor Final
     *
     * @param obj  thisObj
     * @param type Field type
     * @return the FIRST(as declared seq in dex) field value meeting the type
     */
    //@Deprecated
    public static <T> T getFirstNSFByType(Object obj, Class<T> type) {
        if (obj == null) {
            throw new NullPointerException("obj == null");
        }
        if (type == null) {
            throw new NullPointerException("type == null");
        }
        Class clz = obj.getClass();
        while (clz != null && !clz.equals(Object.class)) {
            for (Field f : clz.getDeclaredFields()) {
                if (!f.getType().equals(type)) {
                    continue;
                }
                int m = f.getModifiers();
                if (Modifier.isStatic(m) || Modifier.isFinal(m)) {
                    continue;
                }
                f.setAccessible(true);
                try {
                    return (T) f.get(obj);
                } catch (IllegalAccessException ignored) {
                    //should not happen
                }
            }
            clz = clz.getSuperclass();
        }
        return null;
    }

    /**
     * NSF: Neither Static nor Final
     *
     * @param clz  Obj class
     * @param type Field type
     * @return the FIRST(as declared seq in dex) field value meeting the type
     */
    //@Deprecated
    public static Field getFirstNSFFieldByType(Class clz, Class type) {
        if (clz == null) {
            throw new NullPointerException("clz == null");
        }
        if (type == null) {
            throw new NullPointerException("type == null");
        }
        while (clz != null && !clz.equals(Object.class)) {
            for (Field f : clz.getDeclaredFields()) {
                if (!f.getType().equals(type)) {
                    continue;
                }
                int m = f.getModifiers();
                if (Modifier.isStatic(m) || Modifier.isFinal(m)) {
                    continue;
                }
                f.setAccessible(true);
                return f;
            }
            clz = clz.getSuperclass();
        }
        return null;
    }

    public static boolean isCallingFrom(@NonNull String classname) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTraceElements) {
            if (element.toString().contains(classname)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCallingFromEither(@NonNull String... classname) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTraceElements) {
            for (String name : classname) {
                if (element.toString().contains(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    @NonNull
    public static Class<?> getWrapperClassForPrimitive(Class<?> primitiveClass) {
        if (primitiveClass == null) {
            throw new NullPointerException("primitiveClass == null");
        }
        if (primitiveClass.isPrimitive()) {
            if (primitiveClass == int.class) {
                return Integer.class;
            }
            if (primitiveClass == long.class) {
                return Long.class;
            }
            if (primitiveClass == float.class) {
                return Float.class;
            }
            if (primitiveClass == double.class) {
                return Double.class;
            }
            if (primitiveClass == boolean.class) {
                return Boolean.class;
            }
            if (primitiveClass == short.class) {
                return Short.class;
            }
            if (primitiveClass == byte.class) {
                return Byte.class;
            }
            if (primitiveClass == char.class) {
                return Character.class;
            }
            if (primitiveClass == void.class) {
                return Void.class;
            }
            throw new AssertionError("Unknown primitive class: " + primitiveClass);
        } else {
            throw new IllegalArgumentException("Not a primitive class: " + primitiveClass.getName());
        }
    }

    @NonNull
    public static String getShortClassName(Object obj) {
        String name;
        if (obj == null) {
            return "null";
        }
        if (obj instanceof String) {
            name = ((String) obj).replace("/", ".");
        } else if (obj instanceof Class) {
            name = ((Class<?>) obj).getName();
        } else if (obj instanceof Field) {
            name = ((Field) obj).getType().getName();
        } else {
            name = obj.getClass().getName();
        }
        if (!name.contains(".")) {
            return name;
        }
        int p = name.lastIndexOf('.');
        return name.substring(p + 1);
    }

    @NonNull
    public static Member virtualMethodLookup(@NonNull Member member, @Nullable Object thiz) {
        Objects.requireNonNull(member, "member == null");
        if (member instanceof Method) {
            return virtualMethodLookup((Method) member, thiz);
        } else {
            return member;
        }
    }

    public static boolean isClassArrayEquals(Class<?>[] a, Class<?>[] b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    @NonNull
    public static Method virtualMethodLookup(@NonNull Method method, @Nullable Object thiz) {
        if ((method.getModifiers() & (Modifier.STATIC | Modifier.PRIVATE)) != 0) {
            // direct method
            return method;
        }
        if (thiz == null) {
            throw new NullPointerException("thiz == null");
        }
        Class<?> current = thiz.getClass();
        Class<?> declaringClass = method.getDeclaringClass();
        // check class
        declaringClass.cast(thiz);
        Class<?> returnType = method.getReturnType();
        String name = method.getName();
        Class<?>[] argt = method.getParameterTypes();
        // start lookup
        do {
            Method[] methods = current.getDeclaredMethods();
            // only compare virtual methods(non-static and non-private)
            for (Method value : methods) {
                boolean isVirtual = !Modifier.isStatic(value.getModifiers()) && !Modifier.isPrivate(value.getModifiers());
                if (isVirtual && value.getName().equals(name) && returnType == value.getReturnType()) {
                    if (isClassArrayEquals(value.getParameterTypes(), argt)) {
                        return value;
                    }
                }
            }
            // TODO: 2024-08-04 support interface default method
            // stop at declaring class
        } while ((current = current.getSuperclass()) != null && current != declaringClass);
        return method;
    }


    public static void dumpClassLoaderRecursive(ClassLoader cl) {
        HashSet<ClassLoader> dumped = new HashSet<>();
        int[] id = new int[1];
        dumpClassLoaderRecursive(cl, dumped, id);
    }

    public static void dumpClassLoaderRecursive(ClassLoader cl, HashSet<ClassLoader> dumped, int[] id) {
        if (cl == null) {
            return;
        }
        ClassLoader parent = cl.getParent();
        Class<?> klass = cl.getClass();
        String objectName = objectToString(cl);
        String toString = cl.toString();
        dumped.add(cl);
        int thisId = id[0]++;
        XposedBridge.log("ClassLoader [" + thisId + "] " + objectName + " -> " + toString + "\n"
                + " --> parent: " + objectToString(parent) + "\n"
                + " --> $class.classLoader: " + objectToString(klass.getClassLoader()));
        if (parent != null && !dumped.contains(parent)) {
            dumpClassLoaderRecursive(parent, dumped, id);
        }
        if (klass.getClassLoader() != null && !dumped.contains(klass.getClassLoader())) {
            dumpClassLoaderRecursive(klass.getClassLoader(), dumped, id);
        }
    }

    public static String objectToString(Object o) {
        if (o == null) {
            return "null";
        } else {
            return o.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(o));
        }
    }

}
