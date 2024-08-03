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

import cc.hicore.QApp.QAppUtils
import cc.ioctl.util.HookUtils
import cc.ioctl.util.Reflex
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import xyz.nextalone.util.get
import xyz.nextalone.util.set
import cc.ioctl.util.HostInfo
import io.github.qauxv.util.TIMVersion

@FunctionHookEntry
@UiItemAgentEntry
object TimRemoveToastTips : CommonSwitchFunctionHook() {
    override val name = "移除群聊“修改/设置消息设置”提示"
    override val description = "仅供 TIM 3.5.1 和 TIM 3.0.0(1082) 使用";

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.GROUP_CATEGORY

    override fun initOnce(): Boolean {

        if (HostInfo.getLongVersionCode‎ == TIMVersion.TIM_3_0_0_1) {
            // 3.0.0 (1082)
            HookUtils.hookBeforeIfEnabled(
                this, Reflex.findMethod(
                    Initiator.loadClass("com.tencent.mobileqq.activity.aio.rebuild.TroopChatPie\$39\$1"),
                    "run")
            ) {
                it.result = null;
            }
        } else {
            // 3.5.1
            HookUtils.hookBeforeIfEnabled(
                this, Reflex.findMethod(
                    Initiator.loadClass("com.tencent.mobileqq.activity.aio.rebuild.TroopChatPie\$8\$1"),
                    "run")
            ) {
                it.result = null;
            }
            // “你可以在这里xxxxx”
            HookUtils.hookBeforeIfEnabled(
                this, Reflex.findMethod(
                    Initiator.loadClass("com.tencent.mobileqq.activity.aio.rebuild.TroopChatPie\$38"),
                    "run")
            ) {
                it.result = null;
            }
        }
        return true;
    }

}
