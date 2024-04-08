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

package io.github.fusumayuki.hook

import android.view.View
import cc.ioctl.util.HookUtils.hookBeforeIfEnabled
import com.alphi.qhmk.module.HiddenVipIconForSe
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion

@FunctionHookEntry
@UiItemAgentEntry
object HideQQValue : CommonSwitchFunctionHook() {

    override val name = "隐藏QQ能量值"

    override val description = "隐藏侧滑栏与资料卡上的QQ能量值"

    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Simplify.SLIDING_UI

    private val qqValueMethodName = when {
        requireMinQQVersion(QQVersion.QQ_9_0_30) -> "z"
        requireMinQQVersion(QQVersion.QQ_9_0_0) -> "y"
        else -> "x"
    }

    override fun initOnce(): Boolean {
        val qqValueClass = Initiator.loadClass("com.tencent.mobileqq.vas.qqvaluecard.view.QQValuePagView;")
        val qqValueMethod = qqValueClass.getDeclaredMethod(
            qqValueMethodName,
            String::class.java,
            String::class.java,
            Boolean::class.javaPrimitiveType
        )

        hookBeforeIfEnabled(this, qqValueMethod) { param: MethodHookParam ->
            val obj = param.thisObject as View
            obj.isClickable = false
            param.setResult(null)
        }

        if (!HiddenVipIconForSe.INSTANCE.isEnabled)
            HiddenVipIconForSe.INSTANCE.optimizeQLevel(10, this)
        return true
    }

}
