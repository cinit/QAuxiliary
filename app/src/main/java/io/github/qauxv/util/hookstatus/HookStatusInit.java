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

package io.github.qauxv.util.hookstatus;

import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Field;

/**
 * Called in handleLoadPackage, NO KOTLIN, NO ANDROIDX
 **/
public class HookStatusInit {

    private HookStatusInit() {
    }

    public static void init(ClassLoader classLoader) throws Throwable {
        Class<?> kHookStatusImpl = classLoader.loadClass("io.github.qauxv.util.hookstatus.HookStatusImpl");
        Field f = kHookStatusImpl.getDeclaredField("sZygoteHookMode");
        f.setAccessible(true);
        f.set(null, true);
        boolean dexObfsEnabled = !"de.robv.android.xposed.XposedBridge".equals(XposedBridge.class.getName());
        String hookProvider = null;
        if (dexObfsEnabled) {
            f = kHookStatusImpl.getDeclaredField("sIsLsposedDexObfsEnabled");
            f.setAccessible(true);
            f.set(null, true);
            hookProvider = "LSPosed";
        } else {
            String bridgeTag = null;
            try {
                bridgeTag = (String) XposedBridge.class.getDeclaredField("TAG").get(null);
            } catch (ReflectiveOperationException ignored) {
            }
            if (bridgeTag != null) {
                if (bridgeTag.startsWith("LSPosed")) {
                    hookProvider = "LSPosed";
                } else if (bridgeTag.startsWith("EdXposed")) {
                    hookProvider = "EdXposed";
                }
            }
        }
        if (hookProvider != null) {
            f = kHookStatusImpl.getDeclaredField("sZygoteHookProvider");
            f.setAccessible(true);
            f.set(null, hookProvider);
        }
    }
}
