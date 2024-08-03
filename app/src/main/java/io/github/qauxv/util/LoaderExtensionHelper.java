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

import androidx.annotation.Nullable;
import io.github.qauxv.loader.hookapi.IHookBridge;
import io.github.qauxv.loader.hookapi.ILoaderService;
import io.github.qauxv.poststartup.StartupInfo;

public class LoaderExtensionHelper {

    public static final String CMD_GET_XPOSED_BRIDGE_CLASS = "GetXposedBridgeClass";
    private static String sProbeLsposedNativeApiClassName = "Lorg/lsposed/lspd/nativebridge/NativeAPI;";

    private LoaderExtensionHelper() {
    }

    @Nullable
    public static Class<?> getXposedBridgeClass() {
        ILoaderService loaderService = StartupInfo.getLoaderService();
        return (Class<?>) loaderService.queryExtension(CMD_GET_XPOSED_BRIDGE_CLASS);
    }

    public static String getObfuscatedLsposedNativeApiClassName() {
        return sProbeLsposedNativeApiClassName.replace('.', '/').substring(1, sProbeLsposedNativeApiClassName.length() - 1);
    }

    @Nullable
    public static String getXposedBridgeClassName() {
        Class<?> xposedBridgeClass = getXposedBridgeClass();
        if (xposedBridgeClass != null) {
            return xposedBridgeClass.getName();
        } else {
            return null;
        }
    }

    public static long getHookCounter() {
        IHookBridge hookBridge = StartupInfo.getHookBridge();
        if (hookBridge != null) {
            return hookBridge.getHookCounter();
        } else {
            return -1;
        }
    }

}
