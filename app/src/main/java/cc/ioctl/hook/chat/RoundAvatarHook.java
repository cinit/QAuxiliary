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

import static io.github.qauxv.util.QQVersion.QQ_8_8_11;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.util.xpcompat.XC_MethodHook;
import io.github.qauxv.util.xpcompat.XposedBridge;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.dexkit.CSimpleUiUtil;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import java.lang.reflect.Method;

@FunctionHookEntry
@UiItemAgentEntry
public class RoundAvatarHook extends CommonSwitchFunctionHook {

    public static final RoundAvatarHook INSTANCE = new RoundAvatarHook();

    private RoundAvatarHook() {
        super(new DexKitTarget[]{CSimpleUiUtil.INSTANCE});
    }

    @NonNull
    @Override
    public String getName() {
        return "简洁模式圆头像";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "仅支持 QQ 8.8.11 以下";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.CHAT_CATEGORY;
    }

    @Override
    public boolean initOnce() {
        Method a = null, b = null;
        Class<?> clz = DexKit.requireClassFromCache(CSimpleUiUtil.INSTANCE);
        for (Method m : clz.getDeclaredMethods()) {
            if (!boolean.class.equals(m.getReturnType())) {
                continue;
            }
            Class[] argt = m.getParameterTypes();
            if (argt.length != 1) {
                continue;
            }
            if (String.class.equals(argt[0])) {
                if (m.getName().equals("a")) {
                    a = m;
                }
                if (m.getName().equals("b")) {
                    b = m;
                }
            }
        }
        XC_MethodHook hook = new XC_MethodHook(43) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!isEnabled()) {
                    return;
                }
                param.setResult(false);
            }
        };
        if (b != null) {
            XposedBridge.hookMethod(b, hook);
        } else {
            XposedBridge.hookMethod(a, hook);
        }
        return true;
    }

    @Override
    public boolean isAvailable() {
        return !HostInfo.isTim() && HostInfo.getVersionCode() < QQ_8_8_11;
    }
}
