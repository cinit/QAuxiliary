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

package wang.allenyou.hook

import android.annotation.SuppressLint
import android.graphics.Rect
import android.text.method.TransformationMethod
import android.view.View
import android.widget.TextView
import cc.ioctl.util.hookBeforeIfEnabled
import com.github.kyuubiran.ezxhelper.utils.hookAllConstructorAfter
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion

@FunctionHookEntry
@UiItemAgentEntry
object DisableUtfControlCharacterInNicknameHook : CommonSwitchFunctionHook() {

    override val name = "过滤聊天中的 UTF-8 特殊字符"

    override val description = "将聊天中出现的特殊字符替换为空格"

    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Simplify.CHAT_OTHER

    override val isAvailable: Boolean get() = requireMinQQVersion(QQVersion.QQ_9_0_20)

    private class HookTransformationMethod(val originalMethod: TransformationMethod) : TransformationMethod {
        override fun getTransformation(source: CharSequence?, view: View?): CharSequence {
            if (source == null) {
                return originalMethod.getTransformation(null, view)
            }
            return originalMethod.getTransformation(filterControlCharacter(source), view)
        }

        override fun onFocusChanged(view: View?, sourceText: CharSequence?, focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
            originalMethod.onFocusChanged(view, sourceText, focused, direction, previouslyFocusedRect)
        }
    }

    @SuppressLint("BidiSpoofing")
    private const val BLACKLIST = "‭‮‪‫‎⁦⁧‏"

    override fun initOnce(): Boolean {
        val textViewClass = Initiator.loadClass("android.widget.TextView")
        val setTransformationMethod = textViewClass.getDeclaredMethod("setTransformationMethod", TransformationMethod::class.java)
        hookBeforeIfEnabled(setTransformationMethod) {
            it.args[0] = HookTransformationMethod(it.args[0] as TransformationMethod)
        }
        textViewClass.hookAllConstructorAfter {
            val original = (it.thisObject as TextView).transformationMethod
            if (original != null) {
                (it.thisObject as TextView).transformationMethod = HookTransformationMethod(original)
            }
        }
        return true
    }

    private fun filterControlCharacter(str: CharSequence): CharSequence {
        var ret = str.toString()
        BLACKLIST.forEach { ret = ret.replace(it, ' ') }
        return ret
    }
}