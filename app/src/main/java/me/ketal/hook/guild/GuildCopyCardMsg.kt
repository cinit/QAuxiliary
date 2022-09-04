/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package me.ketal.hook.guild

import android.view.View
import cc.ioctl.hook.CopyCardMsg
import cc.ioctl.util.Reflex.getFirstByType
import io.github.qauxv.R
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.CustomMenu
import io.github.qauxv.util.Initiator._ChatMessage
import io.github.qauxv.util.Toasts
import xyz.nextalone.util.SystemServiceUtils.copyToClipboard
import xyz.nextalone.util.clazz
import xyz.nextalone.util.get
import xyz.nextalone.util.hookBefore
import xyz.nextalone.util.hookBeforeAllConstructors
import xyz.nextalone.util.invoke
import xyz.nextalone.util.method
import xyz.nextalone.util.throwOrTrue

@UiItemAgentEntry
@FunctionHookEntry
object GuildCopyCardMsg : CommonSwitchFunctionHook() {

    override val name = "频道复制卡片消息"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.GUILD_CATEGORY

    override fun initOnce() = throwOrTrue {
        val adapterClazz = "com.tencent.mobileqq.guild.message.chatpie.GuildChatpieMenuAdapter".clazz
            ?: return@throwOrTrue
        val clFragment = "com.tencent.mobileqq.guild.message.chatpie.GuildMenuDialogFragment".clazz
            ?: return@throwOrTrue
        val clMessageForStructing = "com.tencent.mobileqq.data.MessageForStructing".clazz!!
        val clMessageForArkApp = "com.tencent.mobileqq.data.MessageForArkApp".clazz!!

        clFragment.hookBeforeAllConstructors {
            val (_, menu, chatMessage) = it.args
            if (!chatMessage.javaClass.isAssignableFrom(clMessageForStructing)
                && !chatMessage.javaClass.isAssignableFrom(clMessageForArkApp)) return@hookBeforeAllConstructors
            val list = getFirstByType(menu, List::class.java) as MutableList<Any>
            val clQQCustomMenuItem = list.first().javaClass
            val item = CustomMenu.createItem(
                clQQCustomMenuItem,
                CopyCardMsg.R_ID_COPY_CODE,
                "复制代码",
                R.drawable.ic_guild_schedule_edit
            )
            list.add(item)
        }

        adapterClazz.method("onClick")?.hookBefore(this) {
            val view = it.args[0] as View
            val ctx = view.context
            if (view.id == CopyCardMsg.R_ID_COPY_CODE) {
                it.result = null
                val fragment = getFirstByType(it.thisObject, clFragment)
                val chatMessage = getFirstByType(fragment,
                    _ChatMessage())
                val text = when {
                    clMessageForStructing.isAssignableFrom(chatMessage.javaClass) -> {
                        chatMessage.get("structingMsg")?.invoke("getXml") as String
                    }
                    clMessageForArkApp.isAssignableFrom(chatMessage.javaClass) -> {
                        chatMessage.get("ark_app_message")?.invoke("toAppXml") as String
                    }
                    else -> return@hookBefore
                }
                copyToClipboard(ctx, text)
                Toasts.info(ctx, "复制成功")
                fragment.invoke("dismiss")
            }
        }
    }
}
