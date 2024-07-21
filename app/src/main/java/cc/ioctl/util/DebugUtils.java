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

import android.view.View;
import android.view.ViewGroup;
import io.github.qauxv.util.xpcompat.XC_MethodHook;
import io.github.qauxv.util.xpcompat.XC_MethodHook.MethodHookParam;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.dexkit.DexMethodDescriptor;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * Handy utils used for debug/development env, not to use in production.
 */
public class DebugUtils {

    private DebugUtils() {
        throw new AssertionError("no instance");
    }

    public static final XC_MethodHook INVOKE_RECORD = new XC_MethodHook(200) {
        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            dumpParam(param);
        }
    };

    public static void dumpParam(MethodHookParam param) {
        Member m = param.method;
        String desc;
        if (m instanceof Constructor) {
            desc = new DexMethodDescriptor((Constructor<?>) m).toString();
        } else {
            desc = new DexMethodDescriptor((Method) m).toString();
        }
        Log.d("+++ DUMP TRACE START");
        Log.d(desc);
        Log.d("argc: " + param.args.length);
        for (int i = 0; i < param.args.length; i++) {
            Log.d("arg" + i + ": " + valueToString(param.args[i]));
        }
        Log.d("ret: " + valueToString(param.getResult()));
        if (param.thisObject != null) {
            Log.d("this object: ");
            dumpObject(param.thisObject);
        }
        Throwable t = new Throwable("Trace dump");
        Log.d(t);
//        Log.d(stackTraceElementsToString(getStackTrace(4,6)));
        Log.d("+++ DUMP TRACE END");
    }

    public static void dumpObject(Object obj) {
        try {
            StringBuilder ret = new StringBuilder("dump object: " + obj + "\n");
            Field[] fs = obj.getClass().getDeclaredFields();
            for (int i = 0; i < fs.length; i++) {
                fs[i].setAccessible(true);
                int mod = fs[i].getModifiers();
                boolean isStatic = (mod & Modifier.STATIC) != 0;
                String accessName = "DEF";
                if ((Modifier.PUBLIC & mod) != 0) {
                    accessName = "PUB";
                } else if ((Modifier.PROTECTED & mod) != 0) {
                    accessName = "PRO";
                } else if ((Modifier.PRIVATE & mod) != 0) {
                    accessName = "PRI";
                }
                Class<?> type = fs[i].getType();
                String shortyName = type.isPrimitive() ? DexMethodDescriptor.getTypeSig(fs[i].getType()) : "L";
                ret.append(i < fs.length - 1 ? "+-" : "--")
                        .append(accessName)
                        .append(' ')
                        .append(isStatic ? "STA" : "   ")
                        .append(' ')
                        .append(shortyName)
                        .append(' ')
                        .append(fs[i].getName())
                        .append(" = ")
                        .append(valueToString(fs[i].get(obj)))
                        .append("\n");
            }
            Log.d(ret.toString());
        } catch (Exception e) {
            Log.d(e);
        }
    }

    public static String valueToString(Object value) {
        String valueString;
        if (value == null) {
            valueString = "null";
        } else {
            Class<?> type = value.getClass();
            if (type == String.class) {
                valueString = en_toStr(value);
            } else if (type.isPrimitive()) {
                valueString = String.valueOf(value);
            } else if (type.isArray()) {
                valueString = primitiveArrayToString(value);
            } else {
                valueString = value.toString();
            }
        }
        return valueString;
    }

    public static void dumpViewHierarchy(View view) {
        StringBuilder sb = new StringBuilder();
        if (view == null) {
            Log.d("view is null");
        } else {
            sb.append(view);
            if (view instanceof ViewGroup) {
                sb.append("\n");
                dumpViewGroupHierarchyImpl((ViewGroup) view, 1, sb);
            }
            Log.d(sb.toString());
        }
    }

    public static String primitiveArrayToString(Object array) {
        if (array == null) {
            return "null";
        }
        if (!array.getClass().isArray()) {
            return array.toString();
        }
        if (array instanceof boolean[]) {
            return Arrays.toString((boolean[]) array);
        }
        if (array instanceof byte[]) {
            return Arrays.toString((byte[]) array);
        }
        if (array instanceof char[]) {
            return Arrays.toString((char[]) array);
        }
        if (array instanceof double[]) {
            return Arrays.toString((double[]) array);
        }
        if (array instanceof float[]) {
            return Arrays.toString((float[]) array);
        }
        if (array instanceof int[]) {
            return Arrays.toString((int[]) array);
        }
        if (array instanceof long[]) {
            return Arrays.toString((long[]) array);
        }
        if (array instanceof short[]) {
            return Arrays.toString((short[]) array);
        }
        return Arrays.toString((Object[]) array);
    }

    private static void dumpViewGroupHierarchyImpl(ViewGroup vg, int currentLevel, StringBuilder ret) {
        if (vg == null) {
            return;
        }
        for (int i = 0; i < vg.getChildCount(); i++) {
            View v = vg.getChildAt(i);
            ret.append(getRepeatString(" +", currentLevel)).append('[').append(i).append(']').append(v).append("\n");
            if (v instanceof ViewGroup) {
                dumpViewGroupHierarchyImpl((ViewGroup) v, currentLevel + 1, ret);
            }
        }
    }

    public static String en_toStr(Object obj) {
        if (obj == null) {
            return null;
        }
        String str;
        if (obj instanceof CharSequence) {
            str = en(obj.toString());
        } else {
            str = "" + obj;
        }
        return str;
    }

    private static String getRepeatString(String str, int count) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < count; i++) {
            ret.append(str);
        }
        return ret.toString();
    }

    public static String en(String str) {
        if (str == null) {
            return "null";
        }
        return "\"" + str.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    public static String csvenc(String s) {
        if (!s.contains("\"") && !s.contains(" ") && !s.contains(",")
                && !s.contains("\r") && !s.contains("\n") && !s.contains("\t")) {
            return s;
        }
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    public static String getPathTail(File path) {
        return getPathTail(path.getPath());
    }

    public static String getPathTail(String path) {
        String[] arr = path.split("/");
        return arr[arr.length - 1];
    }

    public static StackTraceElement[] getStackTrace(int skipDepth, int depth) {
        Throwable t = new Throwable();
        StackTraceElement[] src = t.getStackTrace();
        int start = skipDepth + 2;
        int end = start + depth;
        if (end > src.length) {
            end = src.length;
        }
        StackTraceElement[] ret = new StackTraceElement[end - start];
        System.arraycopy(src, start, ret, 0, ret.length);
        return ret;
    }

    private static String stackTraceElementsToString(StackTraceElement[] elements) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : elements) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}
