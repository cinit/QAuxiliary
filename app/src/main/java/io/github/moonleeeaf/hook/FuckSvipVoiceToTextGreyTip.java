package io.github.moonleeeaf.hook;

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

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.NonNull;
import cc.ioctl.util.HookUtils;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import java.lang.reflect.Method;

@FunctionHookEntry
@UiItemAgentEntry
public class FuckSvipVoiceToTextGreyTip extends CommonSwitchFunctionHook {

    public static final FuckSvipVoiceToTextGreyTip INSTANCE = new FuckSvipVoiceToTextGreyTip();

    private FuckSvipVoiceToTextGreyTip() {
    }

    @NonNull
    @Override
    public String getName() {
        return "移除语音自动转文本灰字提示";
    }

    @NonNull
    @Override
    public String getDescription() {
        return "去你妈的自动语音转文本诱导开SBVIP消费\n理论支持任意 NTQQ, 仅在 TIM NT 4.0.98 测试通过";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return FunctionEntryRouter.Locations.Simplify.UI_CHAT_MSG;
    }

    @Override
    public boolean isAvailable() {
        return Initiator.load("com.tencent.mobileqq.vas.perception.api.impl.VipPerceptionImpl") != null;
    }

    @Override
    public boolean initOnce() throws Exception {
        // Lcom/tencent/mobileqq/vas/perception/api/impl/VipPerceptionImpl;->addSVipLocalGrayTip(Ljava/lang/String;I)V
        // qQ某些灰字提示真的恶心死人
        Method mMethod = Initiator.loadClass("com.tencent.mobileqq.vas.perception.api.impl.VipPerceptionImpl")
                .getDeclaredMethod("addSVipLocalGrayTip", String.class, int.class);

        HookUtils.hookBeforeIfEnabled(this, mMethod, (param) -> {
            param.setResult(null);
        });
        return true;
    }

}
