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

import android.content.Context
import android.view.View
import com.github.kyuubiran.ezxhelper.utils.findMethod
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import xyz.nextalone.util.replace
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object HideAssistantRemoveTips : CommonSwitchFunctionHook("ketal_hide_assistant_removetips") {

    override val name = "移除移出群助手提醒"

    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.CHAT_GROUP_OTHER

    override fun initOnce() = throwOrTrue {
        val k = Initiator.loadClass("com/tencent/mobileqq/activity/ChatActivityUtils")
        val showChatTopBar = findMethod(k, false) {
            if (returnType == View::class.java) {
                val params = parameterTypes
                return@findMethod params.size == 4 && params[0] == Context::class.java && params[1] == String::class.java
                    && params[2] == View.OnClickListener::class.java && params[3] == View.OnClickListener::class.java
            }
            return@findMethod false
        }
        showChatTopBar.replace(this, null)
    }
}
