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
package xyz.nextalone.hook

import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.bridge.AppRuntimeHelper
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator._TroopChatPie
import xyz.nextalone.util.get
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.method
import xyz.nextalone.util.throwOrTrue

@UiItemAgentEntry
@FunctionHookEntry
object CollapseTroopMessage : CommonSwitchFunctionHook("na_collapse_troop_message_kt") {

    override val name = "折叠群消息"
    override val description = "不推荐再使用"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY

    override fun initOnce() = throwOrTrue {
        _TroopChatPie().method(
            "a",
            List::class.java,
            List::class.java
        )?.hookAfter(this) {
            var picMd5: CharSequence
            var text: CharSequence
            val list = (it.result as List<*>).toMutableList()
            val iterator = list.iterator()
            val textList = arrayListOf("", "").toMutableList()
            val picList = arrayListOf("", "").toMutableList()
            while (iterator.hasNext()) {
                val obj = iterator.next()!!
                if (obj.javaClass.name == "com.tencent.mobileqq.data.MessageForText" && obj.get("sb") != null) {
                    text = obj.get("sb") as CharSequence
                    if (textList.last() == text.toString() && textList[textList.size - 2] == text.toString()) {
                        if (obj.get("senderuin") as String != AppRuntimeHelper.getLongAccountUin()
                                .toString()
                        ) iterator.remove()
                    }
                    textList.add(text.toString())
                }
                if (obj.javaClass.name == "com.tencent.mobileqq.data.MessageForPic" && obj.get("md5") != null) {
                    picMd5 = obj.get("md5") as CharSequence
                    if (picList.last() == picMd5.toString()) {
                        if (obj.get("senderuin") as String != AppRuntimeHelper.getLongAccountUin()
                                .toString()
                        ) iterator.remove()
                    }
                    picList.add(picMd5.toString())
                }
            }
            it.result = list
        }
    }
}
