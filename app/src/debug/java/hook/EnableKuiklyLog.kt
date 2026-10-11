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

package hook

import com.github.kyuubiran.ezxhelper.utils.loadClass
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.dsl.uiSwitchPreference
import io.github.qauxv.util.Log
import me.hd.base.KuiklyDelayableHook
import xyz.nextalone.util.hookBefore
import xyz.nextalone.util.method
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object EnableKuiklyLog : KuiklyDelayableHook() {
    override val kuiklyName: String
        get() = ".apk"

    override val uiItemAgent = uiSwitchPreference {
        title = "输出 KLog 到 NADump"
    }

    override val uiItemLocation = FunctionEntryRouter.Locations.DebugCategory.DEBUG_CATEGORY

    private const val METHOD_LOG_INFO = "logInfo"
    private const val METHOD_LOG_DEBUG = "logDebug"
    private const val METHOD_LOG_ERROR = "logError"

    override fun startHook(classLoader: ClassLoader) = throwOrTrue {
        loadClass("com.tencent.kuikly.core.log.KLog", classLoader).method { m ->
            m.name == "logToNative" && m.parameterCount == 2
        }?.hookBefore(this) { param ->
            val method = param.args[0] as String
            val msg = param.args[1] as String
            when (method) {
                METHOD_LOG_INFO -> Log.i("[NADump]$msg")
                METHOD_LOG_DEBUG -> Log.d("[NADump]$msg")
                METHOD_LOG_ERROR -> Log.e("[NADump]$msg")
            }
        }
    }
}
