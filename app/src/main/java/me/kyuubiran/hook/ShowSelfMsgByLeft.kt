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
package me.kyuubiran.hook

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.DexMethodDescriptor
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.LicenseStatus
import xyz.nextalone.util.throwOrTrue

//自己的消息居左显示
@FunctionHookEntry
@UiItemAgentEntry
object ShowSelfMsgByLeft : CommonSwitchFunctionHook() {

    override val name = "自己的消息居左显示"

    override val uiItemLocation = FunctionEntryRouter.Locations.Entertainment.ENTERTAIN_CATEGORY

    override fun initOnce() = throwOrTrue {
        val m = DexMethodDescriptor("Lcom/tencent/mobileqq/activity/aio/BaseChatItemLayout;->setHearIconPosition(I)V")
            .getMethodInstance(Initiator.getHostClassLoader())
        XposedBridge.hookMethod(m, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam?) {
                if (LicenseStatus.sDisableCommonHooks) return
                if (!isEnabled) return
                param?.result = null
            }
        })
    }
}
