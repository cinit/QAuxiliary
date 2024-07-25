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

package io.github.qauxv.poststartup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.github.qauxv.loader.hookapi.IHookBridge;
import io.github.qauxv.loader.hookapi.ILoaderService;
import java.util.Objects;

public class StartupInfo {

    private StartupInfo() {
        throw new AssertionError("No instance for you!");
    }

    private static String modulePath;

    private static ILoaderService loaderService;

    private static IHookBridge hookBridge;

    private static Boolean inHostProcess = null;

    @NonNull
    public static String getModulePath() {
        return modulePath;
    }

    @NonNull
    public static ILoaderService getLoaderService() {
        return loaderService;
    }

    @Nullable
    public static IHookBridge getHookBridge() {
        return hookBridge;
    }

    @NonNull
    public static IHookBridge requireHookBridge() {
        if (hookBridge == null) {
            throw new IllegalStateException("HookBridge is not initialized");
        }
        return hookBridge;
    }

    public static void setHookBridge(@Nullable IHookBridge hookBridge) {
        StartupInfo.hookBridge = hookBridge;
    }

    public static void setLoaderService(@NonNull ILoaderService loaderService) {
        Objects.requireNonNull(loaderService);
        StartupInfo.loaderService = loaderService;
    }

    public static void setModulePath(@NonNull String modulePath) {
        Objects.requireNonNull(modulePath);
        StartupInfo.modulePath = modulePath;
    }

    public static boolean isInHostProcess() {
        if (inHostProcess == null) {
            throw new IllegalStateException("Host process status is not initialized");
        }
        return inHostProcess;
    }

    public static void setInHostProcess(boolean inHostProcess) {
        if (StartupInfo.inHostProcess != null) {
            throw new IllegalStateException("Host process status is already initialized");
        }
        StartupInfo.inHostProcess = inHostProcess;
    }

}
