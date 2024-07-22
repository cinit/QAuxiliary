/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2024 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is an opensource software: you can redistribute it
 * and/or modify it under the terms of the General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the General Public License for more details.
 *
 * You should have received a copy of the General Public License
 * along with this software.
 * If not, see
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package io.github.qauxv.loader.sbl.xp51;

import androidx.annotation.Keep;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Field;

public class Xp51HookStatusInit {

    private Xp51HookStatusInit() {
    }

    @Keep
    public static void init(ClassLoader classLoader) throws ReflectiveOperationException {
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
