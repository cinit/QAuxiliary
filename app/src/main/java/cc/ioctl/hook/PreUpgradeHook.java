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

import static io.github.qauxv.util.Initiator._UpgradeController;

import androidx.annotation.NonNull;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@FunctionHookEntry
@UiItemAgentEntry
public class PreUpgradeHook extends CommonSwitchFunctionHook {

    @NonNull
    @Override
    public String getName() {
        return "屏蔽更新提醒";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Simplify.UI_MISC;
    }

    public static final PreUpgradeHook INSTANCE = new PreUpgradeHook();

    private PreUpgradeHook() {
    }

    @Override
    public boolean initOnce() throws Exception {
        for (Method m : _UpgradeController().getDeclaredMethods()) {
            if (m.getParameterTypes().length != 0) {
                continue;
            }
            if (Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            if (!m.getName().equals("a")) {
                continue;
            }
            if (m.getReturnType().getName().contains("UpgradeDetailWrapper")) {
                XposedBridge.hookMethod(m, new XC_MethodHook(43) {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        param.setResult(null);
                    }
                });
                break;
            }
        }
        return true;
    }
}
