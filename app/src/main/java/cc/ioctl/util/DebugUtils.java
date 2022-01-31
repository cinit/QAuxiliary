/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
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

import de.robv.android.xposed.XC_MethodHook;
import io.github.qauxv.util.Log;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
 * Handy utils used for debug/development env, not to use in production.
 */
public class DebugUtils {

    public static final XC_MethodHook dummyHook = new XC_MethodHook(200) {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            super.beforeHookedMethod(param);
        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            super.afterHookedMethod(param);
        }
    };
    public static final XC_MethodHook invokeRecord = new XC_MethodHook(200) {
        @Override
        protected void afterHookedMethod(MethodHookParam param)
            throws IllegalAccessException, IllegalArgumentException {
            Member m = param.method;
            StringBuilder ret = new StringBuilder(
                m.getDeclaringClass().getSimpleName() + "->" + ((m instanceof Method) ? m.getName()
                    : "<init>") + "(");
            Class[] argt;
            if (m instanceof Method) {
                argt = ((Method) m).getParameterTypes();
            } else if (m instanceof Constructor) {
                argt = ((Constructor) m).getParameterTypes();
            } else {
                argt = new Class[0];
            }
            for (int i = 0; i < argt.length; i++) {
                if (i != 0) {
                    ret.append(",\n");
                }
                ret.append(param.args[i]);
            }
            ret.append(")=").append(param.getResult());
            Log.i(ret.toString());
            ret = new StringBuilder(
                "↑dump object:" + m.getDeclaringClass().getCanonicalName() + "\n");
            Field[] fs = m.getDeclaringClass().getDeclaredFields();
            for (int i = 0; i < fs.length; i++) {
                fs[i].setAccessible(true);
                ret.append(i < fs.length - 1 ? "├" : "↓").append(fs[i].getName()).append("=")
                    .append(en_toStr(fs[i].get(param.thisObject))).append("\n");
            }
            Log.i(ret.toString());
            Throwable t = new Throwable("Trace dump");
            Log.i(t);
        }
    };

    public static final XC_MethodHook invokeInterceptor = new XC_MethodHook(200) {
        @Override
        protected void beforeHookedMethod(MethodHookParam param)
            throws IllegalAccessException, IllegalArgumentException {
            Member m = param.method;
            StringBuilder ret = new StringBuilder(
                m.getDeclaringClass().getSimpleName() + "->" + ((m instanceof Method) ? m.getName()
                    : "<init>") + "(");
            Class[] argt;
            if (m instanceof Method) {
                argt = ((Method) m).getParameterTypes();
            } else if (m instanceof Constructor) {
                argt = ((Constructor) m).getParameterTypes();
            } else {
                argt = new Class[0];
            }
            for (int i = 0; i < argt.length; i++) {
                if (i != 0) {
                    ret.append(",\n");
                }
                ret.append(param.args[i]);
            }
            ret.append(")=").append(param.getResult());
            Log.i(ret.toString());
            ret = new StringBuilder(
                "↑dump object:" + m.getDeclaringClass().getCanonicalName() + "\n");
            Field[] fs = m.getDeclaringClass().getDeclaredFields();
            for (int i = 0; i < fs.length; i++) {
                fs[i].setAccessible(true);
                ret.append(i < fs.length - 1 ? "├" : "↓").append(fs[i].getName()).append("=")
                    .append(en_toStr(fs[i].get(param.thisObject))).append("\n");
            }
            Log.i(ret.toString());
            Throwable t = new Throwable("Trace dump");
            Log.i(t);
        }
    };

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

    public static String en(String str) {
        if (str == null) {
            return "null";
        }
        return "\"" + str.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    public static String csvenc(String s) {
        if (!s.contains("\"") && !s.contains(" ") && !s.contains(",") && !s.contains("\r") && !s
            .contains("\n") && !s.contains("\t")) {
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
}
