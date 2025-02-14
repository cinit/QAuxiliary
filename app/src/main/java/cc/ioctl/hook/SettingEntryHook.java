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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.LayoutHelper;
import cc.ioctl.util.Reflex;
import io.github.qauxv.BuildConfig;
import io.github.qauxv.R;
import io.github.qauxv.activity.SettingsUiFragmentHostActivity;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.core.HookInstaller;
import io.github.qauxv.fragment.EulaFragment;
import io.github.qauxv.fragment.FuncStatusDetailsFragment;
import io.github.qauxv.hook.BasePersistBackgroundHook;
import io.github.qauxv.lifecycle.Parasitics;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.LicenseStatus;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.xpcompat.XC_MethodHook;
import io.github.qauxv.util.xpcompat.XposedBridge;
import io.github.qauxv.util.xpcompat.XposedHelpers;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import kotlin.collections.ArraysKt;
import kotlin.jvm.functions.Function0;

@FunctionHookEntry
public class SettingEntryHook extends BasePersistBackgroundHook {

    public static final SettingEntryHook INSTANCE = new SettingEntryHook();

    private static final int BG_TYPE_SINGLE = 0;
    private static final int BG_TYPE_FIRST = 1;
    private static final int BG_TYPE_MIDDLE = 2;
    private static final int BG_TYPE_LAST = 3;

    // am start "intent:#Intent;component=com.tencent.mobileqq/com.tencent.mobileqq.activity.QPublicFragmentActivity;S.public_fragment_class=com.tencent.mobileqq.setting.main.MainSettingFragment;end"

    private SettingEntryHook() {
    }

    @Override
    public boolean initOnce() throws Exception {
        injectSettingEntryForMainSettingConfigProvider();
        // below 8.9.70
        Class<?> kQQSettingSettingActivity = Initiator._QQSettingSettingActivity();
        if (kQQSettingSettingActivity != null) {
            XposedHelpers.findAndHookMethod(kQQSettingSettingActivity, "doOnCreate", Bundle.class, mAddModuleEntry);
        }
        Class<?> kQQSettingSettingFragment = Initiator._QQSettingSettingFragment();
        if (kQQSettingSettingFragment != null) {
            Method doOnCreateView = kQQSettingSettingFragment.getDeclaredMethod("doOnCreateView",
                    LayoutInflater.class, ViewGroup.class, Bundle.class);
            XposedBridge.hookMethod(doOnCreateView, mAddModuleEntry);
        }
        return true;
    }

    private void injectSettingEntryForMainSettingConfigProvider() throws ReflectiveOperationException {
        // 8.9.70+
        Class<?> kMainSettingFragment = Initiator.load("com.tencent.mobileqq.setting.main.MainSettingFragment");
        if (kMainSettingFragment != null) {
            Class<?> kMainSettingConfigProvider = Initiator.loadClass("com.tencent.mobileqq.setting.main.MainSettingConfigProvider");
            // 9.1.20+, NewSettingConfigProvider, A/B test on 9.1.20
            Class<?> kNewSettingConfigProvider = Initiator.load("com.tencent.mobileqq.setting.main.NewSettingConfigProvider");
            Method getItemProcessListOld = Reflex.findSingleMethod(kMainSettingConfigProvider, List.class, false, Context.class);
            Method getItemProcessListNew = null;
            if (kNewSettingConfigProvider != null) {
                getItemProcessListNew = Reflex.findSingleMethod(kNewSettingConfigProvider, List.class, false, Context.class);
            }
            Class<?> kAbstractItemProcessor = Initiator.loadClass("com.tencent.mobileqq.setting.main.processor.AccountSecurityItemProcessor").getSuperclass();
            // SimpleItemProcessor has too few xrefs. I have no idea how to find it without a list of candidates.
            final String[] possibleSimpleItemProcessorNames = new String[]{
                    // 8.9.70 ~ 9.0.0
                    "com.tencent.mobileqq.setting.processor.g",
                    // 9.0.8+
                    "com.tencent.mobileqq.setting.processor.h",
                    // 9.1.50 (9006)
                    "com.tencent.mobileqq.setting.processor.i",
                    // QQ 9.1.28.21880 (8398) gray
                    "as3.i",
            };
            List<Class<?>> possibleSimpleItemProcessorCandidates = new ArrayList<>(5);
            for (String name : possibleSimpleItemProcessorNames) {
                Class<?> klass = Initiator.load(name);
                if (klass != null && klass.getSuperclass() == kAbstractItemProcessor) {
                    possibleSimpleItemProcessorCandidates.add(klass);
                }
            }
            // assert possibleSimpleItemProcessorCandidates.size() == 1;
            if (possibleSimpleItemProcessorCandidates.size() != 1) {
                throw new IllegalStateException("possibleSimpleItemProcessorCandidates.size() != 1, got " + possibleSimpleItemProcessorCandidates);
            }
            Class<?> kSimpleItemProcessor = possibleSimpleItemProcessorCandidates.get(0);
            Method setOnClickListener;
            {
                List<Method> candidates = ArraysKt.filter(kSimpleItemProcessor.getDeclaredMethods(), m -> {
                    Class<?>[] argt = m.getParameterTypes();
                    // NOSONAR java:S1872 not same class
                    return m.getReturnType() == void.class && argt.length == 1 && Function0.class.getName().equals(argt[0].getName());
                });
                candidates.sort(Comparator.comparing(Method::getName));
                // TIM 4.0.95.4001 only have one method, that is the one we need (onClick() lambda)
                if (candidates.size() != 2 && candidates.size() != 1) {
                    throw new IllegalStateException("com.tencent.mobileqq.setting.processor.g.?(Function0)V candidates.size() != 1|2");
                }
                // take the smaller one
                setOnClickListener = candidates.get(0);
            }
            Constructor<?> ctorSimpleItemProcessor = kSimpleItemProcessor.getDeclaredConstructor(Context.class, int.class, CharSequence.class, int.class);
            XC_MethodHook callback = HookUtils.afterAlways(this, 50, param -> {
                List<Object> result = (List<Object>) param.getResult();
                Context ctx = (Context) param.args[0];
                Class<?> kItemProcessorGroup = result.get(0).getClass();
                Constructor<?> ctor = kItemProcessorGroup.getDeclaredConstructor(List.class, CharSequence.class, CharSequence.class);
                Parasitics.injectModuleResources(ctx.getResources());
                @SuppressLint("DiscouragedApi")
                int resId = ctx.getResources().getIdentifier("qui_tuning", "drawable", ctx.getPackageName());
                Object entryItem = ctorSimpleItemProcessor.newInstance(ctx, R.id.setting2Activity_settingEntryItem, "QAuxiliary", resId);
                Class<?> thatFunction0 = setOnClickListener.getParameterTypes()[0];
                Object theUnit = thatFunction0.getClassLoader().loadClass("kotlin.Unit").getField("INSTANCE").get(null);
                ClassLoader hostClassLoader = Initiator.getHostClassLoader();
                Object func0 = Proxy.newProxyInstance(hostClassLoader, new Class<?>[]{thatFunction0}, (proxy, method, args) -> {
                    if (method.getName().equals("invoke")) {
                        onSettingEntryClick(ctx);
                        return theUnit;
                    }
                    // must be sth from Object
                    return method.invoke(this, args);
                });
                setOnClickListener.invoke(entryItem, func0);
                ArrayList<Object> list = new ArrayList<>(1);
                list.add(entryItem);
                Object group = ctor.newInstance(list, "", "");
                boolean isNew = param.thisObject.getClass().getName().contains("NewSettingConfigProvider");
                int indexToInsert = isNew ? 2 : 1;
                result.add(indexToInsert, group);
            });
            XposedBridge.hookMethod(getItemProcessListOld, callback);
            if (getItemProcessListNew != null) {
                XposedBridge.hookMethod(getItemProcessListNew, callback);
            }
        }
    }

    private final XC_MethodHook mAddModuleEntry = new XC_MethodHook(51) {
        @Override
        protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
            try {
                final Activity activity;
                var thisObject = param.thisObject;
                if (thisObject instanceof Activity) {
                    activity = (Activity) thisObject;
                } else {
                    activity = (Activity) Reflex.invokeVirtual(thisObject, "getActivity");
                }
                Resources res = activity.getResources();
                Class<?> itemClass;
                View itemRef = null;
                {
                    Class<?> clz = load("com/tencent/mobileqq/widget/FormSimpleItem");
                    if (clz != null) {
                        // find a candidate view field
                        for (Field f : thisObject.getClass().getDeclaredFields()) {
                            if (f.getType() == clz && !Modifier.isStatic(f.getModifiers())) {
                                f.setAccessible(true);
                                View v = (View) f.get(thisObject);
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
                if (HookInstaller.getFuncInitException() != null) {
                    Reflex.invokeVirtual(item, "setRightText", "[严重错误]", CharSequence.class);
                } else if (LicenseStatus.hasUserAcceptEula()) {
                    Reflex.invokeVirtual(item, "setRightText", BuildConfig.VERSION_NAME, CharSequence.class);
                } else {
                    Reflex.invokeVirtual(item, "setRightText", "[未激活]", CharSequence.class);
                }
                item.setOnClickListener(v -> {
                    onSettingEntryClick(activity);
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

    private void onSettingEntryClick(@NonNull Context context) {
        if (HookInstaller.getFuncInitException() != null) {
            SettingsUiFragmentHostActivity.startActivityForFragment(context, FuncStatusDetailsFragment.class,
                    FuncStatusDetailsFragment.getBundleForLocation(FuncStatusDetailsFragment.TARGET_INIT_EXCEPTION));
        } else if (LicenseStatus.hasUserAcceptEula()) {
            context.startActivity(new Intent(context, SettingsUiFragmentHostActivity.class));
        } else {
            SettingsUiFragmentHostActivity.startActivityForFragment(context, EulaFragment.class, null);
            if (context instanceof Activity) {
                ((Activity) context).finish();
            }
        }
    }

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
