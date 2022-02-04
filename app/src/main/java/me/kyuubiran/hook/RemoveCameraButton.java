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
package me.kyuubiran.hook;

import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import java.lang.reflect.Method;

//屏蔽聊天界面相机图标
@FunctionHookEntry
@UiItemAgentEntry
public class RemoveCameraButton extends CommonSwitchFunctionHook {

    private RemoveCameraButton() {
        super("kr_disable_camera_button");
    }

    public static final RemoveCameraButton INSTANCE = new RemoveCameraButton();

    @Override
    public boolean initOnce() throws Exception {
        for (Method m : Initiator._ConversationTitleBtnCtrl().getDeclaredMethods()) {
            Class<?>[] argt = m.getParameterTypes();
            if ("a".equals(m.getName()) && m.getReturnType() == void.class
                && argt.length == 0) {
                HookUtils.hookBeforeIfEnabled(this, m, param -> param.setResult(null));
            }
        }
        return true;
    }

    @NonNull
    @Override
    public String getName() {
        return "屏蔽聊天界面相机图标";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Simplify.CHAT_GROUP_OTHER;
    }
}
