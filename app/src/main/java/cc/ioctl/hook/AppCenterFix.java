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

package cc.ioctl.hook;

import android.content.Context;
import cc.ioctl.util.HostInfo;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.ingestion.models.Device;
import com.microsoft.appcenter.ingestion.models.WrapperSdk;
import com.microsoft.appcenter.utils.DeviceInfoHelper;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import io.github.qauxv.BuildConfig;
import io.github.qauxv.util.Log;
import java.lang.reflect.Method;

public class AppCenterFix {

    private AppCenterFix() {
    }

    private static boolean sIsInitialized = false;

    /**
     * HostInfo must be initialized before AppCenterFix
     */
    public static void init() {
        if (sIsInitialized) {
            return;
        }
        try {
            Method getDeviceInfo = DeviceInfoHelper.class.getMethod("getDeviceInfo", Context.class);
            XposedBridge.hookMethod(getDeviceInfo, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Device device = (Device) param.getResult();
                    if (device != null) {
                        // set module info rather than host app info
                        device.setAppVersion(BuildConfig.VERSION_NAME);
                        device.setAppBuild(String.valueOf(BuildConfig.VERSION_CODE));
                        device.setAppNamespace(BuildConfig.APPLICATION_ID);
                    }
                }
            });
            // set wrapper app info
            WrapperSdk hostSdk = new WrapperSdk();
            hostSdk.setWrapperSdkName(HostInfo.getPackageName());
            hostSdk.setWrapperSdkVersion(HostInfo.getVersionName());
            hostSdk.setWrapperRuntimeVersion(String.valueOf(HostInfo.getVersionCode32()));
            AppCenter.setWrapperSdk(hostSdk);
            sIsInitialized = true;
        } catch (Exception e) {
            Log.e(e);
        }
    }
}
