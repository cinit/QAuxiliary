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
package cc.ioctl.hook.chat;

import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.QQVersion;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import io.github.qauxv.util.dexkit.TroopEnterEffect_QQNT;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

//屏蔽所有进场特效
@FunctionHookEntry
@UiItemAgentEntry
public class DisableEnterEffect extends CommonSwitchFunctionHook {

    public static final DisableEnterEffect INSTANCE = new DisableEnterEffect();

    protected DisableEnterEffect() {
        super("rq_disable_enter_effect", new DexKitTarget[]{TroopEnterEffect_QQNT.INSTANCE});
    }

    @NonNull
    @Override
    public String getName() {
        return "屏蔽所有进场特效";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Simplify.CHAT_DECORATION;
    }

    @Override
    public boolean isAvailable() {
        return !HostInfo.isTim();
    }

    @Override
    public boolean initOnce() {
        if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_63_BETA_11345)) {
            Method m = DexKit.requireMethodFromCache(TroopEnterEffect_QQNT.INSTANCE);
            HookUtils.hookBeforeIfEnabled(this, m, param -> param.setResult(null));
            return true;
        }
        for (Method m : Initiator._TroopEnterEffectController().getDeclaredMethods()) {
            if ((m.getName().equals("a") || m.getName().equals("l")) && !Modifier.isStatic(m.getModifiers())
                    && m.getParameterTypes().length == 0 && m.getReturnType() == void.class) {
                HookUtils.hookBeforeIfEnabled(this, m, param -> param.setResult(null));
                return true;
            }
        }
        return false;
    }
}
