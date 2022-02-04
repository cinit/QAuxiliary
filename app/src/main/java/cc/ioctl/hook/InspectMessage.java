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

import static cc.ioctl.util.Reflex.getFirstNSFByType;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static io.github.qauxv.util.Initiator.load;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.Reflex;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.DebugCategory;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.ui.CustomDialog;
import io.github.qauxv.util.DexKit;
import io.github.qauxv.util.LicenseStatus;
import io.github.qauxv.util.Toasts;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@FunctionHookEntry
public class InspectMessage extends CommonSwitchFunctionHook implements View.OnLongClickListener {

    public static final InspectMessage INSTANCE = new InspectMessage();
    static Field f_panel;
    boolean bInspectMode = false;

    private InspectMessage() {
        super(new int[]{DexKit.C_AIO_UTILS});
    }

    @Override
    public boolean initOnce() throws Exception {
        findAndHookMethod(load("com/tencent/mobileqq/activity/aio/BaseBubbleBuilder"),
                "onClick", View.class, new XC_MethodHook(49) {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (LicenseStatus.sDisableCommonHooks) {
                            return;
                        }
                        if (!bInspectMode) {
                            return;
                        }
                        if (!isEnabled()) {
                            return;
                        }
                        Context ctx = Reflex.getInstanceObjectOrNull(param.thisObject, "a",
                                Context.class);
                        if (ctx == null) {
                            ctx = getFirstNSFByType(param.thisObject, Context.class);
                        }
                        View view = (View) param.args[0];
                        if (ctx == null || MultiForwardAvatarHook.isLeftCheckBoxVisible()) {
                            return;
                        }
                        String activityName = ctx.getClass().getName();
                        if (activityName
                                .equals("com.tencent.mobileqq.activity.MultiForwardActivity")) {
                            return;
                        }
                        final Object msg = MultiForwardAvatarHook.getChatMessageByView(view);
                        if (msg == null) {
                            return;
                        }
                        //取消istroop判断，在群里也可以撤回部分消息
                        CustomDialog dialog = CustomDialog.createFailsafe(ctx);
                        dialog.setTitle(Reflex.getShortClassName(msg));
                        dialog.setMessage(msg.toString());
                        dialog.setCancelable(true);
                        dialog.setPositiveButton("确认", null);
                        dialog.show();
                        param.setResult(null);
                    }
                });
        //begin panel
        Method a = null, b = null, c = null, _emmm_ = null;
        for (Method m : load("com.tencent.mobileqq.activity.aio.panel.PanelIconLinearLayout")
                .getDeclaredMethods()) {
            if (m.getReturnType().equals(void.class) && Modifier.isPublic(m.getModifiers())
                    && !Modifier.isStatic(m.getModifiers())
                    && m.getParameterTypes().length == 0) {
                String name = m.getName();
                if ("a".equals(name)) {
                    a = m;
                } else if ("b".equals(name)) {
                    b = m;
                } else if ("c".equals(name)) {
                    c = m;
                } else if (m.getName().length() < 4) {
                    _emmm_ = m;
                }
            }
        }
        Method m = c == null ? a : b;
        if (m == null) {
            m = _emmm_;
        }
        XposedBridge.hookMethod(m, new XC_MethodHook(49) {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (LicenseStatus.sDisableCommonHooks) {
                    return;
                }
                if (!isEnabled()) {
                    return;
                }
                ViewGroup panel = (ViewGroup) param.thisObject;
                View v = panel.getChildAt(panel.getChildCount() - 1);
                if (v instanceof ViewGroup) {
                    View v2;
                    for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) {
                        v2 = ((ViewGroup) v).getChildAt(i);
                        if (!(v2 instanceof ViewGroup)) {
                            v2.setOnLongClickListener(InspectMessage.this);
                        }
                    }
                } else {
                    v.setOnLongClickListener(InspectMessage.this);
                }
            }
        });
        //end panel
        //begin tweak
        findAndHookMethod(load("com.tencent.mobileqq.activity.aio.panel.PanelIconLinearLayout"),
                "setAllEnable", boolean.class, new XC_MethodHook(47) {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (LicenseStatus.sDisableCommonHooks) {
                            return;
                        }
                        if (!isEnabled()) {
                            return;
                        }
                        boolean z = (boolean) param.args[0];
                        ViewGroup panel = (ViewGroup) param.thisObject;
                        int cnt = panel.getChildCount();
                        if (cnt == 0) {
                            return;
                        }
                        View v = panel.getChildAt(cnt - 1);
                        v.setEnabled(true);
                        v.setClickable(z);
                        v.setLongClickable(true);
                    }
                });
        //end tweak
        return true;
    }

    @Override
    public boolean onLongClick(View v) {
        bInspectMode = !bInspectMode;
        Context ctx = v.getContext();
        if (bInspectMode) {
            Toasts.info(ctx, "已开启检查消息");
        } else {
            Toasts.info(ctx, "已关闭检查消息");
        }
        return true;
    }

    @NonNull
    @Override
    public String getName() {
        return "检查消息";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "仅用于调试，无其他作用";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return DebugCategory.DEBUG_CATEGORY;
    }
}
