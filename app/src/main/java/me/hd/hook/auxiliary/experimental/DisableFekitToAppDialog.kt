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

package me.hd.hook.auxiliary.experimental

import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.dexkit.Hd_DisableFekitToAppDialog_Method
import io.github.qauxv.util.requireMinQQVersion
import me.hd.util.hookBeforeIfEnabled
import me.hd.util.toHostMethod

@FunctionHookEntry
@UiItemAgentEntry
object DisableFekitToAppDialog : CommonSwitchFunctionHook(
    targets = arrayOf(Hd_DisableFekitToAppDialog_Method)
) {
    override val name = "屏蔽社交功能限制提醒"
    override val description = "屏蔽每次打开QQ弹出提醒对话框"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_9_0_0)

    override fun initOnce(): Boolean {
        Hd_DisableFekitToAppDialog_Method.toHostMethod()
            .hookBeforeIfEnabled(this) { param ->
                val string = param.args[0] as String
                if ("socialError" == string) {
                    param.result = null
                }
            }
        return true
    }
}
