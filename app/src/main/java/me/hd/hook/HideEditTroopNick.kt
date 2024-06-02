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

import android.view.ViewGroup
import android.widget.EditText
import cc.ioctl.util.hookAfterIfEnabled
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion

@FunctionHookEntry
@UiItemAgentEntry
object HideEditTroopNick : CommonSwitchFunctionHook() {

    override val name = "隐藏编辑群昵称装扮"
    override val description = "对编辑群昵称界面的装扮布局进行简单隐藏"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.CHAT_GROUP_OTHER
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override fun initOnce(): Boolean {
        val editTroopNickClass = Initiator.loadClass("com.tencent.mobileqq.activity.editservice.EditTroopMemberNickService")
        val initUiMethod = editTroopNickClass.getDeclaredMethod("c", ViewGroup::class.java, EditText::class.java, ViewGroup::class.java)
        hookAfterIfEnabled(initUiMethod) { param ->
            val view = param.args[2] as ViewGroup
            view.visibility = ViewGroup.GONE
        }
        return true
    }
}