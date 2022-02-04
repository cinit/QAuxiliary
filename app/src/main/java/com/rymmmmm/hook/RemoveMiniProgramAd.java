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

import static cc.ioctl.util.HostInfo.requireMinQQVersion;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.Reflex;
import io.github.qauxv.SyncUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.QQVersion;
import java.lang.reflect.Method;

//去除小程序广告 需要手动点关闭
@FunctionHookEntry
@UiItemAgentEntry
public class RemoveMiniProgramAd extends CommonSwitchFunctionHook {

    @NonNull
    @Override
    public String getName() {
        return "屏蔽小程序广告";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "需要手动关闭广告, 请勿反馈此功能无效";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.EXPERIMENTAL_CATEGORY;
    }

    public static final RemoveMiniProgramAd INSTANCE = new RemoveMiniProgramAd();

    protected RemoveMiniProgramAd() {
        super(SyncUtils.PROC_ANY & ~(SyncUtils.PROC_MAIN | SyncUtils.PROC_MSF | SyncUtils.PROC_QZONE
            | SyncUtils.PROC_PEAK | SyncUtils.PROC_VIDEO));
    }

    @Override
    public boolean initOnce() {
        for (Method m : Initiator._GdtMvViewController().getDeclaredMethods()) {
            Class<?>[] argt = m.getParameterTypes();
            if (m.getName().equals("x") && argt.length == 0) {
                HookUtils.hookBeforeIfEnabled(this, m, param -> {
                    Reflex.setInstanceObject(param.thisObject, "c", Boolean.TYPE, true);
                    if (requireMinQQVersion(QQVersion.QQ_8_4_1)) {
                        Reflex.invokeVirtual(param.thisObject, "e");
                    }
                });
            }
        }
        return true;
    }
}
