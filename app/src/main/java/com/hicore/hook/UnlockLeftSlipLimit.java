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

import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import com.hicore.ReflectUtil.MMethod;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.QQVersion;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import io.github.qauxv.util.dexkit.NLeftSwipeReplyHelper_reply;
import java.lang.reflect.Method;

@FunctionHookEntry
@UiItemAgentEntry
public class UnlockLeftSlipLimit extends CommonSwitchFunctionHook {

    public static final UnlockLeftSlipLimit INSTANCE = new UnlockLeftSlipLimit();
    public static final String methodName = HostInfo.requireMinQQVersion(QQVersion.QQ_8_8_93) ? "H" : "h";
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
        Method m = MMethod.FindMethod(DexKit.INSTANCE.requireMethodFromCache(NLeftSwipeReplyHelper_reply.INSTANCE).getDeclaringClass(), methodName, boolean.class, new Class[0]);
        HookUtils.hookBeforeIfEnabled(this, m, param -> param.setResult(true));
        return true;
    }

    @NonNull
    @Override
    public String getName() {
        return "允许各种消息左滑回复";
    }
}
