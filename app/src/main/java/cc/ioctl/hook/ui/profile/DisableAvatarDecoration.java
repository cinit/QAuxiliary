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
package cc.ioctl.hook.ui.profile;

import static io.github.qauxv.util.HostInfo.requireMinQQVersion;

import android.view.View;
import androidx.annotation.NonNull;
import cc.hicore.QApp.QAppUtils;
import cc.ioctl.util.HookUtils;
import com.tencent.qqnt.kernel.nativeinterface.VASMsgAvatarPendant;
import io.github.qauxv.util.xpcompat.XC_MethodHook;
import io.github.qauxv.util.xpcompat.XposedBridge;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.QQVersion;
import java.lang.reflect.Method;
import java.util.Objects;
import kotlin.Lazy;
import kotlin.LazyKt;

//屏蔽头像挂件
@FunctionHookEntry
@UiItemAgentEntry
public class DisableAvatarDecoration extends CommonSwitchFunctionHook {

    public static final DisableAvatarDecoration INSTANCE = new DisableAvatarDecoration();

    private final Lazy<Class<VASMsgAvatarPendant>> lazyVapCls = LazyKt.lazy(() -> VASMsgAvatarPendant.class);

    protected DisableAvatarDecoration() {
        super("rq_disable_avatar_decoration");
    }

    @NonNull
    @Override
    public String getName() {
        return "屏蔽头像挂件";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Simplify.CHAT_DECORATION;
    }

    @Override
    public boolean initOnce() throws NoSuchMethodException {
        if (requireMinQQVersion(QQVersion.QQ_9_0_15)) {
            XposedBridge.hookAllConstructors(lazyVapCls.getValue(), new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(@NonNull MethodHookParam param) {
                    VASMsgAvatarPendant v = (VASMsgAvatarPendant) param.thisObject;
                    v.pendantId = 0L;
                    v.pendantDiyInfoId = 0;
                }
            });
            return true;
        } else if (QAppUtils.isQQnt()) {
            HookUtils.hookBeforeIfEnabled(this, lazyVapCls.getValue().getDeclaredMethod("getPendantId"), param -> param.setResult(0L));
            HookUtils.hookBeforeIfEnabled(this, lazyVapCls.getValue().getDeclaredMethod("getPendantDiyInfoId"), param -> param.setResult(0));
            return true;
        }
        for (Method m : Initiator.load("com.tencent.mobileqq.vas.PendantInfo").getDeclaredMethods()) {
            if (m.getReturnType() == void.class) {
                Class<?>[] argt = m.getParameterTypes();
                if (argt.length == 5 && argt[0] == View.class && argt[1] == int.class
                        && argt[2] == long.class && argt[3] == String.class && argt[4] == int.class) {
                    HookUtils.hookBeforeIfEnabled(this, m, param -> param.setResult(null));
                }
            }
        }
        //Lcom/tencent/mobileqq/vas/pendant/drawable/PendantInfo;->setDrawable(Landroid/view/View;IJLjava/lang/String;I)V
        try {
            Method method = Objects.requireNonNull(Initiator.load("com.tencent.mobileqq.vas.pendant.drawable.PendantInfo"))
                    .getMethod("setDrawable", View.class, int.class, long.class, String.class, int.class);
            HookUtils.hookBeforeIfEnabled(this, method, param -> param.setResult(null));
        } catch (Exception e) {
            // 老版本无此方法，忽略
        }
        return true;
    }
}
