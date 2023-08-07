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

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit;
import io.github.qauxv.core.MainHook;
import io.github.qauxv.core.NativeCoreBridge;
import io.github.qauxv.util.HostInfo;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Natives;
import java.lang.reflect.Field;
import org.lsposed.hiddenapibypass.HiddenApiBypass;

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
    public static void execPostStartupInit(Context ctx, Object step, String lpwReserved, boolean bReserved) {
        ensureHiddenApiAccess();
        // init all kotlin utils here
        EzXHelperInit.INSTANCE.initZygote(HookEntry.getInitZygoteStartupParam());
        EzXHelperInit.INSTANCE.initHandleLoadPackage(HookEntry.getLoadPackageParam());
        // resource injection is done somewhere else, do not init it here
        EzXHelperInit.INSTANCE.initAppContext(ctx, false, false);
        EzXHelperInit.INSTANCE.setLogTag("QAuxv");
        HostInfo.init((Application) ctx);
        Initiator.init(ctx.getClassLoader());
        Natives.load(ctx);
        overrideLSPatchModifiedVersionCodeIfNecessary(ctx);
        NativeCoreBridge.initNativeCore(ctx.getPackageName(), Build.VERSION.SDK_INT,
                HostInfo.getHostInfo().getVersionName(), HostInfo.getHostInfo().getVersionCode());
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
