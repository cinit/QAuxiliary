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

package me.singleneuron.hook

import cc.ioctl.util.Reflex
import cc.ioctl.util.beforeHookIfEnabled
import io.github.qauxv.util.xpcompat.XposedBridge
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.EmotcationConstants


@UiItemAgentEntry
@FunctionHookEntry
object SystemEmoji : CommonSwitchFunctionHook(
    targets = arrayOf(EmotcationConstants),
    defaultEnabled = true
) {

    override val name: String = "强制使用系统Emoji"

    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Simplify.UI_CHAT_MSG

    override fun initOnce(): Boolean {
        // com.tencent.mobileqq.text.EmotcationConstants.getSingleEmoji(II)I
        // com.tencent.mobileqq.text.EmotcationConstants.getDoubleEmoji(I)I
        val r = beforeHookIfEnabled { param ->
            // this will be called before the clock was updated by the original
            // method
            param.result = -1
        }
        val kEmotcationConstants = DexKit.requireClassFromCache(EmotcationConstants)
        val getSingleEmoji = Reflex.findSingleMethod(kEmotcationConstants, Integer.TYPE, false, Integer.TYPE)
        val getDoubleEmoji = Reflex.findSingleMethod(kEmotcationConstants, Integer.TYPE, false, Integer.TYPE, Integer.TYPE)
        XposedBridge.hookMethod(getSingleEmoji, r)
        XposedBridge.hookMethod(getDoubleEmoji, r)
        return true
    }
}
