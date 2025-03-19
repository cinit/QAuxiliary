/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2025 QAuxiliary developers
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

package io.github.ex3124.hook;

import android.content.Intent;
import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.QQVersion;
import io.github.qauxv.util.HostInfo;
import java.lang.reflect.Method;

@FunctionHookEntry
@UiItemAgentEntry
public final class BlockBusCard extends CommonSwitchFunctionHook {

    public static final BlockBusCard INSTANCE = new BlockBusCard();

    @NonNull
    @Override
    public String getName() {
        return "禁用QQ公交卡";
    }

    @Override
    public String getDescription() {
        return "禁止QQ在后台干扰NFC";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Auxiliary.MISC_CATEGORY;
    }

    @Override
    public boolean isAvailable() {
        return HostInfo.requireMaxQQVersion(QQVersion.QQ_8_4_10);
    }

    @Override
    public boolean initOnce() throws Exception {
        Class<?> helper = Initiator.loadClass("cooperation.buscard.BuscardHelper");
        for (Method m : helper.getDeclaredMethods()) {
            for (Class<?> parameterType : m.getParameterTypes()) {
                if (parameterType.equals(Intent.class))
                    HookUtils.hookBeforeIfEnabled(this, m, param -> param.setResult(null));
            }
        }
        return true;
    }
}
