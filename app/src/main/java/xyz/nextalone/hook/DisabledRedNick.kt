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
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.NFriendChatPie_updateUITitle
import io.github.qauxv.util.dexkit.NVipUtils_getUserStatus
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.hookBefore
import xyz.nextalone.util.isSimpleUi

@FunctionHookEntry
@UiItemAgentEntry
object DisabledRedNick : CommonSwitchFunctionHook(
    "na_disable_red_nick_kt",
    arrayOf(NFriendChatPie_updateUITitle, NVipUtils_getUserStatus)
) {
    private var updating = false
    override val name = "隐藏会员红名"

    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.UI_MISC

    override fun initOnce(): Boolean {
        DexKit.requireMethodFromCache(NVipUtils_getUserStatus).hookBefore(this) {
            if (updating && !isSimpleUi) {
                it.result = -1
            }
        }
        DexKit.requireMethodFromCache(NFriendChatPie_updateUITitle).hookBefore(this) {
            updating = true
        }
        DexKit.requireMethodFromCache(NFriendChatPie_updateUITitle).hookAfter(this) {
            updating = false
        }
        return true
    }

    override val isAvailable: Boolean get() = requireMinQQVersion(QQVersion.QQ_8_5_5)
}
