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
import io.github.qauxv.BuildConfig
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.router.decorator.BaseSwitchFunctionDecorator
import io.github.qauxv.router.decorator.IItemBuilderFactoryHookDecorator
import io.github.qauxv.router.dispacher.ItemBuilderFactoryHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Log

@UiItemAgentEntry
@FunctionHookEntry
object CardMsgToText : BaseSwitchFunctionDecorator(), IItemBuilderFactoryHookDecorator {

    override val name = "卡片消息文本化"
    override val description = "可能导致聊天界面滑动掉帧"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.UI_CHAT_MSG
    override val dispatcher = ItemBuilderFactoryHook

    override fun onGetMsgTypeHook(
            result: Int,
            chatMessage: Any,
            param: XC_MethodHook.MethodHookParam
    ): Boolean {
        return try {
            var text: String
            if (Initiator.loadClass("com.tencent.mobileqq.data.MessageForStructing")
                            .isAssignableFrom(chatMessage.javaClass)
            ) {
                text = Reflex.invokeVirtual(
                        Reflex.getInstanceObjectOrNull(
                                chatMessage,
                                "structingMsg"
                        ), "getXml", *arrayOfNulls(0)
                ) as String
                dumpCardMsg(chatMessage)
            } else if (Initiator.loadClass("com.tencent.mobileqq.data.MessageForArkApp")
                            .isAssignableFrom(chatMessage.javaClass)
            ) {
                text = Reflex.invokeVirtual(
                        Reflex.getInstanceObjectOrNull(
                                chatMessage,
                                "ark_app_message"
                        ), "toAppXml", *arrayOfNulls(0)
                ) as String
                dumpCardMsg(chatMessage)
            } else return false

            text = "[卡片消息] ${chatMessage::class.java.simpleName}\n\n$text"
            XposedHelpers.setObjectField(chatMessage, "msg", text)
            param.result = -1
            true
        } catch (e: Exception) {
            traceError(e)
            false
        }
    }
}

private fun dumpCardMsg(chatMessage: Any) {
    if (!BuildConfig.DEBUG) return
    try {
        Log.d("Start dump card message...")
        Log.d("chatMessage class: " + chatMessage::class.java.name)

        if (Initiator.loadClass("com.tencent.mobileqq.data.MessageForStructing")
                        .isAssignableFrom(chatMessage.javaClass)
        ) {
            val structingMsg = Reflex.getInstanceObjectOrNull(chatMessage, "structingMsg")
            Log.d("structingMsg class: " + structingMsg::class.java.name)
        }

        if (Initiator.loadClass("com.tencent.mobileqq.data.MessageForArkApp")
                        .isAssignableFrom(chatMessage.javaClass)
        ) {
            val arkAppMessage = Reflex.getInstanceObjectOrNull(chatMessage, "ark_app_message")
            Log.d("ark_app_message class: " + arkAppMessage::class.java.name)
        }

        Log.d("...Dump end")
    } catch (e: Exception) {
        Log.e(e)
    }
}
