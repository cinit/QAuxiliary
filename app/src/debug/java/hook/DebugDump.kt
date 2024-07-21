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

import android.content.Intent
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.router.decorator.BaseSwitchFunctionDecorator
import io.github.qauxv.router.decorator.IStartActivityHookDecorator
import io.github.qauxv.router.dispacher.StartActivityHook
import io.github.qauxv.util.Log
import me.singleneuron.util.dump

@UiItemAgentEntry
@FunctionHookEntry
object DebugDump : BaseSwitchFunctionDecorator(), IStartActivityHookDecorator {

    override fun onStartActivityIntent(intent: Intent, param: XC_MethodHook.MethodHookParam): Boolean {
        // don't mess up with the log output if all functions are enable for unit tests
        if (!isEnabledAllFunction()) {
            Log.d("debugDump: startActivity, this=" + param.thisObject)
            intent.dump()
        }
        return false
    }

    private fun isEnabledAllFunction(): Boolean {
        return ConfigManager.getDefaultConfig().getBooleanOrDefault("EnableAllHook.enabled", false)
    }

    override val name = "Activity堆栈转储"
    override val description = "没事别开"
    override val dispatcher = StartActivityHook

    override val uiItemLocation = FunctionEntryRouter.Locations.DebugCategory.DEBUG_CATEGORY

    /*override fun doInit(): Boolean {
        //dump setResult
        XposedBridge.hookAllMethods(
            Activity::class.java,
            "setResult",
            object : XposedMethodHookAdapter() {
                override fun beforeMethod(param: MethodHookParam?) {
                    if (param!!.args.size != 2) return
                    val intent = param.args[1] as Intent
                    Log.d("debugDump: setResult " + param.thisObject::class.java.name)
                    intent.dump()
                }
            })
        return true
    }*/
}
