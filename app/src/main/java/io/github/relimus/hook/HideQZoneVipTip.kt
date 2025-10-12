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

import com.github.kyuubiran.ezxhelper.utils.hookAllConstructorAfter
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
object HideQZoneVipTip : CommonSwitchFunctionHook() {
    override val name = "隐藏QQ空间VIP"
    override val description = "隐藏空间中头像下边的的\"开通VIP\", 会导致右侧小眼睛动画消失 (介意勿用)"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.MAIN_UI_MISC
    override val isAvailable = requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA)

    override fun initOnce() = throwOrTrue {
        Initiator.loadClass("com.qzone.reborn.feedx.widget.header.QZoneFeedxHeaderVipElement").let {
            it.hookAllConstructorAfter { param ->
                param.thisObject.javaClass.declaredFields.first { field ->
                    field.name == "h"
                }.apply { isAccessible = true }.set(param.thisObject, null)
            }
        }
        Initiator.loadClass("com.qzone.reborn.feedx.widget.header.ax").let {
            it.hookAllConstructorAfter { param ->
                param.thisObject.javaClass.declaredFields.first { field ->
                    field.name == "h"
                }.apply { isAccessible = true }.set(param.thisObject, null)
            }
        }
    }
}