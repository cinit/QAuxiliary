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

package me.hd.hook

import android.view.View
import cc.ioctl.util.hookAfterIfEnabled
import com.github.kyuubiran.ezxhelper.utils.setViewZeroSize
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.Hd_HideEmoReplyLayout_Method
import io.github.qauxv.util.requireMinQQVersion

@FunctionHookEntry
@UiItemAgentEntry
object HideEmoReplyLayout : CommonSwitchFunctionHook(
    targets = arrayOf(Hd_HideEmoReplyLayout_Method)
) {

    override val name = "隐藏表情表态"
    override val description = "隐藏消息底部的表情回应布局"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.UI_CHAT_MSG
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_9_1_35)

    override fun initOnce(): Boolean {
        hookAfterIfEnabled(DexKit.requireMethodFromCache(Hd_HideEmoReplyLayout_Method)) { param ->
            val layout = param.result as View
            layout.setViewZeroSize()
        }
        return true
    }
}