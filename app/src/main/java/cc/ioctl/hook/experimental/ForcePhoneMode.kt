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

package cc.ioctl.hook.experimental

import com.github.kyuubiran.ezxhelper.utils.getStaticObject
import com.github.kyuubiran.ezxhelper.utils.hookAfter
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
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object ForcePhoneMode : CommonSwitchFunctionHook(targetProc = SyncUtils.PROC_ANY) {

    override val name = "强制手机模式"
    override val description = "支持 QQ8.9.15 及以上，未经测试，谨慎使用"
    override val extraSearchKeywords: Array<String> = arrayOf("phone")
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY
    override val isApplicationRestartRequired = true
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_15) || requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA)

    override fun initOnce() = throwOrTrue {
        check(isAvailable) { "ForcePhoneMode is not available" }
        val appSettingClass = Initiator.loadClass("com.tencent.common.config.AppSetting")
        appSettingClass.getDeclaredMethod("f").hookAfter {
            val (appIdPhone, appIdPad) = Pair(
                when {
                    requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA) -> "f"
                    requireMinQQVersion(QQVersion.QQ_9_1_50) -> "f"
                    else -> "e"
                },
                when {
                    requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA) -> "g"
                    requireMinQQVersion(QQVersion.QQ_9_1_50) -> "g"
                    else -> "f"
                },
            )
            it.result = appSettingClass.getStaticObject(appIdPhone)
        }
    }

}