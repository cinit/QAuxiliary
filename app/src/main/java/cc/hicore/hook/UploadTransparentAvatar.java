/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the qwq233 Universal License
 * as published on https://github.com/qwq233/license; either
 * version 2 of the License, or any later version and our EULA as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the qwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package cc.hicore.hook;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.Reflex;
import io.github.qauxv.util.xpcompat.XC_MethodHook;
import io.github.qauxv.util.xpcompat.XposedHelpers;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.QQVersion;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import io.github.qauxv.util.dexkit.NVipUtils_getPrivilegeFlags;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Objects;

@FunctionHookEntry
@UiItemAgentEntry
public class UploadTransparentAvatar extends CommonSwitchFunctionHook {

    public static final UploadTransparentAvatar INSTANCE = new UploadTransparentAvatar();

    private UploadTransparentAvatar() {
        super(new DexKitTarget[]{NVipUtils_getPrivilegeFlags.INSTANCE});
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
        if (HostInfo.requireMinQQVersion(QQVersion.QQ_9_1_50)){
            clazzName = "com.tencent.mobileqq.pic.compress.g";
        }
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

        Method med = Objects.requireNonNull(DexKit.loadMethodFromCache(NVipUtils_getPrivilegeFlags.INSTANCE), "VipStatusManagerImpl");
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
