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
import static io.github.qauxv.util.Initiator.loadClass;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.LayoutHelper;
import cc.ioctl.util.Reflex;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import io.github.qauxv.BuildConfig;
import io.github.qauxv.R;
import io.github.qauxv.activity.SettingsUiFragmentHostActivity;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.fragment.EulaFragment;
import io.github.qauxv.hook.BasePersistBackgroundHook;
import io.github.qauxv.util.LicenseStatus;
import io.github.qauxv.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@FunctionHookEntry
public class SettingEntryHook extends BasePersistBackgroundHook {

    public static final SettingEntryHook INSTANCE = new SettingEntryHook();

    private static final int BG_TYPE_SINGLE = 0;
    private static final int BG_TYPE_FIRST = 1;
    private static final int BG_TYPE_MIDDLE = 2;
    private static final int BG_TYPE_LAST = 3;

    private SettingEntryHook() {
    }

    @Override
    public boolean initOnce() throws Exception {
        Class<?> kQQSettingSettingActivity = loadClass("com.tencent.mobileqq.activity.QQSettingSettingActivity");
        XposedHelpers.findAndHookMethod(kQQSettingSettingActivity, "doOnCreate", Bundle.class, mAddModuleEntry);
        return true;
    }

    private final XC_MethodHook mAddModuleEntry = new XC_MethodHook(51) {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            try {
                final Activity activity = (Activity) param.thisObject;
                Resources res = activity.getResources();
                Class<?> itemClass;
                View itemRef = null;
                {
                    Class<?> clz = load("com/tencent/mobileqq/widget/FormSimpleItem");
                    if (clz != null) {
                        // find a candidate view field
                        for (Field f : activity.getClass().getDeclaredFields()) {
                            if (f.getType() == clz && !Modifier.isStatic(f.getModifiers())) {
                                f.setAccessible(true);
                                View v = (View) f.get(activity);
                                if (v != null && v.getParent() != null) {
                                    itemRef = v;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (itemRef == null && (itemClass = load("com/tencent/mobileqq/widget/FormCommonSingleLineItem")) != null) {
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
                    item = (View) Reflex.newInstance(load("com/tencent/mobileqq/widget/FormSimpleItem"), activity, Context.class);
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
                    if (LicenseStatus.hasUserAcceptEula()) {
                        activity.startActivity(new Intent(activity, SettingsUiFragmentHostActivity.class));
                    } else {
                        SettingsUiFragmentHostActivity.startActivityForFragment(activity, EulaFragment.class, null);
                        activity.finish();
                    }
                });
                if (itemRef != null && !HostInfo.isQQHD()) {
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
                        lp = new ViewGroup.LayoutParams(MATCH_PARENT, /*reflp.height*/WRAP_CONTENT);
                    }
                    int index = 0;
                    int account_switch = res.getIdentifier("account_switch", "id", list.getContext().getPackageName());
                    try {
                        if (account_switch > 0) {
                            View accountItem = (View) list.findViewById(account_switch).getParent();
                            if (accountItem != null && accountItem.getParent() != null) {
                                // fix up the parent for CHA
                                list = (ViewGroup) accountItem.getParent();
                            }
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
                    fixBackgroundType(list, item, index);
                } else {
                    // triassic period, we have to find the ViewGroup ourselves
                    int qqsetting2_msg_notify = res.getIdentifier("qqsetting2_msg_notify", "id", activity.getPackageName());
                    if (qqsetting2_msg_notify == 0) {
                        throw new UnsupportedOperationException("R.id.qqsetting2_msg_notify not found in triassic period");
                    } else {
                        ViewGroup vg = (ViewGroup) activity.findViewById(qqsetting2_msg_notify).getParent().getParent();
                        vg.addView(item, 0, new ViewGroup.LayoutParams(MATCH_PARENT, /*reflp.height*/WRAP_CONTENT));
                    }
                }
            } catch (Throwable e) {
                traceError(e);
                throw e;
            }
        }
    };

    private void fixBackgroundType(@NonNull ViewGroup parent, @NonNull View itemView, int index) {
        int lastClusterId = index - 1;
        if (lastClusterId < 0) {
            // unexpected
            return;
        }
        // make QQ 8.8.80 happy
        try {
            Reflex.invokeVirtual(itemView, "setBgType", 0, int.class);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) itemView.getLayoutParams();
            lp.setMargins(0, LayoutHelper.dip2px(parent.getContext(), 15), 0, 0);
            parent.requestLayout();
        } catch (ReflectiveOperationException e) {
            Log.e(e);
        }
    }
}
