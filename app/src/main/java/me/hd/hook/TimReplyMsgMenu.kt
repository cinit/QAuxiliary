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

import com.github.kyuubiran.ezxhelper.utils.hookBefore
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.TIMVersion
import io.github.qauxv.util.hostInfo
import io.github.qauxv.util.isTim
import xyz.nextalone.util.method

@FunctionHookEntry
@UiItemAgentEntry
object TimReplyMsgMenu : CommonSwitchFunctionHook() {
    override val name = "TIM回复消息菜单"
    override val description = "在私聊和自己发送的消息上,增加回复菜单,仅支持TIM3.0.0"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY
    override val isAvailable = isTim() && hostInfo.versionCode == TIMVersion.TIM_3_0_0

    override fun initOnce(): Boolean {
        "Lcom/tencent/mobileqq/activity/aio/BaseBubbleBuilder;->a(Lcom/tencent/mobileqq/data/ChatMessage;Lcom/tencent/mobileqq/utils/dialogutils/QQCustomMenu;)V".method.hookBefore {
            val qqCustomMenu = it.args[1]
            qqCustomMenu::class.java
                .getDeclaredMethod("F", Int::class.java, String::class.java, Int::class.java)
                .invoke(qqCustomMenu, 0x7f081f64, "回复", 0x7f070269)
            it.result = null
        }
        return true
    }
}