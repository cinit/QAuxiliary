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
package cc.ioctl.hook.ui.chat;

import static io.github.qauxv.util.Initiator._BaseChatPie;
import static io.github.qauxv.util.PlayQQVersion.PlayQQ_8_2_9;
import static io.github.qauxv.util.QQVersion.QQ_8_2_0;
import static io.github.qauxv.util.QQVersion.QQ_8_6_0;
import static io.github.qauxv.util.TIMVersion.TIM_3_1_1;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.hicore.QApp.QAppUtils;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.tlb.ConfigTable;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import io.github.qauxv.util.dexkit.Reply_At_QQNT;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

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
        return new String[]{"艾特", "at"};
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Simplify.UI_CHAT_MSG;
    }

    public static final ReplyNoAtHook INSTANCE = new ReplyNoAtHook();

    private ReplyNoAtHook() {
        super(new DexKitTarget[]{Reply_At_QQNT.INSTANCE});
    }

    @Override
    public boolean initOnce() throws ReflectiveOperationException {
        if (QAppUtils.isQQnt()) {
            HookUtils.hookBeforeIfEnabled(this, DexKit.requireMethodFromCache(Reply_At_QQNT.INSTANCE),
                    49, param -> param.setResult(null));
        } else if (HostInfo.requireMinQQVersion(QQ_8_6_0)) {
            String className = ConfigTable.getConfig(ReplyNoAtHook.class.getSimpleName());
            if (className == null) {
                return false;
            }
            Class<?> kInputUIUtils = Initiator.loadClass(className);
            Method method = null;
            for (Method m : kInputUIUtils.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers()) && m.getReturnType() == void.class) {
                    Class<?>[] argt = m.getParameterTypes();
                    if (argt[1] == Initiator._BaseSessionInfo() && argt[2] == boolean.class) {
                        method = m;
                        break;
                    }
                }
            }
            Objects.requireNonNull(method, "InputUIUtils.a(AIOContext, BaseSessionInfo, boolean)V not found");
            HookUtils.hookBeforeIfEnabled(this, method, 49, param -> {
                boolean p0 = (boolean) param.args[2];
                if (!p0) {
                    param.setResult(null);
                }
            });
            return true;
        } else {
            String methodName = ConfigTable.getConfig(ReplyNoAtHook.class.getSimpleName());
            if (methodName == null) {
                return false;
            }
            Method createAtMsg = _BaseChatPie().getDeclaredMethod(methodName, boolean.class);
            HookUtils.hookBeforeIfEnabled(this, createAtMsg, 49, param -> {
                boolean p0 = (boolean) param.args[0];
                if (!p0) {
                    param.setResult(null);
                }
            });
        }
        return true;
    }

    @Override
    public boolean isAvailable() {
        return HostInfo.requireMinQQVersion(QQ_8_2_0)
                || HostInfo.requireMinTimVersion(TIM_3_1_1)
                || HostInfo.requireMinPlayQQVersion(PlayQQ_8_2_9);
    }
}
