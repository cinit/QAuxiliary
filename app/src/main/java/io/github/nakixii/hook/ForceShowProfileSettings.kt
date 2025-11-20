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

package io.github.nakixii.hook

import com.github.kyuubiran.ezxhelper.utils.hookBefore
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.util.method

@FunctionHookEntry
@UiItemAgentEntry
object ForceShowProfileSettings : CommonSwitchFunctionHook() {
    override val name = "强制显示资料卡内所有设置"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.PROFILE_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_9_1_70)

    override fun initOnce(): Boolean {
        Initiator.loadClass("com.tencent.mobileqq.profilesetting.api.impl.ProfileSettingApiImpl")
            .method("getProfileDisplaySettingStateFromCard")?.hookBefore { it.result = 0 }

        return true
    }
}
