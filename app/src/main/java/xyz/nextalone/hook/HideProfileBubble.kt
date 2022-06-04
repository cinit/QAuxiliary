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
package xyz.nextalone.hook

import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.DexKit
import io.github.qauxv.util.PlayQQVersion.PlayQQ_8_2_9
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinPlayQQVersion
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.util.replace
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object HideProfileBubble : CommonSwitchFunctionHook(intArrayOf(DexKit.N_QQSettingMe_updateProfileBubble)) {

    override val name = "隐藏资料卡气泡"
    override val description = "空间点赞提醒"

    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.UI_PROFILE

    override fun initOnce() = throwOrTrue {
        DexKit.doFindMethod(DexKit.N_QQSettingMe_updateProfileBubble)!!.replace(this, null)
    }

    override val isAvailable: Boolean
        get() = requireMinQQVersion(QQVersion.QQ_8_3_6) or requireMinPlayQQVersion(PlayQQ_8_2_9)
}
