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

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.qauxv.R;
import io.github.qauxv.util.hookstatus.HookStatusInit;

/**
 * Xposed entry class DO NOT MODIFY ANY CODE HERE UNLESS NECESSARY. DO NOT INVOKE ANY METHOD THAT MAY GET IN TOUCH WITH
 * KOTLIN HERE. DO NOT TOUCH ANDROIDX OR KOTLIN HERE, WHATEVER DIRECTLY OR INDIRECTLY. THIS CLASS SHOULD ONLY CALL
 * {@code StartupHook.getInstance().doInit()} AND RETURN GRACEFULLY. OTHERWISE SOMETHING MAY HAPPEN BECAUSE OF A
 * NON-STANDARD PLUGIN CLASSLOADER.
 *
 * @author kinit
 */
public class HookEntry implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    public static final String PACKAGE_NAME_QQ = "com.tencent.mobileqq";
    public static final String PACKAGE_NAME_QQ_INTERNATIONAL = "com.tencent.mobileqqi";
    public static final String PACKAGE_NAME_QQ_LITE = "com.tencent.qqlite";
    public static final String PACKAGE_NAME_QQ_HD = "com.tencent.minihd.qq";
    public static final String PACKAGE_NAME_TIM = "com.tencent.tim";
    public static final String PACKAGE_NAME_SELF = "io.github.qauxv";
    public static final String PACKAGE_NAME_XPOSED_INSTALLER = "de.robv.android.xposed.installer";

    private static XC_LoadPackage.LoadPackageParam sLoadPackageParam = null;
    private static IXposedHookZygoteInit.StartupParam sInitZygoteStartupParam = null;
    private static String sModulePath = null;

    /**
     * *** No kotlin code should be invoked here.*** May cause a crash.
     */
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (R.string.res_inject_success >>> 24 == 0x7f) {
            XposedBridge.log("package id must NOT be 0x7f, reject loading...");
            return;
        }
        sLoadPackageParam = lpparam;
        // check LSPosed dex-obfuscation
        Class<?> kXposedBridge = XposedBridge.class;
        if (!"de.robv.android.xposed.XposedBridge".equals(kXposedBridge.getName())) {
            String className = kXposedBridge.getName();
            String pkgName = className.substring(0, className.lastIndexOf('.'));
            HybridClassLoader.setObfuscatedXposedApiPackage(pkgName);
        }
        switch (lpparam.packageName) {
            case PACKAGE_NAME_SELF: {
                HookStatusInit.init(lpparam.classLoader);
                break;
            }
            case PACKAGE_NAME_TIM:
            case PACKAGE_NAME_QQ:
            case PACKAGE_NAME_QQ_HD:
            case PACKAGE_NAME_QQ_LITE: {
                if (sInitZygoteStartupParam == null) {
                    throw new IllegalStateException("handleLoadPackage: sInitZygoteStartupParam is null");
                }
                StartupHook.getInstance().doInit(lpparam.classLoader);
                break;
            }
            case PACKAGE_NAME_QQ_INTERNATIONAL: {
                //coming...
                break;
            }
            default:
                break;
        }
    }

    /**
     * *** No kotlin code should be invoked here.*** May cause a crash.
     */
    @Override
    public void initZygote(StartupParam startupParam) {
        sInitZygoteStartupParam = startupParam;
        sModulePath = startupParam.modulePath;
    }

    /**
     * Get the {@link XC_LoadPackage.LoadPackageParam} of the current module.
     * <p>
     * Do NOT add @NonNull annotation to this method. *** No kotlin code should be invoked here.*** May cause a crash.
     *
     * @return the lpparam
     */
    public static XC_LoadPackage.LoadPackageParam getLoadPackageParam() {
        if (sLoadPackageParam == null) {
            throw new IllegalStateException("LoadPackageParam is null");
        }
        return sLoadPackageParam;
    }

    /**
     * Get the path of the current module.
     * <p>
     * Do NOT add @NonNull annotation to this method. *** No kotlin code should be invoked here.*** May cause a crash.
     *
     * @return the module path
     */
    public static String getModulePath() {
        if (sModulePath == null) {
            throw new IllegalStateException("Module path is null");
        }
        return sModulePath;
    }

    /**
     * Get the {@link IXposedHookZygoteInit.StartupParam} of the current module.
     * <p>
     * Do NOT add @NonNull annotation to this method. *** No kotlin code should be invoked here.*** May cause a crash.
     *
     * @return the initZygote param
     */
    public static IXposedHookZygoteInit.StartupParam getInitZygoteStartupParam() {
        if (sInitZygoteStartupParam == null) {
            throw new IllegalStateException("InitZygoteStartupParam is null");
        }
        return sInitZygoteStartupParam;
    }
}
