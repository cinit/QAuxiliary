/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2026 QAuxiliary developers
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

package me.hd.hook.auxiliary.profile

import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import me.hd.util.hookAfterIfEnabled
import me.hd.util.name
import me.hd.util.parameterCount
import me.hd.util.returnType
import me.hd.util.singleMethod
import me.hd.util.toHostClass

@FunctionHookEntry
@UiItemAgentEntry
object GroupMemberManageFakeMyRole : CommonSwitchFunctionHook() {
    override val name = "群成员管理页伪装身份为群主"
    override val description = "使其在管理页显示群昵称及设置禁言等入口"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.PROFILE_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_9_2_20)

    override fun initOnce(): Boolean {
        // MemberSettingGroupManagePart
        "com.tencent.mobileqq.troop.membersetting.model.a".toHostClass()
            .singleMethod {
                // myRole
                returnType(Int::class.java) &&
                    name("o") &&
                    parameterCount(0)
            }.hookAfterIfEnabled(this) { param ->
                param.result = 3
            }
        return true
    }
}
