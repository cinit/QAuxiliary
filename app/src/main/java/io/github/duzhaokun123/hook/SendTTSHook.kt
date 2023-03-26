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

package io.github.duzhaokun123.hook

import android.content.Context
import android.os.Parcelable
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.EditText
import cc.ioctl.util.HostInfo
import io.github.duzhaokun123.util.TTS
import io.github.qauxv.base.IDynamicHook
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.router.decorator.BaseSwitchFunctionDecorator
import io.github.qauxv.router.decorator.IInputButtonDecorator
import io.github.qauxv.router.dispacher.InputButtonHookDispatcher
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.dexkit.CArkAppItemBubbleBuilder
import io.github.qauxv.util.dexkit.CFaceDe
import io.github.qauxv.util.dexkit.NBaseChatPie_init
import mqq.app.AppRuntime

@UiItemAgentEntry
@FunctionHookEntry
object SendTTSHook :
    BaseSwitchFunctionDecorator(
        "qn_send_tts", false,
        arrayOf(
            CArkAppItemBubbleBuilder,
            CFaceDe,
            NBaseChatPie_init,
        )
    ), IInputButtonDecorator {

    override val name: String
        get() = "文字转语音发送 (使用系统 TTS)"

    override val description: String
        get() = "用法 (长按\"发送\"发送)\n" +
            "#tts\n" +
            "[text]\n" +
            "\n" +
            "Example:\n" +
            "#tts\n" +
            "你好"

    override val dispatcher: IDynamicHook
        get() = InputButtonHookDispatcher.INSTANCE

    override fun initOnce(): Boolean {
        super.initOnce()
        TTS.addInitCallback {
            if (it == TextToSpeech.ERROR) {
                Toasts.error(HostInfo.getApplication(), "TTS 初始化失败")
                traceError(RuntimeException("TTS init failed"))
            }
        }
        TTS.init(HostInfo.getApplication())
        return true
    }

    override val uiItemLocation: Array<String>
        get() = FunctionEntryRouter.Locations.Entertainment.ENTERTAIN_CATEGORY

    override fun onFunBtnLongClick(text: String, session: Parcelable, input: EditText, sendBtn: View, ctx1: Context, qqApp: AppRuntime): Boolean {
        if (isEnabled.not()) return false
        val wc = CommonContextWrapper.createAppCompatContext(ctx1)
        val lines = text.split("\n")
        val line1 = lines.getOrNull(0) ?: ""
        if (line1 != "#tts") return false
        val toSend = lines.slice(1 until lines.size).joinToString("\n")
        TTS.showSendDialog(wc, toSend, session, input, qqApp)
        return true
    }
}
