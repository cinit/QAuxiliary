/*
 * QNotified - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 dmca@ioctl.cc
 * https://github.com/ferredoxin/QNotified
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/ferredoxin/QNotified/blob/master/LICENSE.md>.
 */
package me.singleneuron.hook

import android.app.Activity
import android.content.Intent
import android.net.Uri
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedBridge
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Log
import me.singleneuron.util.NoAppletUtil
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object NoApplet : CommonSwitchFunctionHook() {

    override val name = "小程序分享转链接（发送）"
    override val description = "感谢Alcatraz323开发的远离小程序，由神经元移植到Xposed"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY

    override fun initOnce() = throwOrTrue {
        //val jumpActivityClass = Class.forName("com.tencent.mobileqq.activity.JumpActivity")
        XposedBridge.hookAllMethods(
                Activity::class.java,
                "getIntent",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (param.thisObject::class.java.simpleName != "JumpActivity") return
                        //Log.d("NoApplet started: "+param.thisObject::class.java.simpleName)
                        val originIntent = param.result as Intent
                        /*Log.d("NoApplet getIntent: $originIntent")
                        Log.d("NoApplet getExtra: ${originIntent.extras}")*/
                        val originUri = originIntent.data
                        val schemeUri = originUri.toString()
                        if (!schemeUri.contains("mini_program")) return
                        Log.d("transfer applet intent: $schemeUri")
                        val processScheme = NoAppletUtil.removeMiniProgramNode(schemeUri)
                        val newScheme = NoAppletUtil.replace(processScheme, "req_type", "MQ==")
                        val newUri = Uri.parse(newScheme)
                        originIntent.data = newUri
                        originIntent.component = null
                        param.result = originIntent
                    }
                })
    }
}
