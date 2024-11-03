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
import cc.ioctl.util.hookAfterIfEnabled
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.requireRangeQQVersion

@FunctionHookEntry
@UiItemAgentEntry
object HideTroopToDo : CommonSwitchFunctionHook() {

    override val name = "隐藏群待办"
    override val description = "对群聊上方的群待办进行简单隐藏"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.CHAT_GROUP_TITLE
    override val isAvailable = requireRangeQQVersion(QQVersion.QQ_8_9_88, QQVersion.QQ_9_0_68)

    override fun initOnce(): Boolean {
        val tipsBarNewClass: Class<*>
        val reporterClass: Class<*>
        if (requireMinQQVersion(QQVersion.QQ_9_0_25)) {
            tipsBarNewClass = Initiator.loadClass("com.tencent.mobileqq.troop.trooptodo.TroopToDoTipsBarNew")
            reporterClass = Initiator.loadClass("com.tencent.mobileqq.troop.trooptodo.TroopToDoReporter")
        } else {
            tipsBarNewClass = Initiator.loadClass("com.tencent.mobileqq.activity.aio.troop.trooptodo.nt.TroopToDoTipsBarNew")
            reporterClass = Initiator.loadClass("com.tencent.mobileqq.activity.aio.troop.trooptodo.nt.TroopToDoReporter")
        }
        val method = tipsBarNewClass.constructors.single { method ->
            val params = method.parameterTypes
            params.size == 3 && params[0] == Context::class.java && params[2] == reporterClass
        }
        hookAfterIfEnabled(method) { param ->
            val field = tipsBarNewClass.declaredFields.single { field -> field.type == View::class.java }
            field.apply {
                isAccessible = true
                val view = get(param.thisObject) as View
                view.visibility = View.GONE
            }
        }
        return true
    }
}