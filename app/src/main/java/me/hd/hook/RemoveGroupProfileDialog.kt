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

import android.os.Bundle
import cc.ioctl.util.hookBeforeIfEnabled
import de.robv.android.xposed.XposedHelpers
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion

@FunctionHookEntry
@UiItemAgentEntry
object RemoveGroupProfileDialog : CommonSwitchFunctionHook() {

    override val name = "移除群成员资料卡异常弹窗"
    override val description = "忽略账号异常状态, 使其可以正常查看"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.PROFILE_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override fun initOnce(): Boolean {
        val profileSecureClass = Initiator.loadClass("com.tencent.mobileqq.profilecard.processor.ProfileSecureProcessor")
        val respHeadClass = Initiator.loadClass("SummaryCard.RespHead")
        val respSummaryCardClass = Initiator.loadClass("SummaryCard.RespSummaryCard")
        val profileCardMethod = profileSecureClass.declaredMethods.single { method ->
            val params = method.parameterTypes
            method.name == "processProfileCard" && (if (requireMinQQVersion(QQVersion.QQ_9_0_0)) {
                params.size == 3 && params[0] == Bundle::class.java && params[1] == respHeadClass && params[2] == respSummaryCardClass
            } else {
                params.size == 2 && params[0] == respHeadClass && params[1] == respSummaryCardClass
            })
        }
        hookBeforeIfEnabled(profileCardMethod) { param ->
            val respHead = if (requireMinQQVersion(QQVersion.QQ_9_0_0)) param.args[1] else param.args[0]
            val iResult = XposedHelpers.getObjectField(respHead, "iResult")
            if (iResult == 201 || iResult == 202) {
                XposedHelpers.setObjectField(respHead, "iResult", 0)
            }
        }
        return true
    }
}