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

package io.github.qauxv.util.hookstatus;

import android.app.Application;
import android.os.Build;
import androidx.annotation.NonNull;
import io.github.qauxv.BuildConfig;
import io.github.qauxv.core.NativeCoreBridge;
import io.github.qauxv.loader.hookapi.ILoaderInfo;
import io.github.qauxv.poststartup.StartupInfo;
import io.github.qauxv.util.HostInfo;
import io.github.qauxv.util.Natives;
import org.lsposed.hiddenapibypass.HiddenApiBypass;

public class ModuleAppImpl extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // init host info, even if we are not in the host app
        HostInfo.init(this);
        // load native library
        Natives.load(this);
        // bypass hidden api check for current process
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.setHiddenApiExemptions("L");
        }
        NativeCoreBridge.initNativeCore(getPackageName(), Build.VERSION.SDK_INT,
                HostInfo.getHostInfo().getVersionName(), HostInfo.getHostInfo().getVersionCode());
        initStartupInfo();
        // for fail-safe purpose
        com.github.kyuubiran.ezxhelper.utils.Log.INSTANCE.getCurrentLogger().setLogTag("QAuxv");
    }

    private void initStartupInfo() {
        final String apkPath = getApplicationInfo().sourceDir;
        ILoaderInfo loaderInfo = new ILoaderInfo() {
            @NonNull
            @Override
            public String getEntryPointName() {
                return "ActivityThread";
            }

            @NonNull
            @Override
            public String getLoaderVersionName() {
                return BuildConfig.VERSION_NAME;
            }

            @Override
            public int getLoaderVersionCode() {
                return BuildConfig.VERSION_CODE;
            }

            @NonNull
            @Override
            public String getMainModulePath() {
                return apkPath;
            }

            @Override
            public void log(@NonNull String msg) {
                android.util.Log.i("QAuxv", msg);
            }

            @Override
            public void log(@NonNull Throwable tr) {
                android.util.Log.e("QAuxv", tr.toString(), tr);
            }
        };
        StartupInfo.setModulePath(apkPath);
        StartupInfo.setLoaderInfo(loaderInfo);
    }

}
