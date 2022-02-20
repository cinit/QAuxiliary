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

import android.app.Application;
import android.os.Handler;
import androidx.annotation.NonNull;
import cc.ioctl.util.HostInfo;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.channel.AbstractChannelListener;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.ingestion.models.WrapperSdk;
import io.github.qauxv.BuildConfig;
import io.github.qauxv.util.Log;

public class AppCenterFix {

    // XREF: https://github.com/LSPosed/LSPosed/blob/11be039203f3b66d71baaad7cb91d8e9dc63958b/app/src/debug/java/org/lsposed/manager/util/Telemetry.java

    private AppCenterFix() {
    }

    private static boolean sIsInitialized = false;

    /**
     * HostInfo must be initialized before AppCenterFix
     */
    public static void startAppCenter(@NonNull Application app, @NonNull String appSecret) {
        if (sIsInitialized) {
            return;
        }
        AppCenter.start(app, appSecret, Analytics.class, Crashes.class);
        patchDevice();
        // set wrapper app info
        WrapperSdk hostSdk = new WrapperSdk();
        hostSdk.setWrapperSdkName(HostInfo.getPackageName());
        hostSdk.setWrapperSdkVersion(HostInfo.getVersionName());
        hostSdk.setWrapperRuntimeVersion(String.valueOf(HostInfo.getVersionCode32()));
        AppCenter.setWrapperSdk(hostSdk);
        sIsInitialized = true;
    }

    private static final Channel.Listener patchDeviceListener = new AbstractChannelListener() {
        @Override
        public void onPreparedLog(@NonNull com.microsoft.appcenter.ingestion.models.Log log, @NonNull String groupName, int flags) {
            var device = log.getDevice();
            // set module info rather than host app info
            device.setAppVersion(BuildConfig.VERSION_NAME);
            device.setAppBuild(String.valueOf(BuildConfig.VERSION_CODE));
            device.setAppNamespace(BuildConfig.APPLICATION_ID);
        }
    };

    private static void addPatchDeviceListener() {
        try {
            var channelField = AppCenter.class.getDeclaredField("mChannel");
            channelField.setAccessible(true);
            var channel = (Channel) channelField.get(AppCenter.getInstance());
            assert channel != null;
            channel.addListener(patchDeviceListener);
        } catch (ReflectiveOperationException e) {
            Log.e("add listener", e);
        }
    }

    private static void patchDevice() {
        try {
            var handlerField = AppCenter.class.getDeclaredField("mHandler");
            handlerField.setAccessible(true);
            var handler = ((Handler) handlerField.get(AppCenter.getInstance()));
            assert handler != null;
            handler.post(AppCenterFix::addPatchDeviceListener);
        } catch (ReflectiveOperationException e) {
            Log.e("patch device", e);
        }
    }
}
