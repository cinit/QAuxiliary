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
package io.github.qauxv.lifecycle;

import static io.github.qauxv.util.Initiator.load;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import io.github.qauxv.util.xpcompat.XC_MethodHook;
import io.github.qauxv.util.xpcompat.XposedBridge;
import io.github.qauxv.activity.SettingsUiFragmentHostActivity;
import io.github.qauxv.fragment.EulaFragment;
import io.github.qauxv.fragment.TroubleshootFragment;
import io.github.qauxv.util.LicenseStatus;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.MainProcess;
import java.lang.reflect.Method;

/**
 * Used to jump into module proxy Activities from external Intent
 *
 * @author cinit
 */
public class JumpActivityEntryHook {

    public static final String JUMP_ACTION_CMD = "qa_jump_action_cmd";
    public static final String JUMP_ACTION_TARGET = "qa_jump_action_target";
    public static final String JUMP_ACTION_SETTING_ACTIVITY = "io.github.qauxv.SETTING_ACTIVITY";
    public static final String JUMP_ACTION_TROUBLE_SHOOTING_ACTIVITY = "io.github.qauxv.TROUBLE_SHOOTING_ACTIVITY";
    private static boolean __jump_act_init = false;

    @MainProcess
    @SuppressLint("PrivateApi")
    public static void initForJumpActivityEntry(Context ctx) {
        if (__jump_act_init) {
            return;
        }
        try {
            Class<?> clz = load("com.tencent.mobileqq.activity.JumpActivity");
            if (clz == null) {
                Log.i("class JumpActivity not found.");
                return;
            }
            Method doOnCreate = clz.getDeclaredMethod("doOnCreate", Bundle.class);
            XposedBridge.hookMethod(doOnCreate, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    final Activity activity = (Activity) param.thisObject;
                    Intent intent = activity.getIntent();
                    String cmd;
                    if (intent == null || (cmd = intent.getStringExtra(JUMP_ACTION_CMD)) == null) {
                        return;
                    }
                    if (JUMP_ACTION_SETTING_ACTIVITY.equals(cmd)) {
                        if (LicenseStatus.hasUserAcceptEula()) {
                            activity.startActivity(new Intent(activity, SettingsUiFragmentHostActivity.class));
                        } else {
                            SettingsUiFragmentHostActivity.startActivityForFragment(activity, EulaFragment.class, null);
                        }
                    } else if (JUMP_ACTION_TROUBLE_SHOOTING_ACTIVITY.equals(cmd)) {
                        SettingsUiFragmentHostActivity.startFragmentWithContext(activity, TroubleshootFragment.class);
                    }
                }
            });
            __jump_act_init = true;
        } catch (Exception e) {
            Log.e(e);
        }
    }
}
