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

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.github.kyuubiran.ezxhelper.init.InitFields;
import io.github.qauxv.core.MainHook;
import io.github.qauxv.core.NativeCoreBridge;
import io.github.qauxv.util.HostInfo;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.hookimpl.InMemoryClassLoaderHelper;
import io.github.qauxv.util.hookimpl.LibXposedNewApiByteCodeGenerator;
import io.github.qauxv.util.hookimpl.lsplant.LsplantHookImpl;
import io.github.qauxv.util.soloader.NativeLoader;
import java.io.File;
import java.lang.reflect.Field;

public class StartupRoutine {

    private StartupRoutine() {
        throw new AssertionError("No instance for you!");
    }

    /**
     * From now on, kotlin, androidx or third party libraries may be accessed without crashing the ART.
     * <p>
     * Kotlin and androidx are dangerous, and should be invoked only after the class loader is ready.
     *
     * @param ctx         Application context for host
     * @param step        Step instance
     * @param lpwReserved null, not used
     * @param bReserved   false, not used
     */
    public static void execPostStartupInit(@NonNull Context ctx, @Nullable Object step, String lpwReserved, boolean bReserved) {
        // init all kotlin utils here
        HostInfo.init((Application) ctx);
        Initiator.init(ctx.getClassLoader());
        InitFields.ezXClassLoader = ctx.getClassLoader();
        // resource injection is done somewhere else, do not init it here
        com.github.kyuubiran.ezxhelper.utils.Log.INSTANCE.getCurrentLogger().setLogTag("QAuxv");
        // load and pre-init primary native library -- in case it has not been pre-initialized
        File dataDir = ctx.getDataDir();
        ApplicationInfo ai = ctx.getApplicationInfo();
        NativeLoader.loadPrimaryNativeLibrary(dataDir, ai);
        NativeLoader.primaryNativeLibraryPreInitialize(dataDir, ai, true);
        overrideLSPatchModifiedVersionCodeIfNecessary(ctx);
        // perform full initialization for native core -- including primary and secondary native libraries
        NativeCoreBridge.initNativeCore();
        StartupInfo.getLoaderService().setClassLoaderHelper(InMemoryClassLoaderHelper.INSTANCE);
        LibXposedNewApiByteCodeGenerator.init();
        if (StartupInfo.getHookBridge() == null) {
            Log.w("HookBridge is null, fallback to embedded LSPlant.");
            LsplantHookImpl.initializeLsplantHookBridge();
        }
        MainHook.getInstance().performHook(ctx, step);
    }

    private static void overrideLSPatchModifiedVersionCodeIfNecessary(Context ctx) {
        if (HostInfo.isInHostProcess() && HostInfo.getHostInfo().getVersionCode32() == 1) {
            if ("com.tencent.mobileqq".equals(ctx.getPackageName())) {
                ClassLoader cl = ctx.getClassLoader();
                // try to get version code from Lcooperation/qzone/QUA;->QUA:Ljava/lang/String;
                try {
                    Class<?> kQUA = cl.loadClass("cooperation.qzone.QUA");
                    Field QUA = kQUA.getDeclaredField("QUA");
                    QUA.setAccessible(true);
                    String qua = (String) QUA.get(null);
                    if (qua != null && qua.startsWith("V1_AND_")) {
                        // "V1_AND_SQ_8.9.0_3060_YYB_D"
                        String[] split = qua.split("_");
                        if (split.length >= 5) {
                            int versionCode = Integer.parseInt(split[4]);
                            HostInfo.overrideVersionCodeForLSPatchModified1(versionCode);
                        }
                    }
                } catch (ReflectiveOperationException | NumberFormatException e) {
                    io.github.qauxv.util.Log.e("Failed to get version code from Lcooperation/qzone/QUA;->QUA:Ljava/lang/String;", e);
                }
            }
        }
    }

}
