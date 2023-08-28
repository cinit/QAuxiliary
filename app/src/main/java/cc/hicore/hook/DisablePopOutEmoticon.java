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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the qwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package cc.hicore.hook;

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
                Reflex.findSingleMethod(Objects.requireNonNull(DexKit.loadClassFromCache(CPopOutEmoticonUtil.INSTANCE), "C_PopOutEmoticonUtil"),
                        boolean.class, false,
                        int.class, Initiator.loadClass("com.tencent.mobileqq.emoticonview.EmoticonInfo"), int.class),
                param -> param.setResult(false));
        return true;
    }
}
