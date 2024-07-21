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
import android.view.ViewStub
import cc.ioctl.util.HookUtils.hookBeforeIfEnabled
import io.github.qauxv.util.xpcompat.XC_MethodHook.MethodHookParam
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator

@FunctionHookEntry
@UiItemAgentEntry
object HideQZoneGuidePublishMoodBanner : CommonSwitchFunctionHook() {

    override val name = "隐藏QQ空间动态顶部横幅"

    override val description = "隐藏QQ空间动态顶部每日更新的横幅"

    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Simplify.UI_MISC

    private val qZoneGuidePublishMoodBannerMethodName = "h"

    override fun initOnce(): Boolean {
        val qZoneGuidePublishMoodBannerClass = Initiator.loadClass("com.qzone.component.banner.QZoneGuidePublishMoodBanner;")
        val qZoneGuidePublishMoodBannerMethod = qZoneGuidePublishMoodBannerClass.getDeclaredMethod(
            qZoneGuidePublishMoodBannerMethodName,
            ViewStub::class.java
        )

        hookBeforeIfEnabled(this, qZoneGuidePublishMoodBannerMethod) { param: MethodHookParam ->
            val obj = param.thisObject as View
            obj.isClickable = false
            param.setResult(null)
        }
        return true
    }

}