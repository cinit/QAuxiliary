/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
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
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.AbstractCrashesListener;
import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.crashes.model.ErrorReport;
import io.github.qauxv.BuildConfig;
import io.github.qauxv.bridge.AppRuntimeHelper;
import io.github.qauxv.config.ConfigManager;
import java.util.HashMap;
import java.util.Map;
import me.singleneuron.qn_kernel.data.HostInfo;

public class CliOper {

    private static final String appCenterToken =
        BuildConfig.DEBUG ? "530d3819-3543-46e3-8c59-5576604f3801"
            : "ddf4b597-1833-45dd-af28-96ca504b8123";
    private static boolean appCenterInit = false;

    public static void __init__(Application app) {
        if (app == null) {
            return;
        }
        if (appCenterInit) {
            return;
        }

        long longAccount = AppRuntimeHelper.getLongAccountUin();
        if (longAccount != -1) {
            AppCenter.setUserId(String.valueOf(longAccount));
        }
        Crashes.setListener(new CrashesFilter());
        AppCenter.start(app, appCenterToken, Analytics.class, Crashes.class);
        appCenterInit = true;

    }

    public static class CrashesFilter extends AbstractCrashesListener {

        @Override
        public boolean shouldProcess(ErrorReport report) {
            return containsQN(
                report.getStackTrace().replace("io.github.qauxv.lifecycle.Parasitics", ""));
        }

        private boolean containsQN(String string) {
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
            "me.zpp0196.",
            "io.github.qauxv.",
            "xyz.nextalone."
        };

    }

    public static void onLoad() {
        CliOper.__init__(HostInfo.getHostInfo().getApplication());
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
        Integer newHashCode = properties.hashCode();
        if (oldHashCode != null && oldHashCode.equals(newHashCode)) {
            return;
        }
        try {
            configManager.putObject(LAST_TRACE_HASHCODE_CONFIG, newHashCode);
            configManager.save();
        } catch (Exception e) {
            //ignored
        }
        __init__(HostInfo.getHostInfo().getApplication());
        Analytics.trackEvent("onLoad", properties);
        Log.d("start App Center Trace OnLoad:" + properties.toString());
    }

    public static void copyCardMsg(String msg) {
        if (msg == null) {
            return;
        }
        __init__(HostInfo.getHostInfo().getApplication());
        try {
            Analytics.trackEvent("copyCardMsg", digestCardMsg(msg));
        } catch (Throwable e) {
            Log.e(e);
        }
    }

    public static void sendCardMsg(long uin, String msg) {
        if (msg == null) {
            return;
        }
        __init__(HostInfo.getHostInfo().getApplication());
        try {
            Map<String, String> prop = digestCardMsg(msg);
            prop.put("uin", String.valueOf(uin));
            Analytics.trackEvent("sendCardMsg", prop);
        } catch (Throwable e) {
            Log.e(e);
        }
    }

    public static void batchSendMsg(long uin, String msg, int count) {
        if (msg == null) {
            return;
        }
        Map<String, String> properties = new HashMap<>();
        if (msg.length() > 127) {
            msg = msg.substring(0, 127);
        }
        properties.put("msg", msg);
        properties.put("uin", String.valueOf(uin));
        properties.put("count", String.valueOf(count));
        __init__(HostInfo.getHostInfo().getApplication());
        Analytics.trackEvent("batchSendMsg", properties);
    }

    private static Map<String, String> digestCardMsg(String msg) {
        Map<String, String> prop = new HashMap<>();
        if (msg.startsWith("<")) {
            //xml
            prop.put("type", "xml");
            prop.put("serviceID", findXmlValueOrEmpty(msg, "serviceID"));
            prop.put("templateID", findXmlValueOrEmpty(msg, "templateID"));
            prop.put("action", findXmlValueOrEmpty(msg, "action"));
            prop.put("brief", findXmlValueOrEmpty(msg, "brief"));
            prop.put("name", findXmlValueOrEmpty(msg, "name"));
        } else if (msg.startsWith("{")) {
            //json
            prop.put("type", "json");
            prop.put("app", findJsonValueOrEmpty(msg, "app"));
            prop.put("desc", findJsonValueOrEmpty(msg, "desc"));
            prop.put("prompt", findJsonValueOrEmpty(msg, "prompt"));
            prop.put("appID", findJsonValueOrEmpty(msg, "appID"));
            prop.put("text", findJsonValueOrEmpty(msg, "text"));
            prop.put("actionData", findJsonValueOrEmpty(msg, "actionData"));
        } else {
            if (msg.length() > 127) {
                msg = msg.substring(0, 127);
            }
            prop.put("type", "unknown");
            prop.put("raw", msg);
        }
        return prop;
    }

    private static String findJsonValueOrEmpty(String raw, String key) {
        if (key == null || raw == null) {
            return "";
        }
        key = '"' + key + '"';
        raw = raw.replace(" ", "");
        if (!raw.contains(key)) {
            return "";
        }
        int limit = raw.indexOf(key);
        int start = raw.indexOf(':', limit);
        int e1 = raw.indexOf(',', start);
        int e2 = raw.indexOf('}', start);
        int end;
        if (e1 * e2 == 1) {
            return "";
        }
        if (e1 * e2 < 0) {
            if (e1 == -1) {
                end = e2;
            } else {
                end = e1;
            }
        } else {
            end = Math.min(e1, e2);
        }
        String subseq = raw.substring(start + 1, end);
        if (subseq.startsWith("\"")) {
            int e3 = raw.indexOf('"', start);
            int stop = indexMax(end, e3);
            if ((raw.charAt(stop) == ',' || raw.charAt(stop) == '}')
                && raw.charAt(stop - 1) == '"') {
                return raw.substring(start + 2, stop - 1);//exclude '"'
            } else {
                return raw.substring(start + 1, stop);
            }
        } else {
            return subseq;
        }
    }

    private static String findXmlValueOrEmpty(String raw, String key) {
        if (key == null || raw == null) {
            return "";
        }
        raw = raw.replace('\'', '"').replace(" ", "");
        if (!raw.contains(key)) {
            return "";
        }
        int limit = raw.indexOf(key);
        int start = raw.indexOf('"', limit);
        int end = raw.indexOf('"', start + 1);
        if (start == -1 || end == -1) {
            return "";
        }
        return raw.substring(start + 1, end);
    }

    public static int indexMax(int a, int b) {
        if (a < 0) {
            return b;
        }
        if (b < 0) {
            return a;
        }
        return Math.max(a, b);
    }

    public static int indexMin(int a, int b) {
        if (a < 0) {
            return b;
        }
        if (b < 0) {
            return a;
        }
        return Math.min(a, b);
    }

    public static void enterModuleActivity(String shortName) {
        try {
            __init__(HostInfo.getHostInfo().getApplication());
            Map<String, String> prop = new HashMap<>();
            prop.put("name", shortName);
            Analytics.trackEvent("enterModuleActivity", prop);
            Log.d("Start App Center Trace enterModuleActivity: " + prop.toString());
        } catch (Throwable ignored) {
        }
    }
}
