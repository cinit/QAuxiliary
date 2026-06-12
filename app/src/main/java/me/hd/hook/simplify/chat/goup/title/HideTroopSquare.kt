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

package me.hd.hook.simplify.chat.goup.title

import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import me.hd.util.hookBeforeIfEnabled
import me.hd.util.parameters
import me.hd.util.returnType
import me.hd.util.singleMethod
import me.hd.util.toHostClass

@FunctionHookEntry
@UiItemAgentEntry
object HideTroopSquare : CommonSwitchFunctionHook() {
    override val name = "隐藏群广场"
    override val description = "隐藏群聊聊天页右上角展示的群广场"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.CHAT_GROUP_TITLE
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_9_0_80)

    override fun initOnce(): Boolean {
        "com.tencent.mobileqq.aio.title.AIOTitleVB".toHostClass()
            .singleMethod {
                returnType(Void.TYPE) &&
                    parameters(String::class.java)
            }.hookBeforeIfEnabled(this) { param ->
                val keyword = param.args[0] as String
                if (keyword == "troop_square_vb") {
                    param.result = null
                }
            }
        return true
    }
}
