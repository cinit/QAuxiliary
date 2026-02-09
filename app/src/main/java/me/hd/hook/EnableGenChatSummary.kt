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

package me.hd.hook

import cc.ioctl.util.hookAfterIfEnabled
import com.github.kyuubiran.ezxhelper.utils.findConstructor
import com.github.kyuubiran.ezxhelper.utils.paramCount
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.Hd_EnableGenChatSummary_Class
import io.github.qauxv.util.requireMinQQVersion

@FunctionHookEntry
@UiItemAgentEntry
object EnableGenChatSummary : CommonSwitchFunctionHook(
    targets = arrayOf(Hd_EnableGenChatSummary_Class)
) {

    override val name = "启用聊天总结"
    override val description = "当未读消息不多时, 仍然强制启用 '立即总结' 按钮"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.CHAT_OTHER
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_9_2_35)

    override fun initOnce(): Boolean {
        val clazz = DexKit.requireClassFromCache(Hd_EnableGenChatSummary_Class)
        val cons = clazz.findConstructor {
            paramCount == 5 && parameterTypes[1] == Boolean::class.java
        }
        hookAfterIfEnabled(cons) { param ->
            param.args[1] = true
        }
        return true
    }
}