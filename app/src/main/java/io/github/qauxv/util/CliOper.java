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
package io.github.qauxv.util;

import android.app.Application;
import cc.ioctl.hook.AppCenterFix;
import cc.ioctl.util.HostInfo;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.AbstractCrashesListener;
import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.crashes.model.ErrorReport;
import io.github.qauxv.BuildConfig;
import io.github.qauxv.bridge.AppRuntimeHelper;
import io.github.qauxv.config.ConfigManager;
import java.util.HashMap;
import java.util.Map;

public class CliOper {

    private static final String appCenterToken = "77c14c01-b4bf-46cf-bf95-dad60d7418a4";
    private static boolean appCenterInit = false;
    private static final String TAG_IS_APP_CENTER_ALLOWED = "is_app_center_allowed";

    public static boolean isAppCenterAllowed() {
        return ConfigManager.getDefaultConfig().getBooleanOrDefault(TAG_IS_APP_CENTER_ALLOWED, true);
    }

    public static void setAppCenterAllowed(boolean allowed) {
        ConfigManager.getDefaultConfig().putBoolean(TAG_IS_APP_CENTER_ALLOWED, allowed);
    }

    public static void __init__(Application app) {
        if (app == null) {
            return;
        }
        if (!isAppCenterAllowed()) {
            return;
        }
        if (appCenterInit) {
            return;
        }

        AppCenterFix.startAppCenter(app, appCenterToken);
        Crashes.setListener(new CrashesFilter());
        appCenterInit = true;
    }

    public static class CrashesFilter extends AbstractCrashesListener {

        @Override
        public boolean shouldProcess(ErrorReport report) {
            return containsQA(
                report.getStackTrace().replace("io.github.qauxv.lifecycle.Parasitics", ""));
        }

        private boolean containsQA(String string) {
            for (String name : qauxvPackageName) {
                if (string.contains(name)) {
                    return true;
                }
            }
            return false;
        }

        private String[] qauxvPackageName = new String[]{
                "cc.ioctl.",
                "cn.lliiooll.",
                "com.rymmmmm.",
                "me.ketal.",
                "me.kyuubiran.",
                "me.singleneuron.",
                "io.github.qauxv.",
                "xyz.nextalone.",
                "io.github.duzhaokun123.",
                "com.hicore.",
                "sakura.kooi.QAuxiliaryModified",
        };
    }

    public static void onLoad() {
        if (!isAppCenterAllowed()) {
            return;
        }
        CliOper.__init__(HostInfo.getApplication());
        final String LAST_TRACE_HASHCODE_CONFIG = "lastTraceHashcode";
        ConfigManager configManager = ConfigManager.getDefaultConfig();
        Integer oldHashCode = null;
        try {
            oldHashCode = (Integer) configManager.getObject(LAST_TRACE_HASHCODE_CONFIG);
        } catch (Exception e) {
            Log.e(e);
        }
        HashMap<String, String> properties = new HashMap<>();
        properties.put("versionName", BuildConfig.VERSION_NAME);
        properties.put("versionCode", String.valueOf(BuildConfig.VERSION_CODE));
        long longAccount = AppRuntimeHelper.getLongAccountUin();
        if (longAccount != -1) {
            properties.put("Uin", String.valueOf(longAccount));
        }
        int newHashCode = properties.hashCode();
        if (oldHashCode != null && oldHashCode.equals(newHashCode)) {
            return;
        }
        try {
            configManager.putInt(LAST_TRACE_HASHCODE_CONFIG, newHashCode);
            configManager.save();
        } catch (Exception e) {
            //ignored
        }
        __init__(HostInfo.getApplication());
        Analytics.trackEvent("onLoad", properties);
        Log.d("start App Center Trace OnLoad:" + properties);
    }

    public static void enterModuleActivity(String shortName) {
        if (!isAppCenterAllowed()) {
            return;
        }
        try {
            __init__(HostInfo.getApplication());
            Map<String, String> prop = new HashMap<>();
            prop.put("name", shortName);
            Analytics.trackEvent("enterModuleActivity", prop);
            Log.d("Start App Center Trace enterModuleActivity: " + prop);
        } catch (Throwable ignored) {
        }
    }
}
