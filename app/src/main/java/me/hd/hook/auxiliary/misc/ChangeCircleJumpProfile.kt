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

package me.hd.hook.auxiliary.misc

import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import me.hd.util.hookBeforeIfEnabled
import me.hd.util.name
import me.hd.util.parameters
import me.hd.util.returnType
import me.hd.util.singleMethod
import me.hd.util.toHostClass

@FunctionHookEntry
@UiItemAgentEntry
object ChangeCircleJumpProfile : CommonSwitchFunctionHook() {
    override val name = "更改小世界跳转资料"
    override val description = "忽略作者设置了QQ资料卡不可见,点击头像强制跳转"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MISC_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override fun initOnce(): Boolean {
        "com.tencent.biz.qqcircle.QCirclePluginUtil".toHostClass().also { circleClass ->
            circleClass.singleMethod {
                returnType(Boolean::class.java) &&
                    name("canJumpToQQProfile") &&
                    parameters(List::class.java)
            }.hookBeforeIfEnabled(this) { param ->
                param.result = true
            }
            circleClass.singleMethod {
                returnType(Boolean::class.java) &&
                    name("isJumpToQQProfileBeated") &&
                    parameters(List::class.java)
            }.hookBeforeIfEnabled(this) { param ->
                param.result = false
            }
        }
        return true
    }
}
