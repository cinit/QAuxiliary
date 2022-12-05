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
package cc.ioctl.hook.troop

import android.text.Spannable
import android.widget.TextView
import cc.ioctl.util.HookUtils
import com.tencent.mobileqq.app.QQAppInterface
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import xyz.nextalone.util.isStatic

//屏蔽群聊炫彩昵称
@FunctionHookEntry
@UiItemAgentEntry
object DisableColorNickName : CommonSwitchFunctionHook("rq_disable_color_nick_name") {

    override val name = "屏蔽群聊炫彩昵称"
    override val description = "可能导致聊天页面滑动卡顿"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.CHAT_GROUP_OTHER

    public override fun initOnce(): Boolean {
        // public static void ColorNickManager.?(QQAppInterface, TextView, Spannable)
        // public static void ColorNickManager.?(QQAppInterface, TextView, Spannable, int, boolean)
        val methods = Initiator._ColorNickManager().declaredMethods.filter { m ->
            if (m.isStatic && m.returnType == Void::class.javaPrimitiveType) {
                val argt = m.parameterTypes
                argt.size >= 3 && argt[0] == QQAppInterface::class.java &&
                    argt[1] == TextView::class.java && argt[2] == Spannable::class.java
            } else false
        }
        check(methods.isNotEmpty()) { "ColorNickManager.?(QQAppInterface, TextView, Spannable, ...) not found" }
        check(methods.size <= 2) {
            "too many ColorNickManager.?(QQAppInterface, TextView, Spannable, ...) found, got ${methods.size}"
        }
        methods.forEach { method ->
            HookUtils.hookBeforeIfEnabled(this, method) { param -> param.result = null }
        }
        return true
    }

}
