/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
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

package io.github.moonleeeaf.hook

import cc.hicore.QApp.QAppUtils
import cc.ioctl.util.HookUtils
import cc.ioctl.util.Reflex
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.dexkit.CopyPromptHelper_handlePrompt
import io.github.qauxv.util.dexkit.DexKit
import xyz.nextalone.util.get
import xyz.nextalone.util.set

// 参考滞空方法模板: TimRemoveToastTips.kt
@FunctionHookEntry
@UiItemAgentEntry
object FxxkPasteHereForTIM : CommonSwitchFunctionHook(
    arrayOf(CopyPromptHelper_handlePrompt)
) {

    override val name = "移除聊天输入框“点击粘贴”"
    override val description = "仅在 TIM 3.5.1 测试通过";
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY

    override fun initOnce(): Boolean {
        // 查找字符串参考 https://github.com/cinit/QAuxiliary/issues/890
        // TIM [x, y], x in (3.0.0, 3.1.0], y unknown
        // QQ [x, y], x in (8.0.0, 8.1.0], y in [9.0.8.14755_5540, QQ_9.0.15.14880_5590)
        HookUtils.hookBeforeIfEnabled(
            this, DexKit.requireMethodFromCache(CopyPromptHelper_handlePrompt)
        ) {
            it.result = null;
        }
        return true;
    }

}
