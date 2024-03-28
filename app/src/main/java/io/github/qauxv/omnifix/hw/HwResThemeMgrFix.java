/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2024 QAuxiliary developers
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

package io.github.qauxv.omnifix.hw;

import android.content.Context;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import io.github.qauxv.BuildConfig;
import io.github.qauxv.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class HwResThemeMgrFix {

    private HwResThemeMgrFix() {
    }

    private static boolean sHwThemeManagerHooked = false;
    private static boolean sHwThemeManagerFailed = false;

    public static void initHook(@NonNull Context context) {
        if (sHwThemeManagerHooked || sHwThemeManagerFailed) {
            return;
        }
        String packageName = context.getPackageName();
        // android.hwtheme.HwThemeManager#getDataSkinThemePackages()ArrayList
        Class<?> kHwThemeManager = null;
        try {
            kHwThemeManager = Class.forName("android.hwtheme.HwThemeManager");
        } catch (ClassNotFoundException e) {
            sHwThemeManagerFailed = true;
            // not huawei, skip
            return;
        }
        Method getDataSkinThemePackages;
        try {
            getDataSkinThemePackages = kHwThemeManager.getDeclaredMethod("getDataSkinThemePackages");
        } catch (NoSuchMethodException e) {
            sHwThemeManagerFailed = true;
            logIfDebugVersion(e);
            // maybe older version, skip
            return;
        }
        XposedBridge.hookMethod(getDataSkinThemePackages, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                ArrayList<String> list = (ArrayList<String>) param.getResult();
                if (list != null) {
                    list.remove(packageName);
                }
            }
        });
        sHwThemeManagerHooked = true;
    }

    private static boolean sHwResourcesImplFixFailed = false;
    private static Field sResourcesImplField = null;
    private static Field sHwResourcesImplField = null;
    private static Method getDataThemePackagesMethod = null;

    public static void fix(@NonNull Context context, @NonNull Resources resources) {
        if (sHwThemeManagerFailed) {
            return;
        }
        try {
            Class.forName("android.content.res.AbsResourcesImpl");
            Class.forName("android.content.res.HwResourcesImpl");
        } catch (ClassNotFoundException e) {
            sHwResourcesImplFixFailed = true;
            // not huawei, skip
            return;
        }
        if (sResourcesImplField == null || sHwResourcesImplField == null || getDataThemePackagesMethod == null) {
            try {
                sResourcesImplField = Resources.class.getDeclaredField("mResourcesImpl");
                sResourcesImplField.setAccessible(true);
                Class<?> kResourcesImpl = Class.forName("android.content.res.ResourcesImpl");
                sHwResourcesImplField = kResourcesImpl.getDeclaredField("mHwResourcesImpl");
                sHwResourcesImplField.setAccessible(true);
                Class<?> kAbsResourcesImpl = Class.forName("android.content.res.AbsResourcesImpl");
                getDataThemePackagesMethod = kAbsResourcesImpl.getDeclaredMethod("getDataThemePackages");
                getDataThemePackagesMethod.setAccessible(true);
            } catch (ReflectiveOperationException e) {
                sHwResourcesImplFixFailed = true;
                logIfDebugVersion(e);
            }
        }
        if (sHwResourcesImplFixFailed) {
            return;
        }
        try {
            Object resImpl = sResourcesImplField.get(resources);
            Object hw = sHwResourcesImplField.get(resImpl);
            ArrayList<String> dataThemePackages = (ArrayList<String>) getDataThemePackagesMethod.invoke(hw);
            if (dataThemePackages != null) {
                dataThemePackages.remove(context.getPackageName());
            }
        } catch (ReflectiveOperationException e) {
            sHwResourcesImplFixFailed = true;
            logIfDebugVersion(e);
        }
    }

    private static void logIfDebugVersion(Throwable e) {
        if (BuildConfig.DEBUG) {
            Log.e(e);
        }
    }

}
