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

import android.content.Context
import cc.ioctl.util.hookBeforeIfEnabled
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import mqq.app.AppRuntime

@FunctionHookEntry
@UiItemAgentEntry
object ShowQzoneFrame : CommonSwitchFunctionHook() {

    override val name = "显示空间动态"
    override val description = "点击底栏动态Tab时直接显示空间动态"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.MAIN_UI_OPERATION_LOG
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_9_2_30)

    override fun initOnce(): Boolean {
        val lebaPluginInfoClass = Initiator.loadClass("com.tencent.qzonehub.api.impl.QZoneApiProxyImpl")
        val allowMethod = lebaPluginInfoClass.getDeclaredMethod("needShowQzoneFrame", Context::class.java, AppRuntime::class.java)
        hookBeforeIfEnabled(allowMethod) { param ->
            param.result = true
        }
        return true
    }
}
