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

import android.util.JsonReader;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HostInfo;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import io.github.qauxv.util.QQVersion;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import me.singleneuron.tlb.ConfigTable;

/**
 * Not an important hook. Provide limited anti-crash feature for VasProfileCard, esp DIY card.
 */
@FunctionHookEntry
public class VasProfileAntiCrash extends CommonSwitchFunctionHook {

    public static final VasProfileAntiCrash INSTANCE = new VasProfileAntiCrash();

    private VasProfileAntiCrash() {
        super();
    }

    @NonNull
    @Override
    public String getName() {
        return "资料卡防崩溃";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "现在可能没啥用";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.PROFILE_CATEGORY;
    }

    @Override
    public boolean initOnce() throws Exception {
        String className = null;
        try {
            className = ConfigTable.INSTANCE.getConfig(VasProfileAntiCrash.class.getSimpleName());
        } catch (Exception e) {
            traceError(e);
        }
        doHook(className);
        return true;
    }

    private void doHook(String className) {
        try {
            XposedBridge.hookAllMethods(JsonReader.class, "nextLong", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!param.hasThrowable()) {
                        return;
                    }
                    if (!Log.getStackTraceString(param.getThrowable()).contains("FriendProfileCardActivity")) {
                        return;
                    }
                    param.setResult(0L);
                }
            });
        } catch (Exception e) {
            //ignore
        }
        if (className == null) {
            return;
        }
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (param.hasThrowable()) {
                    param.setResult(0L);
                }
            }
        };
        Class<?> Card = Initiator.load("com.tencent.mobileqq.data.Card");
        if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_6_0)) {
            XposedHelpers.findAndHookMethod(
                    Initiator.load(className),
                    "getDiyTemplateVersion", Card, hook);
            return;
        }
        for (Method m : Initiator.load(className).getDeclaredMethods()) {
            Class<?>[] argt;
            if (Modifier.isStatic(m.getModifiers()) && m.getName().equals("a")
                    && m.getReturnType() == long.class && (argt = m.getParameterTypes()).length == 1
                    && argt[0] == Card) {
                XposedBridge.hookMethod(m, hook);
            }
        }
    }
}
