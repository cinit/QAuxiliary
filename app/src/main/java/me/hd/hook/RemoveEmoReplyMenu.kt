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
import android.view.View
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
object RemoveEmoReplyMenu : CommonSwitchFunctionHook() {

    override val name = "移除消息表态"
    override val description = "移除消息菜单中的表情回应"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.UI_CHAT_MSG
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_9_0_8)

    override fun initOnce(): Boolean {
        val emoReplyMenuApiImplClass = Initiator.loadClass("com.tencent.qqnt.aio.api.impl.AIOEmoReplyMenuApiImpl")
        val getEmoReplyMenuViewMethod = emoReplyMenuApiImplClass.getDeclaredMethod(
            "getEmoReplyMenuView",
            Context::class.java,
            Initiator.loadClass("com.tencent.mobileqq.aio.msg.AIOMsgItem"),
            Object::class.java,
            View.OnClickListener::class.java
        )
        hookBeforeIfEnabled(getEmoReplyMenuViewMethod) { param ->
            param.result = null
        }
        return true
    }
}