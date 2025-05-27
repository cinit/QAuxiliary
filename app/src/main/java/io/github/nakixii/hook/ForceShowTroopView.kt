/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2025 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is an opensource software: you can redistribute it
 * and/or modify it under the terms of the General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the General Public License for more details.
 *
 * You should have received a copy of the General Public License
 * along with this software.
 * If not, see
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package io.github.nakixii.hook

import cc.ioctl.util.hookAfterIfEnabled
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator

@FunctionHookEntry
@UiItemAgentEntry
object ForceShowTroopView : CommonSwitchFunctionHook() {
    override val name = "显示已退群用户的信息"
    override val description = "用于直接查看已退群用户的发言记录"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.GROUP_CATEGORY

    override fun initOnce(): Boolean {
        val troopInfoClass = Initiator.loadClass("com.tencent.mobileqq.profilecard.component.troop.ElegantProfileTroopMemInfoComponent")
        hookAfterIfEnabled(troopInfoClass.declaredMethods.single { it.name == "getTroopMemeJoinTime" }) {
            if (it.result == "") it.result = "已退出该群"
        }

        return true
    }
}
