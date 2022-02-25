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
package com.rymmmmm.hook;

import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

//屏蔽群聊炫彩昵称
@FunctionHookEntry
@UiItemAgentEntry
public class DisableColorNickName extends CommonSwitchFunctionHook {

    public static final DisableColorNickName INSTANCE = new DisableColorNickName();

    protected DisableColorNickName() {
        super("rq_disable_color_nick_name");
    }

    @NonNull
    @Override
    public String getName() {
        return "屏蔽群聊炫彩昵称";
    }

    @NonNull
    @Override
    public String getDescription() {
        return "可能导致聊天页面滑动卡顿";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Simplify.CHAT_GROUP_TITLE;
    }

    @Override
    public boolean initOnce() {
        for (Method m : Initiator._ColorNickManager().getDeclaredMethods()) {
            Class<?>[] argt = m.getParameterTypes();
            if (m.getName().equals("a") && Modifier.isStatic(m.getModifiers())
                && m.getReturnType() == void.class && argt.length == 3) {
                HookUtils.hookBeforeIfEnabled(this, m, param -> param.setResult(null));
            }
        }
        return true;
    }
}
