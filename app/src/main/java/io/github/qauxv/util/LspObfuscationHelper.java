/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2024 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is an opensource software: you can redistribute it
 * and/or modify it under the terms of the General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the General Public License for more details.
 *
 * You should have received a copy of the General Public License
 * along with this software.
 * If not, see
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */
package io.github.qauxv.util;

public class LspObfuscationHelper {

    private static String sObfuscatedPackageName = null;
    private static String sProbeLsposedNativeApiClassName = "Lorg/lsposed/lspd/nativebridge/NativeAPI;";

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
