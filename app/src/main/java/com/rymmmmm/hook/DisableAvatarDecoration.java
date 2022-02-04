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
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import java.lang.reflect.Method;

//屏蔽头像挂件
@FunctionHookEntry
@UiItemAgentEntry
public class DisableAvatarDecoration extends CommonSwitchFunctionHook {

    public static final DisableAvatarDecoration INSTANCE = new DisableAvatarDecoration();

    protected DisableAvatarDecoration() {
        super("rq_disable_avatar_decoration");
    }

    @NonNull
    @Override
    public String getName() {
        return "屏蔽头像挂件";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Simplify.UI_PROFILE;
    }

    @Override
    public boolean initOnce() {
        for (Method m : Initiator.load("com.tencent.mobileqq.vas.PendantInfo").getDeclaredMethods()) {
            if (m.getReturnType() == void.class) {
                Class<?>[] argt = m.getParameterTypes();
                if (argt.length != 5) {
                    continue;
                }
                if (argt[0] != View.class) {
                    continue;
                }
                if (argt[1] != int.class) {
                    continue;
                }
                if (argt[2] != long.class) {
                    continue;
                }
                if (argt[3] != String.class) {
                    continue;
                }
                if (argt[4] != int.class) {
                    continue;
                }
                HookUtils.hookBeforeIfEnabled(this, m, param -> param.setResult(null));
            }
        }
        return true;
    }
}
