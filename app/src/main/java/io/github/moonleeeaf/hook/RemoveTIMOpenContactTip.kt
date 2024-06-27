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

import cc.ioctl.util.HookUtils
import cc.ioctl.util.HostInfo.isTim
import cc.ioctl.util.Reflex
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator

// 模板: TimRemoveToastTips.kt
@FunctionHookEntry
@UiItemAgentEntry
object RemoveTIMOpenContactTip : CommonSwitchFunctionHook() {

    override val name = "禁止提示打开通讯录"
    override val description = "未经测试，根据 TIM 3.5.1 代码编写"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.MAIN_UI_CONTACT
    override val isAvailable = isTim()

    override fun initOnce(): Boolean {
        // 字符串ID 7f0f4c88
        // 最终找到 aejy 方法名 migrateOldOrDefaultContent
        // 目前该字符串搜出的方法最终调用都在这

        HookUtils.hookBeforeIfEnabled(this, Reflex.findMethod(Initiator.loadClass("aejy"), "migrateOldOrDefaultContent")) {
            it.result = null
        }
        return true
    }

}
