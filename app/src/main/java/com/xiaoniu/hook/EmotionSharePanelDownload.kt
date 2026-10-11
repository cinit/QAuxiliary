/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2026 QAuxiliary developers
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

package com.xiaoniu.hook

import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.EmotionDetailAi
import io.github.qauxv.util.dexkit.EmotionDownloadDisableSwitch
import io.github.qauxv.util.requireMinQQVersion

@FunctionHookEntry
@UiItemAgentEntry
object EmotionSharePanelDownload : CommonSwitchFunctionHook(
    targets = arrayOf(EmotionDetailAi, EmotionDownloadDisableSwitch)
) {
    override val name = "表情分享菜单允许保存图片"
    override val description = "表情详情页右上角菜单中显示保存到手机选项"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_80)

    override fun initOnce(): Boolean {
        if (requireMinQQVersion(QQVersion.QQ_9_2_30)) {
            DexKit.requireMethodFromCache(EmotionDetailAi).hookReturnConstant(true)
        } else {
            DexKit.requireMethodFromCache(EmotionDownloadDisableSwitch).hookReturnConstant(false)
        }
        return true
    }
}
