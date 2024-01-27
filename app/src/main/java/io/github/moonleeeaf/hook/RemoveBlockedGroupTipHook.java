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
package io.github.moonleeeaf.hook;

import cc.ioctl.util.hookBeforeIfEnabled;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.QQVersion;
import io.github.qauxv.util.requireMinQQVersion;

@FunctionHookEntry
@UiItemAgentEntry
public final class RemoveBlockedGroupTipHook extends CommonSwitchFunctionHook {

    public static final RemoveBlockedGroupTipHook INSTANCE = new RemoveBlockedGroupTipHook();

    @Override
    public String getName() {
        return "移除解除屏蔽群聊提示";
    }

    @Override
    public String getDescription() {
        return "打开已经屏蔽消息的群聊时，移除\"实时接收消息或移入群助手\"提示，未经测试";
    }

    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Simplify.CHAT_OTHER;
    }

    @Override
    public boolean initOnce() throws Exception {
        Class<?> klass = Initiator.loadClass("com.tencent.mobileqq.activity.aio.rebuild.TroopChatPie$8$1");
        Method someMethod = klass.getDeclaredMethod("run");
        HookUtils.hookBeforeIfEnabled(this, someMethod, param -> {});
        return true;
    }
}