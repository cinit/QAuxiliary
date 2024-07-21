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
        findAndHookMethod(load("com/tencent/widget/PinnedHeaderExpandableListView"),
            "setAdapter", ExpandableListAdapter.class, exfriendEntryHook);
        AppCenterHookKt.initAppCenterHook();
        XposedHelpers.findAndHookMethod(load("com/tencent/mobileqq/activity/SplashActivity"), "doOnResume",
            new XC_MethodHook(700) {
                boolean z = false;

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        if (AppRuntimeHelper.getLongAccountUin() > 10000) {
                            ExfriendManager ex = ExfriendManager.getCurrent();
                            ex.timeToUpdateFl();
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
        findAndHookMethod(load("friendlist/GetFriendListResp"), "readFrom",
            load("com/qq/taf/jce/JceInputStream"), new XC_MethodHook(200) {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        FriendChunk fc = new FriendChunk(param.thisObject);
                        ExfriendManager.onGetFriendListResp(fc);
                    } catch (Throwable e) {
                        traceError(e);
                        throw e;
                    }
                }
            });

        findAndHookMethod(load("friendlist/DelFriendResp"), "readFrom",
            load("com/qq/taf/jce/JceInputStream"), new XC_MethodHook(200) {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        long uin = (Long) Reflex.getInstanceObjectOrNull(param.thisObject, "uin");
                        long deluin = (Long) Reflex.getInstanceObjectOrNull(param.thisObject, "deluin");
                        int result = (Integer) Reflex.getInstanceObjectOrNull(param.thisObject, "result");
                        short errorCode = (Short) Reflex.getInstanceObjectOrNull(param.thisObject, "errorCode");
                        if (result == 0 && errorCode == 0) {
                            ExfriendManager.get(uin).markActiveDelete(deluin);
                        }
                    } catch (Throwable e) {
                        traceError(e);
                        throw e;
                    }
                }
            });
        return true;
    }
}
