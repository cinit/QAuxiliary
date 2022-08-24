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
package io.github.qauxv.util.dexkit;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class DexFieldDescriptor {

    /**
     * Ljava/lang/Object;
     */
    public final String declaringClass;
    /**
     * shadow$_klass_
     */
    public final String name;
    /**
     * Ljava/lang/Class;
     */
    public final String type;

    public DexFieldDescriptor(String desc) {
        if (desc == null) {
            throw new NullPointerException();
        }
        int a = desc.indexOf("->");
        int b = desc.indexOf(':', a);
        declaringClass = desc.substring(0, a);
        name = desc.substring(a + 2, b);
        type = desc.substring(b + 1);
    }

    public DexFieldDescriptor(String clz, String n, String t) {
        if (clz == null || n == null || t == null) {
            throw new NullPointerException();
        }
        declaringClass = clz;
        name = n;
        type = t;
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
        return declaringClass + "->" + name + ":" + type;
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

    public Field getFieldInstance(ClassLoader classLoader) throws NoSuchMethodException {
        try {
            Class<?> clz = classLoader.loadClass(
                declaringClass.substring(1, declaringClass.length() - 1).replace('/', '.'));
            for (Field f : clz.getDeclaredFields()) {
                if (f.getName().equals(name) && getTypeSig(f.getType()).equals(type)) {
                    return f;
                }
            }
            while ((clz = clz.getSuperclass()) != null) {
                for (Field f : clz.getDeclaredFields()) {
                    if (Modifier.isPrivate(f.getModifiers()) || Modifier
                        .isStatic(f.getModifiers())) {
                        continue;
                    }
                    if (f.getName().equals(name) && getTypeSig(f.getType()).equals(type)) {
                        return f;
                    }
                }
            }
            throw new NoSuchMethodException(declaringClass + "->" + name + ":" + type);
        } catch (ClassNotFoundException e) {
            throw (NoSuchMethodException) new NoSuchMethodException(
                declaringClass + "->" + name + ":" + type).initCause(e);
        }
    }
}
