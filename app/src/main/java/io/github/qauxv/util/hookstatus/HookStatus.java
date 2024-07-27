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

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HostInfo;
import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;
import io.github.qauxv.BuildConfig;
import io.github.qauxv.R;
import io.github.qauxv.util.LoaderExtensionHelper;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.util.NonUiThread;
import java.io.File;
import java.util.HashMap;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlowKt;

/**
 * This class is only intended to be used in module process, not in host process.
 */
public class HookStatus {

    private HookStatus() {
    }

    private static boolean sExpCpCalled = false;
    private static boolean sExpCpResult = false;
    private static final MutableStateFlow<XposedService> sXposedService = StateFlowKt.MutableStateFlow(null);
    private static boolean sXposedServiceListenerRegistered = false;
    private static final XposedServiceHelper.OnServiceListener sXposedServiceListener = new XposedServiceHelper.OnServiceListener() {

        @Override
        public void onServiceBind(@NonNull XposedService service) {
            sXposedService.setValue(service);
        }

        @Override
        public void onServiceDied(@NonNull XposedService service) {
            sXposedService.setValue(null);
        }

    };

    public enum HookType {
        /**
         * No hook.
         */
        NONE,
        /**
         * Taichi, BugHook(not implemented), etc.
         */
        APP_PATCH,
        /**
         * Legacy Xposed, EdXposed, LSPosed, Dreamland, etc.
         */
        ZYGOTE,
    }

    @Nullable
    public static String getZygoteHookProvider() {
        return HookStatusImpl.sZygoteHookProvider;
    }

    public static boolean isLsposedDexObfsEnabled() {
        return HookStatusImpl.sIsLsposedDexObfsEnabled;
    }

    public static boolean isZygoteHookMode() {
        return HookStatusImpl.sZygoteHookMode;
    }

    public static boolean isLegacyXposed() {
        try {
            ClassLoader.getSystemClassLoader().loadClass("de.robv.android.xposed.XposedBridge");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isElderDriverXposed() {
        return new File("/system/framework/edxp.jar").exists();
    }

    @NonUiThread
    public static boolean callTaichiContentProvider(@NonNull Context context) {
        try {
            ContentResolver contentResolver = context.getContentResolver();
            Uri uri = Uri.parse("content://me.weishu.exposed.CP/");
            Bundle result = new Bundle();
            try {
                result = contentResolver.call(uri, "active", null, null);
            } catch (RuntimeException e) {
                // TaiChi is killed, try invoke
                try {
                    Intent intent = new Intent("me.weishu.exp.ACTION_ACTIVE");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } catch (ActivityNotFoundException anfe) {
                    return false;
                }
            }
            if (result == null) {
                result = contentResolver.call(uri, "active", null, null);
            }
            if (result == null) {
                return false;
            }
            return result.getBoolean("active", false);
        } catch (Exception e) {
            return false;
        }
    }

    public static void init(@NonNull Context context) {
        if (context.getPackageName().equals(BuildConfig.APPLICATION_ID)) {
            if (!sXposedServiceListenerRegistered) {
                XposedServiceHelper.registerListener(sXposedServiceListener);
                sXposedServiceListenerRegistered = true;
            }
            SyncUtils.async(() -> {
                sExpCpCalled = callTaichiContentProvider(context);
                sExpCpResult = sExpCpCalled;
            });
        } else {
            // in host process???
            try {
                initHookStatusImplInHostProcess();
            } catch (LinkageError ignored) {
            }
        }
    }

    @NonNull
    public static MutableStateFlow<XposedService> getXposedService() {
        return sXposedService;
    }

    public static HookType getHookType() {
        if (isZygoteHookMode()) {
            return HookType.ZYGOTE;
        }
        return sExpCpResult ? HookType.APP_PATCH : HookType.NONE;
    }

    private static void initHookStatusImplInHostProcess() throws LinkageError {
        Class<?> xposedClass = LoaderExtensionHelper.getXposedBridgeClass();
        boolean dexObfsEnabled = false;
        if (xposedClass != null) {
            dexObfsEnabled = !"de.robv.android.xposed.XposedBridge".equals(xposedClass.getName());
        }
        String hookProvider = null;
        if (dexObfsEnabled) {
            HookStatusImpl.sIsLsposedDexObfsEnabled = true;
            hookProvider = "LSPosed";
        } else {
            String bridgeTag = null;
            if (xposedClass != null) {
                try {
                    bridgeTag = (String) xposedClass.getDeclaredField("TAG").get(null);
                } catch (ReflectiveOperationException ignored) {
                }
            }
            if (bridgeTag != null) {
                if (bridgeTag.startsWith("LSPosed")) {
                    hookProvider = "LSPosed";
                } else if (bridgeTag.startsWith("EdXposed")) {
                    hookProvider = "EdXposed";
                } else if (bridgeTag.startsWith("PineXposed")) {
                    hookProvider = "Dreamland";
                }
            }
        }
        if (hookProvider != null) {
            HookStatusImpl.sZygoteHookProvider = hookProvider;
        }
    }

    public static String getHookProviderNameForLegacyApi() {
        if (isZygoteHookMode()) {
            String name = getZygoteHookProvider();
            if (name != null) {
                return name;
            }
            if (isLegacyXposed()) {
                return "Legacy Xposed";
            }
            if (isElderDriverXposed()) {
                return "EdXposed";
            }
            return "Unknown(Zygote)";
        }
        if (sExpCpResult) {
            return "Taichi";
        }
        return "None";
    }

    public static boolean isTaiChiInstalled(@NonNull Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo("me.weishu.exp", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isModuleEnabled() {
        return getHookType() != HookType.NONE;
    }

    public static HashMap<String, String> getHostABI() {
        CharSequence[] scope = HostInfo.getApplication().getResources().getTextArray(R.array.xposedscope);
        HashMap<String, String> result = new HashMap<>(4);
        for (CharSequence s : scope) {
            String abi = AbiUtils.getApplicationActiveAbi(s.toString());
            if (abi != null) {
                result.put(s.toString(), abi);
            }
        }
        return result;
    }
}
