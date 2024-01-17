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

package top.linl.hook;

import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import top.linl.util.reflect.FieldUtils;
import top.linl.util.reflect.MethodTool;

@FunctionHookEntry
@UiItemAgentEntry
public class TurnOffFriendInteractionLogoView extends CommonSwitchFunctionHook {

    public static final TurnOffFriendInteractionLogoView INSTANCE = new TurnOffFriendInteractionLogoView();

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Simplify.UI_PROFILE;
    }

    @Override
    protected boolean initOnce() throws Exception {
        Method initViewMethod = MethodTool.find("com.tencent.mobileqq.profilecard.component.ProfileIntimateComponent")
                .name("initComponentViewContainer")
                .returnType(void.class)
                .get();
        HookUtils.hookBeforeIfEnabled(this, initViewMethod, param -> {
            Field mViewContainerField = FieldUtils.findUnknownTypeField(param.thisObject.getClass(), "mViewContainer");
            mViewContainerField.set(param.thisObject, 1);
            param.setResult(null);
        });
        return true;
    }

    @NonNull
    @Override
    public String getName() {
        return "关闭好友互动标识";
    }
}
