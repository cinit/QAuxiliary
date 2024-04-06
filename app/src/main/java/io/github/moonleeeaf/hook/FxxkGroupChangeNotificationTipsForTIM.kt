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

package io.github.moonleeeaf.hook

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

// 参考滞空方法模板: TimRemoveToastTips.kt
@FunctionHookEntry
@UiItemAgentEntry
object FxxkGroupChangeNotificationTipsForTIM : CommonSwitchFunctionHook() {
    override val name = "移除群聊灰字“打开消息推送设置”"
    override val description = "参考 Issue #889，仅适用于 TIM 3.5.1，未经测试";

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.GROUP_CATEGORY

    // 很抱歉我没有太多精力去测试 DexKit，因此这还需要各位大佬帮助我完善
    // 若完善后可以删除此注释
    
    // 反编译自 TIM 3.5.1 (1298)，未经测试
    // https://github.com/cinit/QAuxiliary/issues/889
    override fun initOnce(): Boolean {
        HookUtils.hookBeforeIfEnabled(
            this, Reflex.findMethod(
                Initiator.loadClass("aiyc"),
                "g")
        ) {
            it.result = null;
        }
        
        HookUtils.hookBeforeIfEnabled(
            this, Reflex.findMethod(
                Initiator.loadClass("aiyc"),
                "b")
        ) {
            it.result = null;
        }

        return true;
    }

}
