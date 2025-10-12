/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2025 QAuxiliary developers
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

package io.github.relimus.hook

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.TIMVersion
import io.github.qauxv.util.requireMinTimVersion
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object HideQZoneAD : CommonSwitchFunctionHook() {
    override val name = "隐藏QQ空间广告"
    override val description = "仅在 TIM_NT 上测试通过"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.MAIN_UI_MISC
    override val isAvailable = requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA)

    override fun initOnce() = throwOrTrue {
        Initiator.loadClass("com.qzone.proxy.feedcomponent.model.gdt.QZoneAdFeedDataExtKt").findMethod {
            name == "isShowingRecommendAd"
        }.hookBefore {
            it.result = true
        }
    }
}