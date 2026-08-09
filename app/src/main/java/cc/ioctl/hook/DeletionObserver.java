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


import static android.widget.LinearLayout.LayoutParams.MATCH_PARENT;
import static android.widget.LinearLayout.LayoutParams.WRAP_CONTENT;
import static cc.ioctl.util.LayoutHelper.dip2px;
import static io.github.qauxv.util.xpcompat.XposedHelpers.findAndHookMethod;
import static io.github.qauxv.util.Initiator.load;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import cc.ioctl.fragment.ExfriendListFragment;
import cc.ioctl.hook.friend.ShowDeletedFriendListEntry;
import cc.ioctl.util.ExfriendManager;
import cc.ioctl.util.LayoutHelper;
import cc.ioctl.util.Reflex;
import io.github.qauxv.util.xpcompat.XC_MethodHook;
import io.github.qauxv.util.xpcompat.XposedHelpers;
import io.github.qauxv.activity.SettingsUiFragmentHostActivity;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.bridge.AppRuntimeHelper;
import io.github.qauxv.bridge.FriendChunk;
import io.github.qauxv.hook.BasePersistBackgroundHook;
import io.github.qauxv.util.CliOper;
import io.github.qauxv.util.LicenseStatus;
import io.github.qauxv.util.Log;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import me.singleneuron.hook.AppCenterHookKt;

@FunctionHookEntry
public class DeletionObserver extends BasePersistBackgroundHook {

    private DeletionObserver() {
        super();
    }

    public static final DeletionObserver INSTANCE = new DeletionObserver();
    public static final int VIEW_ID_DELETED_FRIEND = 0x00EE77AA;
    public HashSet addedListView = new HashSet();
    public WeakReference<TextView> exfriendRef;
    public WeakReference<TextView> redDotRef;
    private final XC_MethodHook exfriendEntryHook = new XC_MethodHook(55) {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                if (LicenseStatus.sDisableCommonHooks || !ShowDeletedFriendListEntry.INSTANCE.isEnable()) {
                    return;
                }
                if (!param.thisObject.getClass().getName().contains("ContactsFPSPinnedHeaderExpandableListView")) {
                    return;
                }
                LinearLayout layout_entrance;
                View lv = (View) param.thisObject;
                final Activity splashActivity = (Activity) lv.getContext();
                layout_entrance = new LinearLayout(splashActivity);
                RelativeLayout rell = new RelativeLayout(splashActivity);
                if (!addedListView.contains(lv)) {
                    Reflex.invokeVirtualOriginal(lv, "addFooterView", layout_entrance, View.class);
                    addedListView.add(lv);
                }
                layout_entrance.setOrientation(LinearLayout.VERTICAL);
                TextView exfriend;
                if (exfriendRef == null || (exfriend = exfriendRef.get()) == null) {
                    exfriend = new TextView(splashActivity);
                    exfriendRef = new WeakReference<>(exfriend);
                }
                exfriend.setTextColor(0xFF3030FF);
                exfriend.setTextSize(LayoutHelper.dip2sp(splashActivity, 17));
                exfriend.setId(VIEW_ID_DELETED_FRIEND);
                exfriend.setText("历史好友");
                exfriend.setGravity(Gravity.CENTER);
                exfriend.setClickable(true);

                TextView redDot = new TextView(splashActivity);
                redDotRef = new WeakReference<>(redDot);
                redDot.setTextColor(0xFFFF0000);

                redDot.setGravity(Gravity.CENTER);
                redDot.getPaint().setFakeBoldText(true);
                redDot.setTextSize(LayoutHelper.dip2sp(splashActivity, 10));
                try {
                    Reflex.invokeStatic(load("com/tencent/widget/CustomWidgetUtil"), "a", redDot, 3, 1, 0,
                        TextView.class, int.class, int.class, int.class, void.class);
                } catch (NullPointerException e) {
                    redDot.setTextColor(Color.RED);
                }
                ExfriendManager.get(AppRuntimeHelper.getLongAccountUin()).setRedDot();

                int height = dip2px(splashActivity, 48);
                RelativeLayout.LayoutParams exlp = new RelativeLayout.LayoutParams(MATCH_PARENT,
                    height);
                exlp.topMargin = 0;
                exlp.leftMargin = 0;
                try {
                    if (exfriend.getParent() != null) {
                        ((ViewGroup) exfriend.getParent()).removeView(exfriend);
                    }
                } catch (Exception e) {
                    Log.e(e);
                }
                rell.addView(exfriend, exlp);
                RelativeLayout.LayoutParams dotlp = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
                dotlp.topMargin = 0;
                dotlp.rightMargin = LayoutHelper.dip2px(splashActivity, 24);
                dotlp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                dotlp.addRule(RelativeLayout.CENTER_VERTICAL);
                rell.addView(redDot, dotlp);
                layout_entrance.addView(rell);
                ViewGroup.LayoutParams llp = new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
                layout_entrance.setPadding(0, (int) (height * 0.3f), 0, (int) (0.3f * height));
                exfriend.setOnClickListener(v -> SettingsUiFragmentHostActivity
                        .startActivityForFragment(splashActivity, ExfriendListFragment.class, null));
                exfriend.postInvalidate();
            } catch (Throwable e) {
                traceError(e);
                throw e;
            }
        }
    };

    @Override
    protected boolean initOnce() throws Exception {
        hookDelFriendNt();
        hookGatheredContactsResp();
        hookSplashActivity();
        hookFriendListEntry();
        AppCenterHookKt.initAppCenterHook();
        startPeriodicFlRefresh();
        return true;
    }

    /**
     * QQ 9.3.30: 定时拉取好友列表
     */
    private void startPeriodicFlRefresh() {
        try {
            final android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        ExfriendManager ex = ExfriendManager.getCurrent();
                        if (ex.isAutoDetectEnabled() && AppRuntimeHelper.getLongAccountUin() > 10000) {
                            ex.doRequestFlRefresh();
                        }
                    } catch (Throwable e) {
                        traceError(e);
                    }
                    long intervalMs = PERIODIC_FL_REFRESH_DEFAULT_MS;
                    try {
                        intervalMs = ExfriendManager.getCurrent().getDetectIntervalMin() * 60 * 1000L;
                    } catch (Throwable ignored) {
                    }
                    h.postDelayed(this, intervalMs);
                }
            }, PERIODIC_FL_REFRESH_DEFAULT_MS);
            Log.i("QAuxv-Del: " + "定时拉取已启动，间隔=" + (PERIODIC_FL_REFRESH_DEFAULT_MS / 60000) + "分钟（可配置）");
        } catch (Throwable e) {
            traceError(e);
        }
    }

    private static final long PERIODIC_FL_REFRESH_DEFAULT_MS = 5 * 60 * 1000L;

    /**
     * QQ 9.3.30 NT 主动删除入口：FriendListHandler.delFriend(String source, String uin, byte delType, int notShield)
     */
    private void hookDelFriendNt() {
        try {
            findAndHookMethod(load("com/tencent/mobileqq/app/FriendListHandler"),
                "delFriend", String.class, String.class, byte.class, int.class, new XC_MethodHook(200) {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            // uin 在 args[1]
                            String uinStr = String.valueOf(param.args[1]);
                            long deluin = 0;
                            try {
                                deluin = Long.parseLong(uinStr);
                            } catch (NumberFormatException e) {
                                Log.i("QAuxv-Del: " + "delFriend uin 解析失败: " + uinStr);
                            }
                            if (deluin > 10000) {
                                long uin = AppRuntimeHelper.getLongAccountUin();
                                Log.i("QAuxv-Del: " + "主动删除请求(handler): deluin=" + deluin);
                                ExfriendManager.get(uin).markActiveDelete(deluin);
                            }
                        } catch (Throwable e) {
                            Log.i("QAuxv-Del: " + "hookDelFriendHandler 异常: " + e);
                            traceError(e);
                        }
                    }
                });
            Log.i("QAuxv-Del: " + "hookDelFriendHandler(4参) 注册成功");
        } catch (Throwable e) {
            Log.i("QAuxv-Del: " + "hookDelFriendHandler(4参) 注册失败: " + e);
            traceError(e);
        }
        try {
            // QQ 9.3.30: 删除响应回调（被动删除）
            findAndHookMethod(load("com/tencent/mobileqq/app/bb"),
                "onUpdateDelFriend", boolean.class, Object.class, new XC_MethodHook(200) {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            boolean success = (Boolean) param.args[0];
                            Object data = param.args[1];
                            if (success && data != null) {
                                long deluin = extractDelUin(data);
                                if (deluin > 10000) {
                                    // 观察者回调同一删除会触发多次，5 秒内同一 uin 只处理一次
                                    long now = System.currentTimeMillis();
                                    if (deluin == sLastDelUin && now - sLastDelTime < 5000) {
                                        return;
                                    }
                                    sLastDelUin = deluin;
                                    sLastDelTime = now;
                                    long uin = AppRuntimeHelper.getLongAccountUin();
                                    Log.i("QAuxv-Del: " + "删除响应确认(被动): deluin=" + deluin);
                                    ExfriendManager.get(uin).markPassiveDelete(deluin);
                                }
                            }
                        } catch (Throwable e) {
                            traceError(e);
                        }
                    }
                });
            Log.i("QAuxv-Del: " + "hookDelFriendNt(回调) 注册成功");
        } catch (Throwable e) {
            Log.i("QAuxv-Del: " + "hookDelFriendNt(回调) 注册失败: " + e);
            traceError(e);
        }
    }

    private static long sLastDelUin = 0;
    private static long sLastDelTime = 0;

    /**
     * 从删除响应数据中提取被删好友 uin
     */    private long extractDelUin(Object data) {
        try {
            if (data instanceof Long) {
                return (Long) data;
            }
            if (data instanceof String) {
                return Long.parseLong((String) data);
            }
            for (String f : new String[]{"deluin", "uin"}) {
                try {
                    java.lang.reflect.Field fd = data.getClass().getField(f);
                    fd.setAccessible(true);
                    Object v = fd.get(data);
                    if (v instanceof Number) {
                        return ((Number) v).longValue();
                    }
                    if (v instanceof String) {
                        return Long.parseLong((String) v);
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    /**
     * QQ 9.3.30: OidbSvc.0x7c4 响应处理
     */
    private void hookGatheredContactsResp() {
        try {
            findAndHookMethod(load("com/tencent/mobileqq/app/FriendListHandler"),
                "handleGetGatheredContactsList",
                load("com/tencent/qphone/base/remote/ToServiceMsg"),
                load("com/tencent/qphone/base/remote/FromServiceMsg"),
                Object.class,
                new XC_MethodHook(200) {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            if (LicenseStatus.sDisableCommonHooks) {
                                return;
                            }
                            if (param.args.length < 3 || !(param.args[2] instanceof byte[])) {
                                return;
                            }
                            byte[] oidb = (byte[]) param.args[2];
                            ExfriendManager.onGatheredContactsResp(oidb);
                        } catch (Throwable e) {
                            traceError(e);
                            throw e;
                        }
                    }
                });
            Log.i("QAuxv-Del: " + "hookGatheredContactsResp 注册成功");
        } catch (Throwable e) {
            Log.i("QAuxv-Del: " + "hookGatheredContactsResp 注册失败: " + e);
            traceError(e);
        }
    }

    private void hookSplashActivity() {
        try {
            XposedHelpers.findAndHookMethod(load("com/tencent/mobileqq/activity/SplashActivity"), "doOnResume",
                new XC_MethodHook(700) {
                    boolean z = false;

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            if (AppRuntimeHelper.getLongAccountUin() > 10000) {
                                ExfriendManager ex = ExfriendManager.getCurrent();
                                // 延迟拉取，等待登录态就绪
                                android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
                                h.postDelayed(() -> ex.doRequestFlRefresh(), 8000);
                            }
                        } catch (Throwable e) {
                            traceError(e);
                            throw e;
                        }
                        if (z) {
                            return;
                        }
                        z = true;
                        CliOper.onLoad();
                    }
                });
        } catch (Throwable e) {
            traceError(e);
        }
    }

    private void hookFriendListEntry() {
        try {
            findAndHookMethod(load("com/tencent/widget/PinnedHeaderExpandableListView"),
                "setAdapter", ExpandableListAdapter.class, exfriendEntryHook);
        } catch (Throwable e) {
            traceError(e);
        }
    }
}
