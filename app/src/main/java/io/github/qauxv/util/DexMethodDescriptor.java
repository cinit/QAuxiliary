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
package io.github.qauxv.util;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class DexMethodDescriptor implements Serializable, Cloneable {

    /**
     * Ljava/lang/Object;
     */
    public final String declaringClass;
    /**
     * toString
     */
    public final String name;
    /**
     * ()Ljava/lang/String;
     */
    public final String signature;

    public DexMethodDescriptor(Method method) {
        if (method == null) {
            throw new NullPointerException();
        }
        declaringClass = getTypeSig(method.getDeclaringClass());
        name = method.getName();
        signature = getMethodTypeSig(method);
    }

    public DexMethodDescriptor(String desc) {
        if (desc == null) {
            throw new NullPointerException();
        }
        int a = desc.indexOf("->");
        int b = desc.indexOf('(', a);
        declaringClass = desc.substring(0, a);
        name = desc.substring(a + 2, b);
        signature = desc.substring(b);
    }

    public DexMethodDescriptor(String clz, String n, String s) {
        if (clz == null || n == null || s == null) {
            throw new NullPointerException();
        }
        declaringClass = clz;
        name = n;
        signature = s;
    }

    public DexMethodDescriptor(Class<?> clz, String n, String s) {
        if (clz == null || n == null || s == null) {
            throw new NullPointerException();
        }
        declaringClass = getTypeSig(clz);
        name = n;
        signature = s;
    }

    public static String getMethodTypeSig(final Method method) {
        final StringBuilder buf = new StringBuilder();
        buf.append("(");
        final Class<?>[] types = method.getParameterTypes();
        for (Class<?> type : types) {
            buf.append(getTypeSig(type));
        }
        buf.append(")");
        buf.append(getTypeSig(method.getReturnType()));
        return buf.toString();
    }

    public static String getTypeSig(final Class<?> type) {
        if (type.isPrimitive()) {
            if (Integer.TYPE.equals(type)) {
                return "I";
            }
            if (Void.TYPE.equals(type)) {
                return "V";
            }
            if (Boolean.TYPE.equals(type)) {
                return "Z";
            }
            if (Character.TYPE.equals(type)) {
                return "C";
            }
            if (Byte.TYPE.equals(type)) {
                return "B";
            }
            if (Short.TYPE.equals(type)) {
                return "S";
            }
            if (Float.TYPE.equals(type)) {
                return "F";
            }
            if (Long.TYPE.equals(type)) {
                return "J";
            }
            if (Double.TYPE.equals(type)) {
                return "D";
            }
            throw new IllegalStateException("Type: " + type.getName() + " is not a primitive type");
        }
        if (type.isArray()) {
            return "[" + getTypeSig(type.getComponentType());
        }
        return "L" + type.getName().replace('.', '/') + ";";
    }

    public String getDeclaringClassName() {
        return declaringClass.substring(1, declaringClass.length() - 1).replace('/', '.');
    }

    @Override
    public String toString() {
        return declaringClass + "->" + name + signature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return toString().equals(o.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public Method getMethodInstance(ClassLoader classLoader) throws NoSuchMethodException {
        try {
            Class<?> clz = classLoader.loadClass(
                declaringClass.substring(1, declaringClass.length() - 1).replace('/', '.'));
            for (Method m : clz.getDeclaredMethods()) {
                if (m.getName().equals(name) && getMethodTypeSig(m).equals(signature)) {
                    return m;
                }
            }
            while ((clz = clz.getSuperclass()) != null) {
                for (Method m : clz.getDeclaredMethods()) {
                    if (Modifier.isPrivate(m.getModifiers()) || Modifier
                        .isStatic(m.getModifiers())) {
                        continue;
                    }
                    if (m.getName().equals(name) && getMethodTypeSig(m).equals(signature)) {
                        return m;
                    }
                }
            }
            throw new NoSuchMethodException(declaringClass + "->" + name + signature);
        } catch (ClassNotFoundException e) {
            throw (NoSuchMethodException) new NoSuchMethodException(
                declaringClass + "->" + name + signature).initCause(e);
        }
    }

}
