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

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.github.qauxv.loader.hookapi.IHookBridge;
import io.github.qauxv.loader.hookapi.ILoaderService;
import io.github.qauxv.util.IoUtils;
import io.github.qauxv.util.hookimpl.lsplant.LsplantHookImpl;
import io.github.qauxv.util.soloader.NativeLoader;
import java.io.File;
import java.lang.reflect.Field;
import org.lsposed.hiddenapibypass.HiddenApiBypass;

@Keep
public class StartupAgent {

    private static boolean sInitialized = false;

    private StartupAgent() {
        throw new AssertionError("No instance for you!");
    }

    @Keep
    public static void startup(
            @NonNull String modulePath,
            @NonNull String hostDataDir,
            @NonNull ILoaderService loaderService,
            @NonNull ClassLoader hostClassLoader,
            @Nullable IHookBridge hookBridge
    ) {
        if (sInitialized) {
            throw new IllegalStateException("StartupAgent already initialized");
        }
        sInitialized = true;
        if (io.github.qauxv.R.string.res_inject_success >>> 24 == 0x7f) {
            throw new AssertionError("package id must NOT be 0x7f, reject loading...");
        }
        if ("true".equals(System.getProperty(StartupAgent.class.getName()))) {
            android.util.Log.e("QAuxv", "Error: QAuxiliary reloaded??");
            // I don't know... What happened?
            return;
        }
        System.setProperty(StartupAgent.class.getName(), "true");
        StartupInfo.setModulePath(modulePath);
        StartupInfo.setLoaderService(loaderService);
        StartupInfo.setHookBridge(hookBridge);
        StartupInfo.setInHostProcess(true);
        // bypass hidden api
        ensureHiddenApiAccess();
        checkWriteXorExecuteForModulePath(modulePath);
        // we want context
        Context ctx = getBaseApplicationImpl(hostClassLoader);
        if (ctx == null) {
            if (hookBridge == null) {
                initializeHookBridgeForEarlyStartup(hostDataDir);
            }
            StartupHook.getInstance().initializeBeforeAppCreate(hostClassLoader);
        } else {
            StartupHook.getInstance().initializeAfterAppCreate(ctx);
        }
    }

    private static void initializeHookBridgeForEarlyStartup(@NonNull String hostDataDir) {
        if (StartupInfo.getHookBridge() != null) {
            return;
        }
        android.util.Log.w("QAuxv", "initializeHookBridgeForEarlyStartup w/o context");
        File hostDataDirFile = new File(hostDataDir);
        if (!hostDataDirFile.exists()) {
            throw new IllegalStateException("Host data dir not found: " + hostDataDir);
        }
        NativeLoader.loadPrimaryNativeLibrary(hostDataDirFile, null);
        NativeLoader.primaryNativeLibraryPreInitialize(hostDataDirFile, null, true);
        // initialize hook bridge
        LsplantHookImpl.initializeLsplantHookBridge();
    }

    private static void checkWriteXorExecuteForModulePath(@NonNull String modulePath) {
        File moduleFile = new File(modulePath);
        if (moduleFile.canWrite()) {
            android.util.Log.w("QAuxv", "Module path is writable: " + modulePath);
            android.util.Log.w("QAuxv", "This may cause issues on Android 15+, please check your Xposed framework");
        }
    }

    public static Context getBaseApplicationImpl(@NonNull ClassLoader classLoader) {
        Context app;
        try {
            Class<?> clz = classLoader.loadClass("com.tencent.common.app.BaseApplicationImpl");
            Field fsApp = null;
            for (Field f : clz.getDeclaredFields()) {
                if (f.getType() == clz) {
                    fsApp = f;
                    break;
                }
            }
            if (fsApp == null) {
                throw new UnsupportedOperationException("field BaseApplicationImpl.sApplication not found");
            }
            app = (Context) fsApp.get(null);
            return app;
        } catch (ReflectiveOperationException e) {
            android.util.Log.e("QAuxv", "getBaseApplicationImpl: failed", e);
            throw IoUtils.unsafeThrow(e);
        }
    }

    private static void ensureHiddenApiAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !isHiddenApiAccessible()) {
            android.util.Log.w("QAuxv", "Hidden API access not accessible, SDK_INT is " + Build.VERSION.SDK_INT);
            HiddenApiBypass.setHiddenApiExemptions("L");
        }
    }

    @SuppressLint({"BlockedPrivateApi", "PrivateApi"})
    public static boolean isHiddenApiAccessible() {
        Class<?> kContextImpl;
        try {
            kContextImpl = Class.forName("android.app.ContextImpl");
        } catch (ClassNotFoundException e) {
            return false;
        }
        Field mActivityToken = null;
        Field mToken = null;
        try {
            mActivityToken = kContextImpl.getDeclaredField("mActivityToken");
        } catch (NoSuchFieldException ignored) {
        }
        try {
            mToken = kContextImpl.getDeclaredField("mToken");
        } catch (NoSuchFieldException ignored) {
        }
        return mActivityToken != null || mToken != null;
    }

}
