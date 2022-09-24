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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.Reflex;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.dexkit.CPopOutEmoticonUtil;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import java.util.Objects;

@FunctionHookEntry
@UiItemAgentEntry
public class DisablePopOutEmoticon extends CommonSwitchFunctionHook {

    public static final DisablePopOutEmoticon INSTANCE = new DisablePopOutEmoticon();

    private DisablePopOutEmoticon() {
        super(new DexKitTarget[]{CPopOutEmoticonUtil.INSTANCE});
    }

    @NonNull
    @Override
    public String getName() {
        return "禁止弹射表情";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "去除好友界面长按小表情发送弹射表情";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Simplify.CHAT_EMOTICON;
    }

    @Override
    protected boolean initOnce() throws Exception {
        HookUtils.hookBeforeIfEnabled(this,
                Reflex.findSingleMethod(Objects.requireNonNull(DexKit.INSTANCE.loadClassFromCache(CPopOutEmoticonUtil.INSTANCE), "C_PopOutEmoticonUtil"),
                        boolean.class, false,
                        int.class, Initiator.loadClass("com.tencent.mobileqq.emoticonview.EmoticonInfo"), int.class),
                param -> param.setResult(false));
        return true;
    }
}
