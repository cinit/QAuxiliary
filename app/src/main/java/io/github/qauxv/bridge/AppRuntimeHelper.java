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

package io.github.qauxv.bridge;

import androidx.annotation.Nullable;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.Reflex;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.MainProcess;
import java.lang.reflect.Field;
import mqq.app.AppRuntime;

public class AppRuntimeHelper {

    private static boolean sAppRuntimeInit = false;
    private static Field f_mAppRuntime = null;

    private AppRuntimeHelper() {
    }

    public static long getLongAccountUin() {
        try {
            AppRuntime rt = getAppRuntime();
            if (rt == null) {
                // getLongAccountUin/E getAppRuntime == null
                return -1;
            }
            return (long) Reflex.invokeVirtual(rt, "getLongAccountUin");
        } catch (Exception e) {
            Log.e(e);
        }
        return -1;
    }

    @MainProcess
    public static AppRuntime getQQAppInterface() {
        AppRuntime art = getAppRuntime();
        if (art == null) {
            return null;
        }
        if (Initiator._QQAppInterface().isAssignableFrom(art.getClass())) {
            return art;
        } else {
            throw new IllegalStateException("QQAppInterface is not available in current process");
        }
    }

    public static void $access$set$sAppRuntimeInit(boolean z) {
        sAppRuntimeInit = z;
    }

    @Nullable
    @MainProcess
    public static AppRuntime getAppRuntime() {
        if (!sAppRuntimeInit) {
            // getAppRuntime/W invoked before NewRuntime.step
            return null;
        }
        Object baseApplicationImpl = HostInfo.getApplication();
        try {
            if (f_mAppRuntime == null) {
                f_mAppRuntime = Class.forName("mqq.app.MobileQQ").getDeclaredField("mAppRuntime");
                f_mAppRuntime.setAccessible(true);
            }
            return (AppRuntime) f_mAppRuntime.get(baseApplicationImpl);
        } catch (Exception e) {
            Log.e(e);
            return null;
        }
    }

    public static String getAccount() {
        Object rt = getAppRuntime();
        try {
            return (String) Reflex.invokeVirtual(rt, "getAccount");
        } catch (Exception e) {
            Log.e(e);
            return null;
        }
    }
}
