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

import android.content.Context
import cc.ioctl.util.hookBeforeIfEnabled
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion

@FunctionHookEntry
@UiItemAgentEntry
object DisableChatsCardContainer : CommonSwitchFunctionHook() {

    override val name = "屏蔽聊天列表顶部卡片推荐"
    override val description = "屏蔽QQ9.0.75+新增的短视频/推荐好友"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.MAIN_UI_TITLE
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_9_0_75)

    override fun initOnce(): Boolean {
        val chatsCardContainerClass = Initiator.loadClass("com.tencent.mobileqq.chatlist.MainChatsCardContainerPartImpl")
        hookBeforeIfEnabled(chatsCardContainerClass.declaredMethods.single { method ->
            val params = method.parameterTypes
            params.size == 2 && params[0] == Context::class.java && params[1] == Boolean::class.java
        }) { it.result = null }
        return true
    }
}