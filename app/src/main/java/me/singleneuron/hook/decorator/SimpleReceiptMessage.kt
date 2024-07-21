/*
 * QNotified - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 dmca@ioctl.cc
 * https://github.com/ferredoxin/QNotified
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/ferredoxin/QNotified/blob/master/LICENSE.md>.
 */
package me.singleneuron.hook.decorator

import cc.ioctl.util.Reflex
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedHelpers
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.router.decorator.BaseSwitchFunctionDecorator
import io.github.qauxv.router.decorator.IItemBuilderFactoryHookDecorator
import io.github.qauxv.router.dispacher.ItemBuilderFactoryHook

@UiItemAgentEntry
@FunctionHookEntry
object SimpleReceiptMessage : BaseSwitchFunctionDecorator(), IItemBuilderFactoryHookDecorator {

    override val name = "回执消息文本化"
    override val description = "可能导致聊天界面滑动掉帧"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.UI_CHAT_MSG
    override val dispatcher = ItemBuilderFactoryHook

    override fun onGetMsgTypeHook(
            result: Int,
            chatMessage: Any,
            param: XC_MethodHook.MethodHookParam
    ): Boolean {
        if (result == 5) {
            val id = Reflex.getInstanceObjectOrNull(
                    Reflex.getInstanceObjectOrNull(
                            param.args[param.args.size - 1],
                            "structingMsg"
                    ), "mMsgServiceID"
            ) as Int
            if (id == 107) {
                XposedHelpers.setObjectField(chatMessage, "msg", "[回执消息]")
                param.result = -1
                return true
            }
        }
        return false
    }
}
