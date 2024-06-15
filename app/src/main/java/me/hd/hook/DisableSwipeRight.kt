/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2024 QAuxiliary developers
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

import cc.ioctl.util.hookBeforeIfEnabled
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.util.isPrivate

@FunctionHookEntry
@UiItemAgentEntry
object DisableSwipeRight : CommonSwitchFunctionHook() {

    override val name = "屏蔽聊天右滑"
    override val description = "屏蔽聊天右滑显示界面[群聊|群应用][好友|亲密关系]"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.CHAT_OTHER
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override fun initOnce(): Boolean {
        val drawerFrameClass = Initiator.loadClass("com.tencent.aio.frame.drawer.DrawerFrameViewGroup")
        val scrollMethod = drawerFrameClass.declaredMethods.single { method ->
            val params = method.parameterTypes
            method.isPrivate && params.size == 1 && params[0] == Int::class.java
        }
        hookBeforeIfEnabled(scrollMethod) { param ->
            val scrollX = param.args[0] as Int
            if (scrollX > 0) {
                param.args[0] = 0
            }
        }
        return true
    }
}