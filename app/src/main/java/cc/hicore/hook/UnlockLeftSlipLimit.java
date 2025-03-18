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

import androidx.annotation.NonNull;
import cc.hicore.ReflectUtil.XMethod;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.QQVersion;
import io.github.qauxv.util.TIMVersion;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import io.github.qauxv.util.dexkit.NLeftSwipeReplyHelper_reply;
import io.github.qauxv.util.xpcompat.XC_MethodHook;
import io.github.qauxv.util.xpcompat.XposedHelpers;
import java.lang.reflect.Method;
import io.github.qauxv.util.PlayQQVersion;

@FunctionHookEntry
@UiItemAgentEntry
public class UnlockLeftSlipLimit extends CommonSwitchFunctionHook {

    public static final UnlockLeftSlipLimit INSTANCE = new UnlockLeftSlipLimit();

    private UnlockLeftSlipLimit() {
        super(new DexKitTarget[]{NLeftSwipeReplyHelper_reply.INSTANCE});
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY;
    }

    @Override
    protected boolean initOnce() throws Exception {
        if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_63_BETA_11345) || HostInfo.requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA)) {
            XposedHelpers.findAndHookMethod(
                    Initiator.loadClass("com.tencent.mobileqq.ark.api.impl.ArkHelperImpl"),
                    "isSupportReply",
                    String.class, String.class, String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            param.setResult(true);
                        }
                    }
            );
            return true;
        }
        Method m = XMethod.clz(DexKit.requireMethodFromCache(NLeftSwipeReplyHelper_reply.INSTANCE).getDeclaringClass())
                .name(io.github.qauxv.util.HostInfo.requireRangePlayQQVersion(PlayQQVersion.PlayQQ_8_2_11, PlayQQVersion.PlayQQ_8_2_11) ? "c" : !HostInfo.requireMinQQVersion(QQVersion.QQ_8_8_93) ? "h" : HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_33) ? "I" : "H")
                .ret(boolean.class)
                .get();
        HookUtils.hookBeforeIfEnabled(this, m, param -> param.setResult(true));
        return true;
    }

    @NonNull
    @Override
    public String getName() {
        return "允许各种消息左滑回复";
    }
}
