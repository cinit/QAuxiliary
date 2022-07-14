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

package com.hicore.hook;

import static io.github.qauxv.util.HostInfo.requireMinQQVersion;
import static io.github.qauxv.util.QQVersion.QQ_8_9_0;

import android.text.Spanned;
import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;

@FunctionHookEntry
@UiItemAgentEntry
public class UnlockTroopNameLimit extends CommonSwitchFunctionHook {

    public static final UnlockTroopNameLimit INSTANCE = new UnlockTroopNameLimit();

    private UnlockTroopNameLimit() {
    }

    @NonNull
    @Override
    public String getName() {
        return "允许群名带表情";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY;
    }

    @Override
    protected boolean initOnce() throws Exception {
        var clazzName = requireMinQQVersion(QQ_8_9_0) ? "n" : "EmojiFilter";
        HookUtils.hookBeforeIfEnabled(this,
                Initiator.loadClass("com.tencent.mobileqq.activity.editservice.EditTroopMemberNickService$" + clazzName)
                        .getDeclaredMethod("filter", CharSequence.class, int.class, int.class, Spanned.class, int.class, int.class),
                param -> param.setResult(null));
        return true;
    }
}
