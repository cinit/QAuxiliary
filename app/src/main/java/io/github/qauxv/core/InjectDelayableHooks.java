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

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import cc.ioctl.hook.SettingEntryHook;
import cc.ioctl.util.LayoutHelper;
import cc.ioctl.util.Reflex;
import cc.ioctl.util.ui.drawable.ProportionDrawable;
import cc.ioctl.util.ui.drawable.SimpleBgDrawable;
import io.github.qauxv.BuildConfig;
import io.github.qauxv.base.IDynamicHook;
import io.github.qauxv.base.RuntimeErrorTracer;
import io.github.qauxv.step.DexDeobfStep;
import io.github.qauxv.step.ShadowBatchDexDeobfStep;
import io.github.qauxv.step.Step;
import io.github.qauxv.util.LicenseStatus;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.util.dexkit.DexDeobfsBackend;
import io.github.qauxv.util.dexkit.DexDeobfsProvider;
import io.github.qauxv.util.dexkit.DexKitTarget;
import io.github.qauxv.util.dexkit.DexKitTargetSealedEnum;
import io.github.qauxv.util.libart.OatInlineDeoptManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import mqq.app.AppActivity;

public class InjectDelayableHooks {

    private static boolean inited = false;

    public static boolean step(@Nullable Object director) {
        if (inited) {
            return true;
        }
        inited = true;
        // TODO: 2023-04-19 check whether NT QQ has an Activity for foreground startup
        final Activity activity;
        if (director != null) {
            Activity act = Reflex.getInstanceObjectOrNull(director, "a", AppActivity.class);
            if (act == null) {
                act = Reflex.getFirstNSFByType(director, AppActivity.class);
            }
            act = Reflex.getInstanceObjectOrNull(director, "a", AppActivity.class);
            activity = act;
        } else {
            activity = null;
        }
        boolean needDeobf = false;
        IDynamicHook[] hooks = HookInstaller.queryAllAnnotatedHooks();
        for (IDynamicHook h : hooks) {
            try {
                if (h.isEnabled() && h.isPreparationRequired()) {
                    needDeobf = true;
                    break;
                }
            } catch (Exception | LinkageError | AssertionError e) {
                if (h instanceof RuntimeErrorTracer) {
                    ((RuntimeErrorTracer) h).traceError(e);
                } else {
                    Log.e("Hook " + h.getClass().getName() + " failed to check if preparation is required", e);
                }
            }
        }
        final LinearLayout[] overlay = new LinearLayout[1];
        final LinearLayout[] main = new LinearLayout[1];
        final ProportionDrawable[] prog = new ProportionDrawable[1];
        final TextView[] text = new TextView[1];
        DexDeobfsProvider.INSTANCE.enterDeobfsSection();
        try (DexDeobfsBackend backend = DexDeobfsProvider.INSTANCE.getCurrentBackend()) {
            if (needDeobf) {
                final HashSet<Step> todos = new HashSet<>();
                for (IDynamicHook h : hooks) {
                    try {
                        if (!h.isEnabled()) {
                            continue;
                        }
                        if (h.isPreparationRequired()) {
                            Step[] steps = h.makePreparationSteps();
                            if (steps != null) {
                                for (Step i : steps) {
                                    if (!i.isDone()) {
                                        todos.add(i);
                                    }
                                }
                            }
                        }
                    } catch (Exception | LinkageError | AssertionError e) {
                        if (h instanceof RuntimeErrorTracer) {
                            ((RuntimeErrorTracer) h).traceError(e);
                        } else {
                            Log.e("Hook " + h.getClass().getName() + " failed to make preparation steps", e);
                        }
                    }
                }
                final ArrayList<Step> steps = new ArrayList<>(todos);
                // collect all dex-deobfs steps if backend supports
                HashSet<String> deobfIndexList = new HashSet<>(16);
                for (Step step : steps) {
                    if (step.getClass() == DexDeobfStep.class && !step.isDone()) {
                        String id = ((DexDeobfStep) step).getId();
                        deobfIndexList.add(id);
                    }
                }
                if (backend.isBatchFindMethodSupported()) {
                    List<DexKitTarget> ids = new LinkedList<>();
                    for (String id : deobfIndexList) {
                        DexKitTarget target = DexKitTargetSealedEnum.INSTANCE.valueOf(id);
                        if (target instanceof DexKitTarget) {
                            ids.add(target);
                        }
                    }
                    ShadowBatchDexDeobfStep shadowBatchStep = new ShadowBatchDexDeobfStep(backend, ids.toArray(new DexKitTarget[0]));
                    steps.add(shadowBatchStep);
                }
                steps.sort(Collections.reverseOrder());
                for (int idx = 0; idx < steps.size(); idx++) {
                    final int j = idx;
                    if (SyncUtils.isMainProcess() && activity != null) {
                        activity.runOnUiThread(() -> {
                            if (overlay[0] == null) {
                                overlay[0] = new LinearLayout(activity);
                                overlay[0].setOrientation(LinearLayout.VERTICAL);
                                overlay[0].setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
                                overlay[0].setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
                                main[0] = new LinearLayout(activity);
                                overlay[0].addView(main[0]);
                                main[0].setOrientation(LinearLayout.VERTICAL);
                                main[0].setGravity(Gravity.CENTER);
                                LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                                llp.bottomMargin = LayoutHelper.dip2px(activity, 55);
                                main[0].setLayoutParams(llp);
                                LinearLayout lprop = new LinearLayout(activity);
                                ViewCompat.setBackground(lprop, new SimpleBgDrawable(0, 0xA0808080, 2));
                                final View _v = new View(activity);
                                prog[0] = new ProportionDrawable(0xA0202020, 0x40FFFFFF,
                                        Gravity.LEFT, 0);
                                ViewCompat.setBackground(_v, prog[0]);
                                int __3_ = LayoutHelper.dip2px(activity, 3);
                                LinearLayout.LayoutParams _tmp_lllp = new LinearLayout.LayoutParams(
                                        MATCH_PARENT, LayoutHelper.dip2px(activity, 4));
                                _tmp_lllp.setMargins(__3_, __3_, __3_, __3_);
                                lprop.addView(_v, _tmp_lllp);
                                LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                                int __5_ = LayoutHelper.dip2px(activity, 5);
                                plp.setMargins(__5_ * 2, 0, __5_ * 2, __5_);
                                main[0].addView(lprop, plp);
                                text[0] = new TextView(activity);
                                text[0].setTextSize(16);
                                text[0].setGravity(Gravity.CENTER_HORIZONTAL);
                                text[0].setTextColor(0xFF000000);
                                text[0].setShadowLayer(__5_ * 2, -0, -0, 0xFFFFFFFF);
                                LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                                main[0].addView(text[0], tlp);
                                ((ViewGroup) activity.getWindow().getDecorView()).addView(overlay[0]);
                            }
                            String statusText;
                            try {
                                statusText = "QAuxiliary " + BuildConfig.VERSION_NAME + " 正在初始化:\n"
                                        + steps.get(j).getDescription() + "\n每个类一般不会超过一分钟";
                            } catch (Throwable e22) {
                                statusText = e22.toString();
                            }
                            text[0].setText(statusText);
                            prog[0].setProportion(1.0f * j / steps.size());
                        });
                    }
                    try {
                        Step step = steps.get(idx);
                        if (!step.isDone()) {
                            step.step();
                        }
                    } catch (Throwable e) {
                        Log.e(e);
                    }
                }
            }
            if (LicenseStatus.hasUserAcceptEula()) {
                for (IDynamicHook h : hooks) {
                    try {
                        if (h.isEnabled() && h.isTargetProcess()) {
                            if (!h.isPreparationRequired()) {
                                h.initialize();
                            } else {
                                Log.e("InjectDelayableHooks/E not init " + h + ", checkPreconditions == false");
                            }
                        }
                    } catch (Throwable e) {
                        Log.e(e);
                    }
                }
                if (OatInlineDeoptManager.getInstance().isDeoptListCacheOutdated()) {
                    OatInlineDeoptManager.getInstance().updateDeoptListForCurrentProcess();
                }
                OatInlineDeoptManager.performOatDeoptimizationForCache();
            } else {
                SettingEntryHook.INSTANCE.initialize();
            }
        } finally {
            DexDeobfsProvider.INSTANCE.exitDeobfsSection();
        }
        if (activity != null && main[0] != null) {
            System.gc();
            activity.runOnUiThread(() -> ((ViewGroup) activity.getWindow().getDecorView()).removeView(overlay[0]));
        }
        return true;
    }

    public static void stepForMainBackgroundStartup() {
        if (LicenseStatus.hasUserAcceptEula()) {
            IDynamicHook[] hooks = HookInstaller.queryAllAnnotatedHooks();
            for (IDynamicHook h : hooks) {
                try {
                    if (h.isEnabled() && h.isTargetProcess()) {
                        if (!h.isPreparationRequired()) {
                            h.initialize();
                        } else {
                            Log.e("InjectDelayableHooks/stepForMainBackgroundStartup not init " + h + ", checkPreconditions == false");
                        }
                    }
                } catch (Throwable e) {
                    Log.e(e);
                }
            }
        } else {
            SettingEntryHook.INSTANCE.initialize();
        }
    }

    public static void doInitDelayableHooksMP() {
        for (IDynamicHook h : HookInstaller.queryAllAnnotatedHooks()) {
            try {
                if (h.isEnabled() && h.isTargetProcess() && !h.isPreparationRequired()) {
                    SyncUtils.requestInitHook(HookInstaller.getHookIndex(h), h.getTargetProcesses());
                    h.initialize();
                }
            } catch (Throwable e) {
                Log.e(e);
            }
        }
    }
}
