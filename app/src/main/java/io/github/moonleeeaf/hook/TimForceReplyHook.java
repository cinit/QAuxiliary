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

package com.example.hook;

import cc.ioctl.util.hookBeforeIfEnabled;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.util.QQVersion;
import io.github.qauxv.util.requireMinQQVersion;
import java.lang.reflect.Method;

// FunctionHookEntry 和 UiItemAgentEntry 用于注册功能，这两个注解都是必要的
@FunctionHookEntry
@UiItemAgentEntry
public final class TimForceReplyHook extends CommonSwitchFunctionHook {

    // INSTANCE 是必须的，因为 Java 没有 object，所以我们需要一个单例
    public static final TimForceReplyHook INSTANCE = new TimForceReplyHook();

    @Override
    public String getName() {
        return "TIM 强制允许回复消息";
    }

    @Override
    public String getDescription() {
        return "仅供 3.0.0 版本使用，未经测试";
    }

    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY;
    }

    @Override
    public boolean isAvailable() {
        return HostInfo.isTim();
    }

    @Override
    public boolean initOnce() throws Exception {
        Class<?> baseBubbleBuilderClass = Initiator.loadClass("com.tencent.mobileqq.activity.aio.BaseBubbleBuilder");
        Method a = baseBubbleBuilderClass.getDeclaredMethod("a", Initiator.loadClass('com.tencent.mobileqq.data.ChatMessage'), Initiator.loadClass('com.tencent.mobileqq.utils.dialogutils.QQCustomMenu'));
        HookUtils.hookBeforeIfEnabled(this, a, param -> {
            // Lcom/tencent/mobileqq/utils/dialogutils/QQCustomMenu;
            Object qqCustomMenu = param.args[0];
            Method f = qqCustomMenu.getClass().getDeclaredMethod('F', int.class, String.class, int.class);
            f.setAccessible(true);
            // 暂时先硬编码了
            // 字符串Resource资源ID： 2131631067 即 7f0e1bdb
            // 可以同thisObject再反射，但因为在网页编辑不便测试，故暂时省去
            f.invoke(qqCustomMenu, 2131238756, "回复", 2131165801);
            // 后面我再看看这样能不能阻断代码执行，也许可以
            param.setResult(null);
        });
        return true;
    }
}
