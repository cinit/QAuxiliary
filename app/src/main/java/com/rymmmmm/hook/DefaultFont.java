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
import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.QQVersion;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

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
    public String[] getUiItemLocation() {
        return Simplify.UI_CHAT_MSG;
    }

    @Override
    public boolean initOnce() {
        for (Method m : Initiator._TextItemBuilder().getDeclaredMethods()) {
            if (m.getName().equals(HostInfo.requireMinPlayQQVersion(QQVersion.QQ_8_8_93) ? "m0" : "a")
                    && !Modifier.isStatic(m.getModifiers())
                    && m.getReturnType() == void.class) {
                Class<?>[] argt = m.getParameterTypes();
                if (argt.length == 2 && argt[0] != View.class && argt[1] == Initiator._ChatMessage()) {
                    HookUtils.hookBeforeIfEnabled(this, m, param -> param.setResult(null));
                }
            }
        }
        return true;
    }
}
