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
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import cc.ioctl.hook.SettingEntryHook;
import cc.ioctl.util.LayoutHelper;
import cc.ioctl.util.Reflex;
import cc.ioctl.util.ui.drawable.ProportionDrawable;
import cc.ioctl.util.ui.drawable.SimpleBgDrawable;
import io.github.qauxv.SyncUtils;
import io.github.qauxv.base.IDynamicHook;
import io.github.qauxv.step.Step;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.LicenseStatus;
import io.github.qauxv.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class InjectDelayableHooks {

    private static boolean inited = false;

    public static boolean step(@NonNull Object director) {
        if (inited) {
            return true;
        }
        inited = true;
        Activity activity = (Activity) Reflex.getInstanceObjectOrNull(director, "a",
                Initiator.load("mqq/app/AppActivity"));
        if (activity == null) {
            activity = (Activity) Reflex.getFirstNSFByType(director, Initiator.load("mqq/app/AppActivity"));
        }
        final Activity ctx = activity;
        boolean needDeobf = false;
        IDynamicHook[] hooks = HookInstaller.queryAllAnnotatedHooks();
        for (IDynamicHook h : hooks) {
            if (h.isEnabled() && h.isPreparationRequired()) {
                needDeobf = true;
                break;
            }
        }
        final LinearLayout[] overlay = new LinearLayout[1];
        final LinearLayout[] main = new LinearLayout[1];
        final ProportionDrawable[] prog = new ProportionDrawable[1];
        final TextView[] text = new TextView[1];
        if (needDeobf) {
            final HashSet<Step> todos = new HashSet<>();
            for (IDynamicHook h : hooks) {
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
            }
            final ArrayList<Step> steps = new ArrayList<>(todos);
            Collections.sort(steps, Collections.<Step>reverseOrder());
            for (int idx = 0; idx < steps.size(); idx++) {
                final int j = idx;
                if (SyncUtils.isMainProcess() && ctx != null) {
                    ctx.runOnUiThread(() -> {
                        if (overlay[0] == null) {
                            overlay[0] = new LinearLayout(ctx);
                            overlay[0].setOrientation(LinearLayout.VERTICAL);
                            overlay[0].setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
                            overlay[0].setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
                            main[0] = new LinearLayout(ctx);
                            overlay[0].addView(main[0]);
                            main[0].setOrientation(LinearLayout.VERTICAL);
                            main[0].setGravity(Gravity.CENTER);
                            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                            llp.bottomMargin = LayoutHelper.dip2px(ctx, 55);
                            main[0].setLayoutParams(llp);
                            LinearLayout lprop = new LinearLayout(ctx);
                            ViewCompat.setBackground(lprop, new SimpleBgDrawable(0, 0xA0808080, 2));
                            final View _v = new View(ctx);
                            prog[0] = new ProportionDrawable(0xA0202020, 0x40FFFFFF,
                                    Gravity.LEFT, 0);
                            ViewCompat.setBackground(_v, prog[0]);
                            int __3_ = LayoutHelper.dip2px(ctx, 3);
                            LinearLayout.LayoutParams _tmp_lllp = new LinearLayout.LayoutParams(
                                    MATCH_PARENT, LayoutHelper.dip2px(ctx, 4));
                            _tmp_lllp.setMargins(__3_, __3_, __3_, __3_);
                            lprop.addView(_v, _tmp_lllp);
                            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                            int __5_ = LayoutHelper.dip2px(ctx, 5);
                            plp.setMargins(__5_ * 2, 0, __5_ * 2, __5_);
                            main[0].addView(lprop, plp);
                            text[0] = new TextView(ctx);
                            text[0].setTextSize(16);
                            text[0].setGravity(Gravity.CENTER_HORIZONTAL);
                            text[0].setTextColor(0xFF000000);
                            text[0].setShadowLayer(__5_ * 2, -0, -0, 0xFFFFFFFF);
                            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                            main[0].addView(text[0], tlp);
                            ((ViewGroup) ctx.getWindow().getDecorView()).addView(overlay[0]);
                        }
                        String statusText;
                        try {
                            statusText = "QAuxiliary 正在初始化:\n" + steps.get(j).getDescription() + "\n每个类一般不会超过一分钟";
                        } catch (Throwable e22) {
                            statusText = e22.toString();
                        }
                        text[0].setText(statusText);
                        prog[0].setProportion(1.0f * j / steps.size());
                    });
                }
                try {
                    steps.get(idx).step();
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
                            Log.e("InjectDelayableHooks/E not init " + h + " ,since checkPreconditions == false");
                        }
                    }
                } catch (Throwable e) {
                    Log.e(e);
                }
            }
        } else {
            SettingEntryHook.INSTANCE.initialize();
        }
        if (ctx != null && main[0] != null) {
            System.gc();
            ctx.runOnUiThread(() -> ((ViewGroup) ctx.getWindow().getDecorView()).removeView(overlay[0]));
        }
        return true;
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
