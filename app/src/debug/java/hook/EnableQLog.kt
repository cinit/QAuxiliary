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

package hook

import com.github.kyuubiran.ezxhelper.utils.loadClass
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.replace
import xyz.nextalone.util.throwOrTrue
import java.lang.reflect.Method

@FunctionHookEntry
@UiItemAgentEntry
object EnableQLog : CommonSwitchFunctionHook() {

    override val name = "输出 QLog 到 NADump"

    override val uiItemLocation = FunctionEntryRouter.Locations.DebugCategory.DEBUG_CATEGORY

    override fun initOnce() = throwOrTrue {
        for (m: Method in loadClass("com.tencent.qphone.base.util.QLog").declaredMethods) {
            val argt = m.parameterTypes
            if (m.name == "isDevelopLevel" && argt.isEmpty()) {
                m.replace(this, true)
            }
            if (m.name == "isColorLevel" && argt.isEmpty()) {
                m.replace(this, true)
            }
            if (m.name == "getTag" && argt.size == 1 && argt[0] == String::class.java) {
                m.hookAfter(this) {
                    val tag = it.args[0]
                    it.result = "NADump:$tag"
                }
            }
        }
    }
}
