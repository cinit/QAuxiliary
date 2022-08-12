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

import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Objects;

//强制使用默认字体
@FunctionHookEntry
@UiItemAgentEntry
public class DefaultFont extends CommonSwitchFunctionHook {

    public static final DefaultFont INSTANCE = new DefaultFont();

    protected DefaultFont() {
        super("rq_default_font");
    }

    @NonNull
    @Override
    public String getName() {
        return "强制使用默认字体";
    }

    @NonNull
    @Override
    public String getDescription() {
        return "禁用特殊字体, 以及大字体";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Simplify.UI_CHAT_MSG;
    }

    @Override
    public boolean initOnce() throws ReflectiveOperationException {
        // protected (BaseBubbleBuilder, TextItemBuilder).void ?(BaseBubbleBuilder.ViewHolder, ChatMessage)
        ArrayList<Method> candidates = new ArrayList<>(2);
        for (Method m : Initiator._TextItemBuilder().getDeclaredMethods()) {
            if (m.getModifiers() == Modifier.PROTECTED && m.getReturnType() == void.class) {
                Class<?>[] argt = m.getParameterTypes();
                if (argt.length == 2 && argt[0] != View.class && argt[1] == Initiator._ChatMessage()) {
                    candidates.add(m);
                }
            }
        }
        if (candidates.size() != 2) {
            throw new RuntimeException("expect 2 methods, got " + candidates.size());
        }
        // 8.8.88 a
        // 8.8.93 q0
        // 8.9.0 q0
        // 8.9.2 q0
        // 8.9.3 p0
        Method method = null;
        for (Method m : candidates) {
            String name = m.getName();
            if ("a".equals(name) || "p0".equals(name) || "q0".equals(name)) {
                method = m;
                break;
            }
        }
        Objects.requireNonNull(method);
        HookUtils.hookBeforeIfEnabled(this, method, param -> param.setResult(null));
        // m.getName().equals(HostInfo.requireMinQQVersion(QQVersion.QQ_8_8_93) ? "q0" : "a") &&
        Method enlargeTextMsg = Initiator.loadClass("com.tencent.mobileqq.vas.font.api.impl.FontManagerServiceImpl")
                .getDeclaredMethod("enlargeTextMsg", TextView.class);
        HookUtils.hookBeforeIfEnabled(this, enlargeTextMsg, param -> param.setResult(null));
        return true;
    }
}
