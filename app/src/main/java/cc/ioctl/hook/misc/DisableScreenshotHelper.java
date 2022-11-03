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
package cc.ioctl.hook.misc;

import android.content.Context;
import android.os.Handler;
import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.dexkit.CScreenShotHelper;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

//屏蔽截屏分享
@FunctionHookEntry
@UiItemAgentEntry
public class DisableScreenshotHelper extends CommonSwitchFunctionHook {

    public static final DisableScreenshotHelper INSTANCE = new DisableScreenshotHelper();

    @NonNull
    @Override
    public String getName() {
        return "屏蔽截屏分享";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Simplify.UI_MISC;
    }

    private DisableScreenshotHelper() {
        super(new DexKitTarget[]{CScreenShotHelper.INSTANCE});
    }

    @Override
    public boolean initOnce() throws Exception {
        for (Method m : DexKit.requireClassFromCache(CScreenShotHelper.INSTANCE).getDeclaredMethods()) {
            if (m.getName().equals("a") && Modifier.isStatic(m.getModifiers())
                && m.getReturnType() == void.class) {
                Class<?>[] argt = m.getParameterTypes();
                if (argt.length == 3 && argt[0] == Context.class && argt[1] == String.class
                    && argt[2] == Handler.class) {
                    HookUtils.hookBeforeIfEnabled(this, m, param -> param.setResult(null));
                }
            }
        }
        return true;
    }
}
