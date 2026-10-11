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

package me.hd.hook

import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import me.hd.util.hookBeforeIfEnabled
import me.hd.util.name
import me.hd.util.returnType
import me.hd.util.singleMethod
import me.hd.util.toHostClass

@FunctionHookEntry
@UiItemAgentEntry
object RandomBubble : CommonSwitchFunctionHook() {
    override val name = "随机气泡"
    override val uiItemLocation = FunctionEntryRouter.Locations.Entertainment.ENTERTAIN_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_9_2_30)

    override fun initOnce(): Boolean {
        "com.tencent.mobileqq.app.SVIPHandler".toHostClass()
            .singleMethod {
                returnType(Int::class.java) &&
                    name("getSubBubbleId")
            }.hookBeforeIfEnabled(this) { param ->
                val bubbles = listOf(
                    2143197 to "超大简约黑",
                    2143198 to "超大简约白",
                    2143199 to "超大简约绿",
                    2143200 to "超大简约黄",
                    2143281 to "超大简约橙",
                    2143283 to "超大简约灰",
                    2143285 to "超大简约蓝",
                    2143286 to "超大简约粉",
                    2143287 to "超大简约红",
                    2143487 to "超大简约紫"
                )
                val (id, name) = bubbles.random()
                param.result = id
            }
        return true
    }
}
