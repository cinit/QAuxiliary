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

package io.github.qauxv.core;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.hook.SettingEntryHook;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.BuildConfig;
import io.github.qauxv.base.IDynamicHook;
import io.github.qauxv.base.RuntimeErrorTracer;
import io.github.qauxv.step.Step;
import io.github.qauxv.ui.CustomDialog;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.util.Toasts;
import io.github.qauxv.util.dexkit.DexDeobfsBackend;
import io.github.qauxv.util.dexkit.DexDeobfsProvider;
import io.github.qauxv.util.libart.OatInlineDeoptManager;
import io.github.qauxv.util.xpcompat.XposedBridge;

public class HookInstaller {

    private HookInstaller() {
    }

    private static IDynamicHook[] sAnnotatedHooks = null;

    private static volatile Throwable sFuncInitException = null;

    @NonNull
    public static IDynamicHook[] queryAllAnnotatedHooks() {
        if (sAnnotatedHooks != null) {
            return sAnnotatedHooks;
        }
        synchronized (HookInstaller.class) {
            if (sAnnotatedHooks == null) {
                // 处理 AnnotatedFunctionHookEntryList.getAnnotatedFunctionHookEntryList
                // 可能会抛出异常的情况:
                // java.lang.ExceptionInInitializerError
                // 通常不会出现, 但是一旦出现会导致极其严重的问题, 整个模块不可用
                try {
                    sAnnotatedHooks = io.github.qauxv.gen.AnnotatedFunctionHookEntryList.getAnnotatedFunctionHookEntryList();
                } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
                    sFuncInitException = e;
                    // leave a setting entry for user to report this issue
                    sAnnotatedHooks = new IDynamicHook[]{
                            SettingEntryHook.INSTANCE
                    };
                    Log.e(e.toString(), e);
                    XposedBridge.log(e);
                }
            }
        }
        return sAnnotatedHooks;
    }

    @Nullable
    public static Throwable getFuncInitException() {
        return sFuncInitException;
    }

    public static int getHookIndex(@NonNull IDynamicHook hook) {
        IDynamicHook[] hooks = queryAllAnnotatedHooks();
        for (int i = 0; i < hooks.length; i++) {
            if (hooks[i] == hook) {
                return i;
            }
        }
        return -1;
    }

    public static void allowEarlyInit(@NonNull IDynamicHook hook) {
        try {
            if (hook.isTargetProcess() && hook.isEnabled() && !hook.isPreparationRequired() && !hook.isInitialized()) {
                hook.initialize();
            }
        } catch (Throwable e) {
            if (hook instanceof RuntimeErrorTracer) {
                ((RuntimeErrorTracer) hook).traceError(e);
            } else {
                Log.e(e);
            }
        }
    }

    public static IDynamicHook getHookById(int index) {
        return queryAllAnnotatedHooks()[index];
    }

    public static void initializeHookForeground(@NonNull Context context, @NonNull IDynamicHook hook) {
        SyncUtils.async(() -> doInitAndSetupHookForeground(context, hook));
    }

    public static void doInitAndSetupHookForeground(@NonNull Context context, @NonNull IDynamicHook hook) {
        final CustomDialog[] pDialog = new CustomDialog[1];
        Throwable err = null;
        boolean isSuccessful = true;
        DexDeobfsProvider.INSTANCE.enterDeobfsSection();
        try (DexDeobfsBackend backend = DexDeobfsProvider.INSTANCE.getCurrentBackend()) {
            if (hook.isPreparationRequired()) {
                Step[] steps = hook.makePreparationSteps();
                if (steps != null) {
                    for (Step s : steps) {
                        if (s.isDone()) {
                            continue;
                        }
                        // TODO: 2022-08-26 add batch init
                        final String name = s.getDescription();
                        SyncUtils.runOnUiThread(() -> {
                            if (pDialog[0] == null) {
                                pDialog[0] = CustomDialog.createFailsafe(context);
                                pDialog[0].setCancelable(false);
                                pDialog[0].setTitle("请稍候");
                                pDialog[0].setMessage("正在初始化...");
                                pDialog[0].show();
                            }
                            pDialog[0].setMessage("QAuxiliary " + BuildConfig.VERSION_NAME + " 正在初始化:\n" + name + "\n每个类一般不会超过一分钟");
                        });
                        s.step();
                    }
                }
            }
            if (hook.isTargetProcess()) {
                boolean success = false;
                try {
                    success = hook.initialize();
                } catch (Throwable ex) {
                    err = ex;
                }
                if (!success) {
                    SyncUtils.runOnUiThread(() -> Toasts.error(context, "初始化失败"));
                } else {
                    OatInlineDeoptManager.getInstance().updateDeoptListForCurrentProcess();
                    OatInlineDeoptManager.performOatDeoptimizationForCache();
                }
            }
            SyncUtils.requestInitHook(HookInstaller.getHookIndex(hook), hook.getTargetProcesses());
        } catch (Throwable stepErr) {
            if (hook instanceof RuntimeErrorTracer) {
                ((RuntimeErrorTracer) hook).traceError(stepErr);
            }
            err = stepErr;
        } finally {
            DexDeobfsProvider.INSTANCE.exitDeobfsSection();
        }
        if (err != null) {
            Throwable finalErr = err;
            SyncUtils.runOnUiThread(() -> CustomDialog.createFailsafe(context).setTitle("发生错误")
                    .setMessage(finalErr.toString())
                    .setCancelable(true).setPositiveButton(android.R.string.ok, null).show());
        }
        if (pDialog[0] != null) {
            SyncUtils.runOnUiThread(() -> pDialog[0].dismiss());
        }
    }

    public static void restartToTakeEffect(@Nullable Context context) {
        Toasts.info(context, "重启 " + HostInfo.getAppName() + " 生效");
    }

    public static Step[] stepsOf(@Nullable Step[] a, @Nullable Step[] b) {
        if (a == null) {
            return b;
        } else if (b == null) {
            return a;
        } else {
            Step[] result = new Step[a.length + b.length];
            System.arraycopy(a, 0, result, 0, a.length);
            System.arraycopy(b, 0, result, a.length, b.length);
            return result;
        }
    }
}
