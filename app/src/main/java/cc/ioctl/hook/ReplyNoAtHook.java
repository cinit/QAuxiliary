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

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static io.github.qauxv.util.Initiator._BaseChatPie;
import static io.github.qauxv.util.PlayQQVersion.PlayQQ_8_2_9;
import static io.github.qauxv.util.QQVersion.QQ_8_1_3;
import static io.github.qauxv.util.QQVersion.QQ_8_6_0;
import static io.github.qauxv.util.TIMVersion.TIM_3_1_1;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.ioctl.util.HostInfo;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.tlb.ConfigTable;
import io.github.qauxv.util.DexMethodDescriptor;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.LicenseStatus;
import java.lang.reflect.Method;

@FunctionHookEntry
@UiItemAgentEntry
public class ReplyNoAtHook extends CommonSwitchFunctionHook {

    @NonNull
    @Override
    public String getName() {
        return "禁止回复自动@";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "去除回复消息时自动@特性";
    }

    @Nullable
    @Override
    public String[] getExtraSearchKeywords() {
        return new String[]{"艾特"};
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Simplify.MAIN_UI_MSG;
    }

    public static final ReplyNoAtHook INSTANCE = new ReplyNoAtHook();

    private ReplyNoAtHook() {
        super();
    }

    /**
     * 813 1246 k 815 1258 l 818 1276 l 820 1296 l 826 1320 m 827 1328 m ... 836 1406 n ^ 848 1492
     * createAtMsg
     */
    @Override
    public boolean initOnce() throws Exception {
        String method = ConfigTable.getConfig(ReplyNoAtHook.class.getSimpleName());
        if (method == null) {
            return false;
        }
        if (HostInfo.requireMinQQVersion(QQ_8_6_0)) {
            Method m = new DexMethodDescriptor(
                    "Lcom/tencent/mobileqq/activity/aio/rebuild/input/InputUIUtils;->a(Lcom/tencent/mobileqq/activity/aio/core/AIOContext;Lcom/tencent/mobileqq/activity/aio/BaseSessionInfo;Z)V").getMethodInstance(
                    Initiator.getHostClassLoader());
            XposedBridge.hookMethod(m, new XC_MethodHook(49) {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (LicenseStatus.sDisableCommonHooks) {
                        return;
                    }
                    if (!isEnabled()) {
                        return;
                    }
                    boolean p0 = (boolean) param.args[2];
                    if (!p0) {
                        param.setResult(null);
                    }
                }
            });
            return true;
        }
        findAndHookMethod(_BaseChatPie(), method, boolean.class, new XC_MethodHook(49) {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (LicenseStatus.sDisableCommonHooks) {
                    return;
                }
                if (!isEnabled()) {
                    return;
                }
                boolean p0 = (boolean) param.args[0];
                if (!p0) {
                    param.setResult(null);
                }
            }
        });
        return true;
    }

    @Override
    public boolean isAvailable() {
        return HostInfo.requireMinQQVersion(QQ_8_1_3)
                || HostInfo.requireMinTimVersion(TIM_3_1_1)
                || HostInfo.requireMinPlayQQVersion(PlayQQ_8_2_9);
    }
}
