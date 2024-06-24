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

import static cc.ioctl.util.HostInfo.requireMinQQVersion;

import androidx.annotation.NonNull;
import cc.hicore.QApp.QAppUtils;
import cc.ioctl.util.HookUtils;
import cc.ioctl.util.Reflex;
import io.github.qauxv.base.annotation.FunctionHookEntry;
import io.github.qauxv.base.annotation.UiItemAgentEntry;
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify;
import io.github.qauxv.hook.CommonSwitchFunctionHook;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.QQVersion;
import java.lang.reflect.Method;
import java.util.ArrayList;

//屏蔽掉落小表情
@FunctionHookEntry
@UiItemAgentEntry
public class DisableDropSticker extends CommonSwitchFunctionHook {

    public static final DisableDropSticker INSTANCE = new DisableDropSticker();

    protected DisableDropSticker() {
        super("rq_disable_drop_sticker");
    }

    @NonNull
    @Override
    public String getName() {
        return "屏蔽掉落小表情";
    }

    @NonNull
    @Override
    public String[] getUiItemLocation() {
        return Simplify.UI_CHAT_MSG;
    }

    @Override
    public boolean isApplicationRestartRequired() {
        return true;
    }

    @Override
    public boolean initOnce() throws ReflectiveOperationException {
        String className;
        if (requireMinQQVersion(QQVersion.QQ_9_0_60)) { // 9.0.60~9.0.70
            className = "com.tencent.mobileqq.aio.animation.util.b";
        } else if (requireMinQQVersion(QQVersion.QQ_9_0_15)) { // 9.0.15~9.0.56
            className = "com.tencent.mobileqq.aio.animation.util.d";
        } else if (QAppUtils.isQQnt()) {
            className = "com.tencent.mobileqq.aio.animation.util.AioAnimationConfigHelper";
        } else {
            className = "com.tencent.mobileqq.activity.aio.anim.AioAnimationConfigHelper";
        }
        Class<?> kAioAnimationConfigHelper = Initiator.loadClass(className);
        Method doParseRules = Reflex.findSingleMethod(kAioAnimationConfigHelper, ArrayList.class, false, org.xmlpull.v1.XmlPullParser.class);
        HookUtils.hookBeforeIfEnabled(this, doParseRules, param -> param.setResult(new ArrayList<>()));
        return true;
    }
}
