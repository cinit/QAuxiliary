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
package io.github.qauxv.startup;

import android.content.Context;
import java.net.URL;

/**
 * NOTICE: Do NOT use any androidx annotations here.
 */
public class HybridClassLoader extends ClassLoader {

    private static String sObfuscatedPackageName = null;
    private static String sProbeLsposedNativeApiClassName = "Lorg/lsposed/lspd/nativebridge/NativeAPI;";
    private static final ClassLoader sBootClassLoader = Context.class.getClassLoader();
    private final ClassLoader clPreload;
    private final ClassLoader clBase;

    public HybridClassLoader(ClassLoader x, ClassLoader ctx) {
        clPreload = x;
        clBase = ctx;
    }

    /**
     * 把宿主和模块共有的 package 扔这里.
     *
     * @param name NonNull, class name
     * @return true if conflicting
     */
    public static boolean isConflictingClass(String name) {
        return name.startsWith("androidx.") || name.startsWith("android.support.")
            || name.startsWith("kotlin.") || name.startsWith("kotlinx.")
            || name.startsWith("com.tencent.mmkv.")
            || name.startsWith("com.android.tools.r8.")
            || name.startsWith("com.google.android.")
            || name.startsWith("com.google.gson.")
            || name.startsWith("com.google.common.")
            || name.startsWith("com.google.protobuf.")
            || name.startsWith("com.microsoft.appcenter.")
            || name.startsWith("org.intellij.lang.annotations.")
            || name.startsWith("org.jetbrains.annotations.")
            || name.startsWith("com.bumptech.glide.")
            || name.startsWith("com.google.errorprone.annotations.")
            || name.startsWith("_COROUTINE.");
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            return sBootClassLoader.loadClass(name);
        } catch (ClassNotFoundException ignored) {
        }
        if (name != null && isConflictingClass(name)) {
            //Nevertheless, this will not interfere with the host application,
            //classes in host application SHOULD find with their own ClassLoader, eg Class.forName()
            //use shipped androidx and kotlin lib.
            throw new ClassNotFoundException(name);
        }
        // The ClassLoader for some apk-modifying frameworks are terrible, XposedBridge.class.getClassLoader()
        // is the sane as Context.getClassLoader(), which mess up with 3rd lib, can cause the ART to crash.
        if (clPreload != null) {
            try {
                return clPreload.loadClass(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        if (clBase != null) {
            try {
                return clBase.loadClass(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    public URL getResource(String name) {
        URL ret = clPreload.getResource(name);
        if (ret != null) {
            return ret;
        }
        return clBase.getResource(name);
    }

    public static void setObfuscatedXposedApiPackage(String packageName) {
        sObfuscatedPackageName = packageName;
    }

    public static String getObfuscatedXposedApiPackage() {
        return sObfuscatedPackageName;
    }

    public static String getObfuscatedLsposedNativeApiClassName() {
        return sProbeLsposedNativeApiClassName.replace('.', '/').substring(1, sProbeLsposedNativeApiClassName.length() - 1);
    }

    public static String getXposedBridgeClassName() {
        if (sObfuscatedPackageName == null) {
            return "de.robv.android.xposed.XposedBridge";
        } else {
            var sb = new StringBuilder(sObfuscatedPackageName);
            sb.append(".XposedBridge");
            return sb.toString();
        }
    }
}
