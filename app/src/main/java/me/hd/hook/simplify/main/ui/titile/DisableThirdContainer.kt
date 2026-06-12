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

package me.hd.hook.simplify.main.ui.titile

import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import me.hd.util.hookBeforeIfEnabled
import me.hd.util.parameters
import me.hd.util.singleMethod
import me.hd.util.toHostClass

@FunctionHookEntry
@UiItemAgentEntry
object DisableThirdContainer : CommonSwitchFunctionHook() {
    override val name = "屏蔽悬浮广告"
    override val description = "屏蔽消息页右上角 24年08月 开学季悬浮广告"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.MAIN_UI_TITLE
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override fun initOnce(): Boolean {
        when {
            requireMinQQVersion(QQVersion.QQ_9_0_60) -> "com.tencent.qqnt.chats.core.ui.third.ThirdContainer"
            else -> "com.tencent.qqnt.chats.core.ui.d.e"
        }.toHostClass().singleMethod {
            parameters(List::class.java)
        }.hookBeforeIfEnabled(this) { param ->
            param.args[0] = emptyList<Any>()
        }

        if (requireMinQQVersion(QQVersion.QQ_9_0_75)) {
            "cooperation.vip.ad.TianshuNewAdManager".toHostClass()
                .singleMethod {
                    parameters("com.tencent.mobileqq.activity.home.chats.biz.tianshu.TianShuConversationThirdView".toHostClass())
                }.hookBeforeIfEnabled(this) { param ->
                    param.result = null
                }
        }
        return true
    }
}
