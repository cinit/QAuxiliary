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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the qwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package cc.hicore.ReflectUtil;

import java.lang.reflect.Field;
import java.util.HashMap;

public class MField {

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
}
