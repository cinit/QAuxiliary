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

import android.app.Activity
import android.content.Context
import android.speech.tts.TextToSpeech
import android.view.View
import cc.ioctl.util.HostInfo
import cc.ioctl.util.Reflex
import cc.ioctl.util.afterHookIfEnabled
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.qauxv.R
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.CustomMenu
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Toasts

@FunctionHookEntry
@UiItemAgentEntry
object MessageTTSHook: CommonSwitchFunctionHook() {
    lateinit var textToSpeech: TextToSpeech

    override val name: String
        get() = "文字消息转语言 (使用系统 TTS)"

    override val description: String
        get() = "提示失败多半是没设置系统 TTS 引擎"

    override fun initOnce(): Boolean {
        val cl_ArkAppItemBuilder = Initiator._TextItemBuilder()
        XposedHelpers.findAndHookMethod(cl_ArkAppItemBuilder, "a", Int::class.javaPrimitiveType, Context::class.java,
            Initiator.load("com/tencent/mobileqq/data/ChatMessage"), menuItemClickCallback
        )
        for (m in cl_ArkAppItemBuilder!!.declaredMethods) {
            if (!m.returnType.isArray) {
                continue
            }
            val ps = m.parameterTypes
            if (ps.size == 1 && ps[0] == View::class.java) {
                XposedBridge.hookMethod(m, getMenuItemCallBack)
                break
            }
        }
        textToSpeech = TextToSpeech(HostInfo.getApplication()) {
            if (it == TextToSpeech.ERROR) {
                Toasts.error(HostInfo.getApplication(), "TTS 初始化失败")
                traceError(RuntimeException("TTS init failed"))
            }
        }
        return true
    }

    override val uiItemLocation: Array<String>
        get() = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY

    private val getMenuItemCallBack = afterHookIfEnabled(60) { param ->
       try {
            val arr = param.result
            val clQQCustomMenuItem = arr.javaClass.componentType
            val item_tts = CustomMenu.createItem(clQQCustomMenuItem, R.id.item_tts, "TTS")
            val ret = java.lang.reflect.Array.newInstance(clQQCustomMenuItem!!, java.lang.reflect.Array.getLength(arr) + 1)
            System.arraycopy(arr, 0, ret, 0, java.lang.reflect.Array.getLength(arr))
            java.lang.reflect.Array.set(ret, java.lang.reflect.Array.getLength(arr), item_tts)
            param.result = ret
        } catch (e: Throwable) {
            traceError(e)
            throw e
        }
    }

    private val menuItemClickCallback = afterHookIfEnabled(60) { param ->
        val id = param.args[0] as Int
        val ctx = param.args[1] as Activity
        val chatMessage = param.args[2]
        if (id != R.id.item_tts) return@afterHookIfEnabled
        val r = textToSpeech.speak(Reflex.getInstanceObjectOrNull(chatMessage, "msg")?.toString() ?: "", TextToSpeech.QUEUE_FLUSH, null)
        if (r == TextToSpeech.ERROR) {
            Toasts.error(ctx, "TTS 请求失败")
        }

    }

}
