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

package com.hicore.hook;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.Reflex;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.QQVersion;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

@FunctionHookEntry
@UiItemAgentEntry
public class UploadTransparentAvatar extends CommonSwitchFunctionHook {

    public static final UploadTransparentAvatar INSTANCE = new UploadTransparentAvatar();

    private UploadTransparentAvatar() {
    }

    @NonNull
    @Override
    public String getName() {
        return "允许上传透明头像和表情";
    }

    @Override
    public CharSequence getDescription() {
        return "若上传不成功可尝试开启非会员贴表情";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY;
    }

    @Override
    protected boolean initOnce() throws Exception {
        XposedHelpers.findAndHookMethod(Bitmap.class, "compress", Bitmap.CompressFormat.class, int.class, OutputStream.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                // 判断为QQ上传头像处调用才拦截
                // TroopUploadingThread
                String currentCallStacks = getCurrentCallStacks();
                if (currentCallStacks.contains("NearbyPeoplePhotoUploadProcessor")
                        || currentCallStacks.contains("doInBackground")
                        || currentCallStacks.contains("TroopUploadingThread")) {
                    param.args[0] = Bitmap.CompressFormat.PNG;
                }
            }
        });
        var methodName = HostInfo.requireMinQQVersion(QQVersion.QQ_8_8_93) ? "i" : "a";
        var clazzName = HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_0) ? "com.tencent.mobileqq.pic.compress.e" : "com.tencent.mobileqq.pic.compress.Utils";
        Method hookMethod = Reflex.findMethod(Initiator.loadClass(clazzName),
                boolean.class, methodName, String.class, Bitmap.class, int.class, String.class,
                Initiator.loadClass("com.tencent.mobileqq.pic.CompressInfo"));
        HookUtils.hookBeforeAlways(this, hookMethod, param -> {
            // 自己进行图像转换, 不给QQ把透明背景扣掉的机会
            FileOutputStream fos = new FileOutputStream((String) param.args[0]);
            Bitmap bitmap = (Bitmap) param.args[1];
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            param.setResult(true);
        });

        Method med = Reflex.findMethod(Initiator.loadClass("com.tencent.mobileqq.vip.VipStatusManagerImpl"),
                int.class, "getPrivilegeFlags", String.class);
        HookUtils.hookAfterAlways(this, med, param -> {
            int i = (int) param.getResult();
            param.setResult(i | 2 | 4 | 8);
        });
        return true;
    }

    public static String getCurrentCallStacks() {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        StringBuilder builder = new StringBuilder();
        for (StackTraceElement ele : elements) {
            builder.append(ele.getClassName()).append(".")
                    .append(ele.getMethodName()).append("() (")
                    .append(ele.getFileName()).append(":")
                    .append(ele.getLineNumber()).append(")")
                    .append("\n");
        }
        return builder.toString();
    }
}
