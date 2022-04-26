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

package me.ketal.hook

import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.bridge.AppRuntimeHelper
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator._MessageRecord
import me.singleneuron.data.MsgRecordData
import xyz.nextalone.util.hookBefore
import xyz.nextalone.util.invoke
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object ShowSelfAnonMsgLeft : CommonSwitchFunctionHook() {

    override val name = "自己的匿名消息居左显示"

    override val uiItemLocation = FunctionEntryRouter.Locations.Entertainment.ENTERTAIN_CATEGORY

    override fun initOnce() = throwOrTrue {
        _MessageRecord().getDeclaredMethod("isSend")
            .hookBefore(this) {
                val data = MsgRecordData(it.thisObject)
                if (AppRuntimeHelper.getAccount() == data.senderUin) {
                    val chatMessage = data.msgRecord
                    val extStr = chatMessage.invoke(
                        "getExtInfoFromExtStr",
                        "anonymous", String::class.java
                    ) as String?
                    val anonymous = extStr != null && extStr.isNotEmpty()
                    if (anonymous) it.result = false
                }

            }

    }
}
