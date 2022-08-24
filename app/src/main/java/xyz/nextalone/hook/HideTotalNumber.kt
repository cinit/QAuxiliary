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
import io.github.qauxv.util.dexkit.DexKit
import xyz.nextalone.util.replace
import xyz.nextalone.util.throwOrTrue

@UiItemAgentEntry
@FunctionHookEntry
object HideTotalNumber : CommonSwitchFunctionHook("na_hide_total_number",
    intArrayOf(DexKit.N_TroopChatPie_showNewTroopMemberCount)) {

    override val name = "隐藏总人数"

    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.CHAT_GROUP_TITLE

    override fun initOnce() = throwOrTrue {
        DexKit.doFindMethod(DexKit.N_TroopChatPie_showNewTroopMemberCount)
            ?.replace(this, false)
    }
}
