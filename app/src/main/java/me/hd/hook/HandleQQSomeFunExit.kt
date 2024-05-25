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
import com.tencent.qqnt.kernel.nativeinterface.FileElement
import com.tencent.qqnt.kernel.nativeinterface.TextGiftElement
import de.robv.android.xposed.XposedHelpers
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.Hd_HandleQQSomeFunExit_fixFileView_Method
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.requireMinVersion

@FunctionHookEntry
@UiItemAgentEntry
object HandleQQSomeFunExit : CommonSwitchFunctionHook(
    targets = arrayOf(Hd_HandleQQSomeFunExit_fixFileView_Method)
) {

    override val name = "拦截QQ部分功能闪退"
    override val description = """
        如无特殊情况不建议打开
        1. 拦截群礼物消息闪退
        2. 拦截群文件消息闪退
    """.trimIndent()
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override fun initOnce(): Boolean {
        fixGiftView()
        fixFileView()
        return true
    }

    private fun fixGiftView() {
        val giftViewClass = Initiator.loadClass("com.tencent.mobileqq.vas.gift.TroopGiftView")
        val method = giftViewClass.declaredMethods.single { method ->
            val params = method.parameterTypes
            params.size == 2 && params[0] == Initiator.loadClass("com.tencent.aio.data.a.a")
        }
        hookBeforeIfEnabled(method) { param ->
            val getGiftMethodName = when {
                requireMinVersion(QQVersion.QQ_9_0_56) -> "b2"
                requireMinVersion(QQVersion.QQ_8_9_88) -> "x1"
                else -> "Unknown"
            }
            val textGiftElement = XposedHelpers.callMethod(param.args[0], getGiftMethodName) as TextGiftElement
            if (textGiftElement.paddingTop.isBlank()) {
                XposedHelpers.setObjectField(textGiftElement, "paddingTop", "0")
                Toasts.show("拦截群礼物消息闪退")
            }
        }
    }

    private fun fixFileView() {
        val method = DexKit.requireMethodFromCache(Hd_HandleQQSomeFunExit_fixFileView_Method)
        hookBeforeIfEnabled(method) { param ->
            val file = param.args[0] as FileElement
            if (file.fileSize >= 1073741824 * 99999L) {
                file.fileSize = 1073741824 * 99999L
                Toasts.show("拦截群文件消息闪退")
            }
        }
    }
}