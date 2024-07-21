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
package cc.ioctl.hook.ui.title;

import android.view.View;
import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import io.github.qauxv.util.xpcompat.XC_MethodReplacement;
import io.github.qauxv.util.xpcompat.XposedBridge;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

//移除消息列表顶栏横幅广告
@UiItemAgentEntry
@FunctionHookEntry
public class RemoveQbossAD extends CommonSwitchFunctionHook {

    public static final RemoveQbossAD INSTANCE = new RemoveQbossAD();

    private RemoveQbossAD() {
        super("kr_remove_qboss_ad");
    }

    @NonNull
    @Override
    public String getName() {
        return "移除消息列表顶栏横幅广告";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Simplify.MAIN_UI_TITLE;
    }

    @Override
    public boolean initOnce() {
        for (Method m : Initiator._QbossADImmersionBannerManager().getDeclaredMethods()) {
            Class<?>[] argt = m.getParameterTypes();
            if (m.getReturnType() == View.class && argt.length == 0 && !Modifier.isStatic(m.getModifiers())) {
                HookUtils.hookBeforeIfEnabled(this, m, param -> param.setResult(null));
            }
        }
        try {
            XposedBridge.hookAllMethods(Initiator.load(
                    "com.tencent.mobileqq.activity.recent.bannerprocessor.VasADBannerProcessor"),
                "handleMessage", new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) {
                        try {
                            return XposedBridge.invokeOriginalMethod(param.method,
                                param.thisObject, param.args);
                        } catch (Exception e) {
                            traceError(e);
                            return null;
                        }
                    }
                });
        } catch (Exception e) {
            //ignore
        }
        return true;
    }
}
