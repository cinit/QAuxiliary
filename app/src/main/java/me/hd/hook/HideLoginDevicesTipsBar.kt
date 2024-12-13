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

import cc.ioctl.util.hookBeforeIfEnabled
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.Hd_HideTipsBar_Method
import io.github.qauxv.util.requireMinQQVersion

@FunctionHookEntry
@UiItemAgentEntry
object HideLoginDevicesTipsBar : CommonSwitchFunctionHook(
    targets = arrayOf(Hd_HideTipsBar_Method)
) {

    override val name = "隐藏登录设备(已登录 Windows、Pad)提示"
    override val extraSearchKeywords = arrayOf("Windows", "Pad")
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.MAIN_UI_TITLE
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override fun initOnce(): Boolean {
        hookBeforeIfEnabled(DexKit.requireMethodFromCache(Hd_HideTipsBar_Method)) { param ->
            param.result = null
        }
        return true
    }
}