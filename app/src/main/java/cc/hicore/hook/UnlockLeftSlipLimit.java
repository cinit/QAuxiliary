/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
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

package cc.hicore.hook;

import androidx.annotation.NonNull;
import cc.hicore.ReflectUtil.MMethod;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.QQVersion;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import io.github.qauxv.util.dexkit.NLeftSwipeReplyHelper_reply;
import java.lang.reflect.Method;

@FunctionHookEntry
@UiItemAgentEntry
public class UnlockLeftSlipLimit extends CommonSwitchFunctionHook {

    public static final UnlockLeftSlipLimit INSTANCE = new UnlockLeftSlipLimit();
    public static final String methodName =
            !HostInfo.requireMinQQVersion(QQVersion.QQ_8_8_93) ? "h" : HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_33) ? "I" : "H";

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
        if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_63)) {
            XposedHelpers.findAndHookMethod(Initiator.loadClass("com.tencent.mobileqq.ark.api.impl.ArkHelperImpl"), "isSupportReply", String.class,
                    String.class, String.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            param.setResult(true);
                        }
                    });
            return true;
        }
        Method m = MMethod.FindMethod(DexKit.requireMethodFromCache(NLeftSwipeReplyHelper_reply.INSTANCE).getDeclaringClass(), methodName, boolean.class,
                new Class[0]);
        HookUtils.hookBeforeIfEnabled(this, m, param -> param.setResult(true));
        return true;
    }

    @NonNull
    @Override
    public String getName() {
        return "允许各种消息左滑回复";
    }
}
