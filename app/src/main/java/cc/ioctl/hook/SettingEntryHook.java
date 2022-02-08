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

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static io.github.qauxv.util.Initiator.load;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import cc.ioctl.util.Reflex;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import io.github.qauxv.BuildConfig;
import io.github.qauxv.R;
import io.github.qauxv.activity.SettingsUiFragmentHostActivity;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.hook.BasePersistBackgroundHook;
import io.github.qauxv.util.LicenseStatus;

@FunctionHookEntry
public class SettingEntryHook extends BasePersistBackgroundHook {

    public static final SettingEntryHook INSTANCE = new SettingEntryHook();

    private SettingEntryHook() {
    }

    @Override
    public boolean initOnce() throws Exception {
        XposedHelpers.findAndHookMethod(
                load("com.tencent.mobileqq.activity.QQSettingSettingActivity"),
                "doOnCreate", Bundle.class, mAddModuleEntry);
        return true;
    }

    private final XC_MethodHook mAddModuleEntry = new XC_MethodHook(51) {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            try {
                final Activity activity = (Activity) param.thisObject;
                Class<?> itemClass;
                View itemRef;
                itemRef = (View) Reflex.getInstanceObjectOrNull(activity, "a",
                        load("com/tencent/mobileqq/widget/FormSimpleItem"));
                if (itemRef == null &&
                        (itemClass = load("com/tencent/mobileqq/widget/FormCommonSingleLineItem")) != null) {
                    itemRef = (View) Reflex.getInstanceObjectOrNull(activity, "a", itemClass);
                }
                if (itemRef == null) {
                    Class<?> clz = load("com/tencent/mobileqq/widget/FormCommonSingleLineItem");
                    if (clz == null) {
                        clz = load("com/tencent/mobileqq/widget/FormSimpleItem");
                    }
                    itemRef = (View) Reflex.getFirstNSFByType(activity, clz);
                }
                View item;
                if (itemRef == null) {
                    // we are in triassic period?
                    item = (View) Reflex.newInstance(
                            load("com/tencent/mobileqq/widget/FormSimpleItem"),
                            activity, Context.class);
                } else {
                    // modern age
                    item = (View) Reflex.newInstance(itemRef.getClass(), activity, Context.class);
                }
                item.setId(R.id.setting2Activity_settingEntryItem);
                Reflex.invokeVirtual(item, "setLeftText", "QAuxiliary", CharSequence.class);
                Reflex.invokeVirtual(item, "setBgType", 2, int.class);
                if (LicenseStatus.hasUserAcceptEula()) {
                    Reflex.invokeVirtual(item, "setRightText", BuildConfig.VERSION_NAME, CharSequence.class);
                } else {
                    Reflex.invokeVirtual(item, "setRightText", "[未激活]", CharSequence.class);
                }
                item.setOnClickListener(v -> {
                    if (true || LicenseStatus.hasUserAcceptEula()) {
                        activity.startActivity(new Intent(activity, SettingsUiFragmentHostActivity.class));
                    } else {
                        // TODO: 2022-02-08 add eula activity
                        throw new UnsupportedOperationException(
                                "activity.startActivity(new Intent(activity, EulaActivity.class));activity.finish();");
                    }
                });
                if (itemRef != null) {
                    //modern age
                    ViewGroup list = (ViewGroup) itemRef.getParent();
                    ViewGroup.LayoutParams reflp;
                    if (list.getChildCount() == 1) {
                        //junk!
                        list = (ViewGroup) list.getParent();
                        reflp = ((View) itemRef.getParent()).getLayoutParams();
                    } else {
                        reflp = itemRef.getLayoutParams();
                    }
                    ViewGroup.LayoutParams lp = null;
                    if (reflp != null) {
                        lp = new ViewGroup.LayoutParams(
                                MATCH_PARENT, /*reflp.height*/WRAP_CONTENT);
                    }
                    int index = 0;
                    int account_switch = list.getContext().getResources()
                            .getIdentifier("account_switch", "id", list.getContext().getPackageName());
                    try {
                        if (account_switch > 0) {
                            View accountItem = (View) list
                                    .findViewById(account_switch).getParent();
                            for (int i = 0; i < list.getChildCount(); i++) {
                                if (list.getChildAt(i) == accountItem) {
                                    index = i + 1;
                                    break;
                                }
                            }
                        }
                        if (index > list.getChildCount()) {
                            index = 0;
                        }
                    } catch (NullPointerException ignored) {
                    }
                    list.addView(item, index, lp);
                } else {
                    // triassic period, we have to find the ViewGroup ourselves
                    int qqsetting2_msg_notify = activity.getResources()
                            .getIdentifier("qqsetting2_msg_notify", "id", activity.getPackageName());
                    if (qqsetting2_msg_notify == 0) {
                        throw new UnsupportedOperationException("R.id.qqsetting2_msg_notify not found in triassic period");
                    } else {
                        ViewGroup vg = (ViewGroup) activity
                                .findViewById(qqsetting2_msg_notify).getParent()
                                .getParent();
                        vg.addView(item, 0, new ViewGroup.LayoutParams(
                                MATCH_PARENT, /*reflp.height*/WRAP_CONTENT));
                    }
                }
            } catch (Throwable e) {
                traceError(e);
                throw e;
            }
        }
    };
}
