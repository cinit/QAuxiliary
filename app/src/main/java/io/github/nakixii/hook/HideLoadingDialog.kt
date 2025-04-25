/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2025 QAuxiliary developers
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

package io.github.nakixii.hook

import cc.ioctl.util.hookBeforeIfEnabled
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinVersion
import xyz.nextalone.util.isPrivate

@FunctionHookEntry
@UiItemAgentEntry
object HideLoadingDialog : CommonSwitchFunctionHook() {
    override val name = "隐藏加载中对话框"
    override val description = "隐藏有时出现的「加载中，请稍候…」对话框"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.CHAT_OTHER
    override val isAvailable = requireMinVersion(QQVersion.QQ_9_1_50)

    override fun initOnce(): Boolean {
        val troopAppsClass = Initiator.loadClass("com.tencent.mobileqq.troop.appscenter.mvi.TroopAppsViewModel")
        val loadTroopMethod = troopAppsClass.declaredMethods.single { method ->
            val params = method.parameterTypes
            method.isPrivate && params.size == 3 && params[1] == Boolean::class.java
        }

        hookBeforeIfEnabled(loadTroopMethod) { param ->
            param.args[1] = false
        }
        return true
    }
}
