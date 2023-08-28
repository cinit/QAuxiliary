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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See theqwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package cc.hicore.hook;

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
