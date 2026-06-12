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

package me.hd.hook.simplify.ui.misc

import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.dexkit.Hd_CustomReportStep_Method
import io.github.qauxv.util.requireMinQQVersion
import me.hd.util.hookBeforeIfEnabled
import me.hd.util.singleField
import me.hd.util.toHostMethod

@FunctionHookEntry
@UiItemAgentEntry
object CustomReportStep : CommonSwitchFunctionHook(
    targets = arrayOf(Hd_CustomReportStep_Method)
) {
    override val name = "篡改上传运动步数"
    override val description = "篡改上传到`QQ运动`的今日步数"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.UI_MISC
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_9_2_30)

    override fun initOnce(): Boolean {
        Hd_CustomReportStep_Method.toHostMethod()
            .hookBeforeIfEnabled(this) { param ->
                val instance = param.thisObject
                instance.singleField {
                    type == Int::class.java
                }.set(instance, 88888)
            }
        return true
    }
}
