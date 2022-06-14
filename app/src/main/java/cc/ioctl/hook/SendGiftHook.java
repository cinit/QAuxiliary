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
package cc.ioctl.hook;

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.Reflex;
import io.github.qauxv.SyncUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.DexKit;
import io.github.qauxv.util.Initiator;
import java.lang.reflect.Method;
import java.util.Objects;

@FunctionHookEntry
@UiItemAgentEntry
public class SendGiftHook extends CommonSwitchFunctionHook {

    @NonNull
    @Override
    public String getName() {
        return "禁用$打开送礼界面";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "禁止聊天时输入$自动弹出[选择赠送对象]窗口";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Simplify.CHAT_OTHER;
    }

    public static final SendGiftHook INSTANCE = new SendGiftHook();

    private SendGiftHook() {
        super(SyncUtils.PROC_MAIN, new int[]{DexKit.C_TROOP_GIFT_UTIL});
    }

    @Override
    public boolean initOnce() throws Exception {
        Method m = Reflex.findSingleMethod(Objects.requireNonNull(DexKit.loadClassFromCache(DexKit.C_TROOP_GIFT_UTIL), "DexKit.C_TROOP_GIFT_UTIL"),
                void.class, false,
                Activity.class, String.class, String.class, Initiator._QQAppInterface());
        HookUtils.hookBeforeIfEnabled(this, m, 47, param -> param.setResult(null));
        return true;
    }
}
