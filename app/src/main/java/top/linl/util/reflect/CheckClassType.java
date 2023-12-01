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

public class CheckClassType {

    //缩小范围匹配字节引用类型
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
