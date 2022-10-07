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
package com.rymmmmm.hook;

import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import cc.ioctl.util.HostInfo;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.LicenseStatus;
import io.github.qauxv.util.QQVersion;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import io.github.qauxv.util.dexkit.NCustomWidgetUtil_updateCustomNoteTxt;
import java.lang.reflect.Method;

/**
 * 显示具体消息数量
 * <p>
 * Peak frequency: ~172 invocations per second
 */
@FunctionHookEntry
@UiItemAgentEntry
public class ShowMsgCount extends CommonSwitchFunctionHook {

    public static final ShowMsgCount INSTANCE = new ShowMsgCount();

    private ShowMsgCount() {
        super(new DexKitTarget[]{NCustomWidgetUtil_updateCustomNoteTxt.INSTANCE});
    }

    @NonNull
    @Override
    public String getName() {
        return "显示具体消息数量";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Auxiliary.MESSAGE_CATEGORY;
    }

    @Override
    public boolean initOnce() {
        Method updateCustomNoteTxt = DexKit.loadMethodFromCache(NCustomWidgetUtil_updateCustomNoteTxt.INSTANCE);
        XposedBridge.hookMethod(updateCustomNoteTxt, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (LicenseStatus.sDisableCommonHooks || !isEnabled()) {
                    return;
                }
                param.args[4] = Integer.MAX_VALUE;
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (LicenseStatus.sDisableCommonHooks || !isEnabled()) {
                    return;
                }
                try {
                    if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_8_11)) {
                        TextView tv = (TextView) param.args[0];
                        tv.setMaxWidth(Integer.MAX_VALUE);
                        ViewGroup.LayoutParams lp = tv.getLayoutParams();
                        lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                        tv.setLayoutParams(lp);
                    }
                } catch (Throwable e) {
                    traceError(e);
                    throw e;
                }
            }
        });
        return true;
    }
}
