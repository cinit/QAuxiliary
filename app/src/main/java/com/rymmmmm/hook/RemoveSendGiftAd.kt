/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */
package com.rymmmmm.hook

import io.github.qauxv.SyncUtils
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import xyz.nextalone.util.hookBefore
import xyz.nextalone.util.isStatic
import xyz.nextalone.util.set
import xyz.nextalone.util.throwOrTrue

//去除群聊送礼物广告
@FunctionHookEntry
@UiItemAgentEntry
object RemoveSendGiftAd : CommonSwitchFunctionHook(SyncUtils.PROC_ANY) {

    override val name = "免广告送免费礼物"
    override val description = "[仅限群聊送礼物]若失效请使用屏蔽小程序广告"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY

    public override fun initOnce() = throwOrTrue {
        val troopGiftPanel = Initiator
            .load("com.tencent.biz.troopgift.TroopGiftPanel")
        for (m in troopGiftPanel.declaredMethods) {
            val argt = m.parameterTypes
            if (m.name == "onClick" && argt.size == 1 && !m.isStatic) {
                m.hookBefore(this) {
                    it.thisObject.set("f", java.lang.Boolean.TYPE, true)
                }
            }
        }
    }
}
