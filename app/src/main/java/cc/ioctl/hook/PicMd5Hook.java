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

import static io.github.qauxv.util.Initiator.load;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.Reflex;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import io.github.qauxv.R;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.ui.CustomDialog;
import io.github.qauxv.util.CustomMenu;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.LicenseStatus;
import io.github.qauxv.util.Toasts;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import xyz.nextalone.util.SystemServiceUtils;

@FunctionHookEntry
@UiItemAgentEntry
public class PicMd5Hook extends CommonSwitchFunctionHook {

    public static final PicMd5Hook INSTANCE = new PicMd5Hook();

    private PicMd5Hook() {
    }

    @NonNull
    @Override
    public String getName() {
        return "显示图片MD5";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "长按图片消息点击MD5, 可同时复制图片链接";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.MESSAGE_CATEGORY;
    }

    @Override
    public boolean initOnce() throws Exception {
        Class<?> cl_PicItemBuilder = Initiator._PicItemBuilder();
        Class<?> cl_BasePicItemBuilder = cl_PicItemBuilder.getSuperclass();
        try {
            XposedHelpers.findAndHookMethod(cl_PicItemBuilder, "a", int.class, Context.class,
                    load("com/tencent/mobileqq/data/ChatMessage"), new MenuItemClickCallback());
            XposedHelpers.findAndHookMethod(cl_BasePicItemBuilder, "a", int.class,
                    Context.class,
                    load("com/tencent/mobileqq/data/ChatMessage"), new MenuItemClickCallback());
        } catch (Exception e) {
        }
        for (Method m : cl_PicItemBuilder.getDeclaredMethods()) {
            if (!m.getReturnType().isArray()) {
                continue;
            }
            Class<?>[] ps = m.getParameterTypes();
            if (ps.length == 1 && ps[0].equals(View.class)) {
                XposedBridge.hookMethod(m, new GetMenuItemCallBack());
                break;
            }
        }
        for (Method m : cl_BasePicItemBuilder.getDeclaredMethods()) {
            if (!m.getReturnType().isArray()) {
                continue;
            }
            Class<?>[] ps = m.getParameterTypes();
            if (ps.length == 1 && ps[0].equals(View.class)) {
                XposedBridge.hookMethod(m, new GetMenuItemCallBack());
                break;
            }
        }
        return true;
    }

    public static class GetMenuItemCallBack extends XC_MethodHook {

        public GetMenuItemCallBack() {
            super(60);
        }

        @Override
        protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
            if (LicenseStatus.sDisableCommonHooks) {
                return;
            }
            if (!INSTANCE.isEnabled()) {
                return;
            }
            try {
                Object arr = param.getResult();
                Class<?> clQQCustomMenuItem = arr.getClass().getComponentType();
                Object item_copy = CustomMenu.createItem(clQQCustomMenuItem, R.id.item_showPicMd5, "MD5");
                Object ret = Array.newInstance(clQQCustomMenuItem, Array.getLength(arr) + 1);
                //noinspection SuspiciousSystemArraycopy
                System.arraycopy(arr, 0, ret, 0, Array.getLength(arr));
                Array.set(ret, Array.getLength(arr), item_copy);
                param.setResult(ret);
            } catch (Throwable e) {
                INSTANCE.traceError(e);
                throw e;
            }
        }
    }

    public static class MenuItemClickCallback extends XC_MethodHook {

        public MenuItemClickCallback() {
            super(60);
        }

        @Override
        protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
            int id = (int) param.args[0];
            final Activity ctx = (Activity) param.args[1];
            final Object chatMessage = param.args[2];
            if (id == R.id.item_showPicMd5) {
                param.setResult(null);
                try {
                    final String md5;
                    if (chatMessage == null
                            || (md5 = (String) Reflex.getInstanceObjectOrNull(chatMessage, "md5")) == null
                            || md5.length() == 0) {
                        Toasts.error(ctx, "获取图片MD5失败");
                        return;
                    }
                    CustomDialog.createFailsafe(ctx).setTitle("MD5").setCancelable(true)
                            .setMessage(md5).setPositiveButton("复制",
                                    (dialog, which) -> SystemServiceUtils.copyToClipboard(ctx, md5))
                            .setNeutralButton("复制图片链接",
                                    (dialog, which) -> SystemServiceUtils.copyToClipboard(ctx, getPicturePath(md5)))
                            .setNegativeButton("关闭", null).show();
                } catch (Throwable e) {
                    INSTANCE.traceError(e);
                    Toasts.error(ctx, e.toString().replace("java.lang.", ""));
                }
            }
        }
    }

    private static String getPicturePath(@NonNull String md5) {
        return "https://gchat.qpic.cn/gchatpic_new/0/0-0-" + md5 + "/0";
    }
}
