/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
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

package cc.microblock.hook

import cc.ioctl.util.Reflex
import cc.ioctl.util.hookBeforeIfEnabled
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.TIMVersion
import io.github.qauxv.util.requireMinTimVersion
import io.github.qauxv.util.requireTimVersionExactly
import java.lang.reflect.Method

@FunctionHookEntry
@UiItemAgentEntry
object TimRemoveToastTips : CommonSwitchFunctionHook() {
    override val name = "移除群聊“修改/设置消息设置”提示"
    override val description = "仅供 TIM 3.0.0(1082) / 3.5.1 使用";
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.GROUP_CATEGORY

    override val isAvailable: Boolean
        get() = requireTimVersionExactly(
            TIMVersion.TIM_3_0_0, TIMVersion.TIM_3_0_0_1, TIMVersion.TIM_3_5_1
        )

    override fun initOnce(): Boolean {
        // 功能基于 Issue #781 和 #667 移植实现
        val runMethods = ArrayList<Method>();

        if (requireMinTimVersion(TIMVersion.TIM_3_5_1)) {
            // TIM 3.5.1
            runMethods += Reflex.findMethod(
                Initiator.loadClass("com.tencent.mobileqq.activity.aio.rebuild.TroopChatPie\$8\$1"),
                "run"
            )

            // 漏了一个“你可以在这里xxxxx”，实在吐了，这玩意不定时的
            runMethods += Reflex.findMethod(
                Initiator.loadClass("com.tencent.mobileqq.activity.aio.rebuild.TroopChatPie\$38"),
                "run"
            )
        } else if (requireMinTimVersion(TIMVersion.TIM_3_0_0_1)) {
            // TIM 3.0.0(1082)
            // Resource ID： 0x7f0e0189
            // Lcom/tencent/mobileqq/activity/aio/rebuild/TroopChatPie$39$1;->run()V
            // 原文本：修改消息设置，实时...
            runMethods += Reflex.findMethod(
                Initiator.loadClass("com.tencent.mobileqq.activity.aio.rebuild.TroopChatPie\$39\$1"),
                "run"
            )
            // 7f0e018a
            // 你可以在这里设置...
            runMethods += Reflex.findMethod(
                Initiator.loadClass("com.tencent.mobileqq.activity.aio.rebuild.TroopChatPie\$23"),
                "run"
            )
        }

        runMethods.forEach {
            hookBeforeIfEnabled(it) { param ->
                param.result = null
            }
        }

        return runMethods.isNotEmpty()
    }

}
