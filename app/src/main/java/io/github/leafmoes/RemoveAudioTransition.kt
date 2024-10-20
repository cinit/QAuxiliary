/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2024 QAuxiliary developers
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

package io.github.leafmoes

import com.github.kyuubiran.ezxhelper.utils.hookReplace
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.RemoveAudioTransitionMethod
import xyz.nextalone.util.throwOrTrue


@FunctionHookEntry
@UiItemAgentEntry
object RemoveAudioTransition : CommonSwitchFunctionHook(
    "removeAudioTransition",
    targets = arrayOf(RemoveAudioTransitionMethod)
) {
    override val name: String get() = "移除语音面板多余过渡动画"
    override val description: String get() = "QQ语音面板左右滑动的时候因为这个动画导致UI重影\n故写此功能移除这个莫名其妙的动画"
    override val uiItemLocation: Array<String> get() = FunctionEntryRouter.Locations.Simplify.CHAT_OTHER

    override fun initOnce() = throwOrTrue {
        DexKit.requireMethodFromCache(RemoveAudioTransitionMethod).hookReplace { }
    }
}