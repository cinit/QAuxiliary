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
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.Hd_DisableGrowHalfLayer_Method1
import io.github.qauxv.util.dexkit.Hd_DisableGrowHalfLayer_Method2
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.xpcompat.XposedBridge

@FunctionHookEntry
@UiItemAgentEntry
object DisableGrowHalfLayer : CommonSwitchFunctionHook(
    targets = arrayOf(
        Hd_DisableGrowHalfLayer_Method1,
        Hd_DisableGrowHalfLayer_Method2,
    )
) {

    override val name = "屏蔽广告弹窗(测试版)"
    override val description = "屏蔽打开QQ时随缘弹出的(小Q提醒)弹窗"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override fun initOnce(): Boolean {
        hookBeforeIfEnabled(DexKit.requireMethodFromCache(Hd_DisableGrowHalfLayer_Method1)) { param ->
            printStackTrace("屏蔽广告弹窗 -> 测试1")
            Toasts.show("屏蔽广告弹窗 -> 测试1成功")
            param.result = null
        }
        hookBeforeIfEnabled(DexKit.requireMethodFromCache(Hd_DisableGrowHalfLayer_Method2)) { param ->
            printStackTrace("屏蔽广告弹窗 -> 测试2")
            Toasts.show("屏蔽广告弹窗 -> 测试2成功")
            param.result = null
        }
        return true
    }

    private fun printStackTrace(name: String) {
        val stackTrace = Throwable().stackTrace
        val stackTraceStr = stackTrace.joinToString("\n") { element ->
            "at ${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})"
        }
        XposedBridge.log("[$name] -> $stackTraceStr")
    }
}