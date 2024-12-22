/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2024 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is an opensource software: you can redistribute it
 * and/or modify it under the terms of the General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the General Public License for more details.
 *
 * You should have received a copy of the General Public License
 * along with this software.
 * If not, see
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package cc.taffy.hook;

import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import java.lang.reflect.Method;

@FunctionHookEntry
@UiItemAgentEntry
public final class FakeGuildUserType extends CommonSwitchFunctionHook {

    public static final FakeGuildUserType INSTANCE = new FakeGuildUserType();
    @NonNull
    @Override
    public String getName() {
        return "假装自己是频道主";
    }

    @Override
    public String getDescription() {
        return "开启后可以查看频道人数，但将无法退出频道(只显示解散频道,但点了也没用)。";
    }

    @Override
    protected boolean initOnce() throws Exception {

        Class<?> clazz = Initiator.loadClass("com.tencent.mobileqq.qqguildsdk.data.GProGuildInfo");

        Method getUserTypeMethod = clazz.getDeclaredMethod("getUserType");

        //本来想做成选择身份的那种 CommonConfigFunctionHook，但能力有限，先这样凑合吧。
        //1是频道内超管，2是频道主
        HookUtils.hookAfterIfEnabled(this, getUserTypeMethod, param -> param.setResult(2));
        return true;
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Auxiliary.GUILD_CATEGORY;
    }
}
