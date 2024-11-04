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

import android.view.View
import android.view.ViewGroup
import cc.ioctl.util.hookAfterIfEnabled
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.util.get

@FunctionHookEntry
@UiItemAgentEntry
object HideJoinTroopBriefContent : CommonSwitchFunctionHook() {

    override val name = "隐藏进群介绍及相关效果"
    override val description = "对验证信息页下方的进群介绍布局进行隐藏"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.CHAT_GROUP_OTHER
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_9_0_0)

    override fun initOnce(): Boolean {
        val joinTroopVerifyClass = Initiator.loadClass("com.tencent.mobileqq.activity.JoinTroopVerifyFragment")
        val initViewMethod = joinTroopVerifyClass.getDeclaredMethod("initView", View::class.java)
        hookAfterIfEnabled(initViewMethod) { param ->
            val briefContentLayout = param.thisObject.get("n") as View
            briefContentLayout.visibility = ViewGroup.GONE
        }
        return true
    }
}