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

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import cc.hicore.QApp.QAppUtils;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.HostInfo;
import cc.ioctl.util.Reflex;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.QQVersion;
import io.github.qauxv.util.SyncUtils;
import io.github.qauxv.util.dexkit.CTroopGiftUtil;
import io.github.qauxv.util.dexkit.DexKit;
import io.github.qauxv.util.dexkit.DexKitTarget;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

@FunctionHookEntry
@UiItemAgentEntry
public class SendGiftHook extends CommonSwitchFunctionHook {

    @NonNull
    @Override
    public String getName() {
        return "禁用$打开送礼界面";
    }

    @Nullable
    @Override
    public String getDescription() {
        return "禁止聊天时输入$自动弹出[选择赠送对象]窗口";
    }

    @Override
    public boolean isAvailable() {
        return !QAppUtils.isQQnt();
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Simplify.CHAT_OTHER;
    }

    public static final SendGiftHook INSTANCE = new SendGiftHook();

    private SendGiftHook() {
        super(SyncUtils.PROC_MAIN, new DexKitTarget[]{CTroopGiftUtil.INSTANCE});
    }

    @Override
    public boolean initOnce() throws Exception {
        {
            Class<?> kTroopGiftUtil = Objects.requireNonNull(DexKit.loadClassFromCache(CTroopGiftUtil.INSTANCE), "CTROOP_GIFT_UTIL.INSTANCE");
            Method startSendGiftActivity = Reflex.findSingleMethodOrNull(kTroopGiftUtil,
                    void.class, false,
                    Activity.class, String.class, String.class, Initiator._QQAppInterface());
            if (startSendGiftActivity != null) {
                // startSendGiftActivity was not used on NT and was removed iin 8.9.63 near 4190
                HookUtils.hookBeforeIfEnabled(this, startSendGiftActivity, 47, param -> param.setResult(null));
            }
        }
        {
            Class<?> kQWalletTextChangeCallback = Initiator.load("com/tencent/mobileqq/activity/aio/rebuild/input/edittext/QWalletTextChangeCallback");
            if (kQWalletTextChangeCallback == null) {
                // 8.8.98/3002 com/tencent/mobileqq/activity/aio/rebuild/input/edittext/QWalletTextChangeCallback
                // 8.9.0/3060 com/tencent/mobileqq/activity/aio/rebuild/input/b/e
                kQWalletTextChangeCallback = Initiator.load("com/tencent/mobileqq/activity/aio/rebuild/input/b/e");
            }
            if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_6_5)) {
                Objects.requireNonNull(kQWalletTextChangeCallback, "kQWalletTextChangeCallback");
            }
            if (kQWalletTextChangeCallback != null) {
                // private void ?(AIOContext, TroopInfo)
                Class<?> kTroopInfo = Objects.requireNonNull(Initiator._TroopInfo(), "TroopInfo.class");
                Method method = null;
                for (Method m : kQWalletTextChangeCallback.getDeclaredMethods()) {
                    if (m.getModifiers() == Modifier.PRIVATE && m.getReturnType() == void.class) {
                        Class<?>[] argt = m.getParameterTypes();
                        if (argt.length == 2 && !argt[0].isPrimitive() && argt[1] == kTroopInfo) {
                            method = m;
                            break;
                        }
                    }
                }
                Objects.requireNonNull(method, "QWalletTextChangeCallback.?(AIOContext, TroopInfo)V");
                HookUtils.hookBeforeIfEnabled(this, method, 47, param -> param.setResult(null));
            }
        }
        return true;
    }
}
