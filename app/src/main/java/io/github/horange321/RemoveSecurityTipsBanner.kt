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

package io.github.horange321

import com.github.kyuubiran.ezxhelper.utils.hookReplace
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.dexkit.DexKit.requireMethodFromCache
import io.github.qauxv.util.dexkit.RemoveSecurityTipsBanner_Method


@FunctionHookEntry
@UiItemAgentEntry
object RemoveSecurityTipsBanner :
    CommonSwitchFunctionHook("removeSecurityTipsBanner", arrayOf(RemoveSecurityTipsBanner_Method)) {
    override val name = "隐藏群聊风险提醒"
    override val uiItemLocation: Array<String> = Simplify.CHAT_GROUP_TITLE
    override val description = "移除群聊顶部风险提醒"


    override fun initOnce(): Boolean {
        requireMethodFromCache(RemoveSecurityTipsBanner_Method).hookReplace { }
        return true
    }
}