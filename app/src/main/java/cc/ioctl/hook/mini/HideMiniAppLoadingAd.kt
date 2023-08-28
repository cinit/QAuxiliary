/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the qwq233 Universal License
 * as published on https://github.com/qwq233/license; either
 * version 2 of the License, or any later version and our EULA as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the qwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package cc.ioctl.hook.mini

import cc.ioctl.util.hookBeforeIfEnabled
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.paramCount
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.TIMVersion
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.requireMinTimVersion

@FunctionHookEntry
@UiItemAgentEntry
object HideMiniAppLoadingAd : CommonSwitchFunctionHook(
    targetProc = SyncUtils.PROC_MAIN or SyncUtils.PROC_MINI
) {

    override val name = "隐藏小程序开屏广告"
    override val description = "未经测试，如果小程序白屏30s以上或闪退请关闭此功能"

    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.UI_MISC

    override val isAvailable: Boolean
        get() = requireMinQQVersion(QQVersion.QQ_8_3_9) || requireMinTimVersion(TIMVersion.TIM_3_5_0)

    override fun initOnce(): Boolean {
        try {
            Initiator.loadClass("com.tencent.mobileqq.mini.helper.MiniAdExposureHelper").findMethod {
                    name == "checkAdExpoFreqAvailable" && paramCount == 0
                }.hookBefore { param ->
                    param.result = false
                }
        } catch (_: Exception) {
            val kMiniLoadingAdManager = Initiator.loadClass("com.tencent.qqmini.sdk.manager.MiniLoadingAdManager")
            val method = kMiniLoadingAdManager.declaredMethods.single { it.name == "updateLoadingAdLayoutAndShow" }
            hookBeforeIfEnabled(method) { param ->
                param.result = null
            }
        }
        return true
    }

}
