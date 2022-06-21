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

package me.kyuubiran.hook

import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import com.github.kyuubiran.ezxhelper.utils.emptyParam
import com.github.kyuubiran.ezxhelper.utils.findAllMethods
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import me.kyuubiran.util.ClassHelper

@FunctionHookEntry
@UiItemAgentEntry
object AntiUpdate : CommonSwitchFunctionHook() {
    override val name: String = "屏蔽更新"

    override val description: CharSequence by lazy {
        SpannableStringBuilder().apply {
            // red color
            val text = "可能导致随机性闪退"
            val color = 0xFFFF5252u.toInt()
            val start = 0
            val end = text.length
            append(text)
            setSpan(
                ForegroundColorSpan(color),
                start,
                end,
                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    override fun initOnce(): Boolean {
        val clz = ClassHelper.UpgradeController1.clz
            ?: ClassHelper.UpgradeController2.clz
            ?: throw ClassNotFoundException("UpgradeController")

        clz.findAllMethods {
            emptyParam && returnType.name.contains("UpgradeDetailWrapper") && name.length == 1
        }.hookBefore { if (isEnabled) it.result = null }
        return true
    }

    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Simplify.UI_MISC
}
