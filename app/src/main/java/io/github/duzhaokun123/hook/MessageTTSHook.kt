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
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import cc.ioctl.util.HostInfo
import cc.ioctl.util.Reflex
import cc.ioctl.util.afterHookIfEnabled
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.duzhaokun123.util.TTS
import io.github.qauxv.R
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.databinding.Tts2DialogBinding
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.CustomMenu
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Toasts

@FunctionHookEntry
@UiItemAgentEntry
object MessageTTSHook: CommonSwitchFunctionHook() {
    override val name: String
        get() = "文字消息转语音 (使用系统 TTS)"

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
        get() = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY

    private val getMenuItemCallBack = afterHookIfEnabled(60) { param ->
        try {
            val arr = param.result
            val clQQCustomMenuItem = arr.javaClass.componentType
            val itemTts = CustomMenu.createItem(clQQCustomMenuItem, R.id.item_tts, "TTS")
            val itemTts2 = CustomMenu.createItem(clQQCustomMenuItem, R.id.item_tts2, "TTS+")
            val ret = java.lang.reflect.Array.newInstance(clQQCustomMenuItem!!, java.lang.reflect.Array.getLength(arr) + 2)
            System.arraycopy(arr, 0, ret, 0, java.lang.reflect.Array.getLength(arr))
            java.lang.reflect.Array.set(ret, java.lang.reflect.Array.getLength(arr) - 1, itemTts)
            java.lang.reflect.Array.set(ret, java.lang.reflect.Array.getLength(arr), itemTts2)
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
        when (id) {
            R.id.item_tts -> {
                val r = TTS.instance.speak(Reflex.getInstanceObjectOrNull(chatMessage, "msg")?.toString() ?: "", TextToSpeech.QUEUE_FLUSH, null)
                if (r == TextToSpeech.ERROR) {
                    Toasts.error(ctx, "TTS 请求失败")
                }
            }
            R.id.item_tts2 -> {
                val wc = CommonContextWrapper.createAppCompatContext(ctx)
                val msg = Reflex.getInstanceObjectOrNull(chatMessage, "msg")?.toString() ?: ""
                val binding = Tts2DialogBinding.inflate(LayoutInflater.from(wc))
                binding.etMsg.setText(msg)
                binding.tvVoice.text = TTS.instance.voice?.toString() ?: "null"
                binding.btnSpeak.setOnClickListener {
                    val r = TTS.instance.speak(binding.etMsg.text.toString(), TextToSpeech.QUEUE_FLUSH, null)
                    if (r == TextToSpeech.ERROR) {
                        Toasts.error(ctx, "TTS 请求失败")
                    }
                }
                binding.btnChange.setOnClickListener {
                    val voices = TTS.instance.voices
                    val byLocal = voices.groupBy { it.locale }
                    val localKeys = byLocal.keys.toList().sortedBy { it.toLanguageTag() }
                    AlertDialog.Builder(wc)
                        .setTitle("local")
                        .setItems(localKeys.map { it.toLanguageTag() }.toTypedArray()) { _, which ->
                            val local = localKeys[which]
                            val voices2 = byLocal[local]
                            val names = voices2!!.map { it.name }.sorted().toTypedArray()
                            AlertDialog.Builder(wc)
                                .setTitle(local.toLanguageTag())
                                .setItems(names) {_, which2 ->
                                    val selectedVoice = voices2[which2]
                                    AlertDialog.Builder(wc)
                                        .setTitle(selectedVoice.name)
                                        .setMessage(selectedVoice.toString())
                                        .setPositiveButton("确定") { _, _ ->
                                            val r = TTS.instance.setVoice(selectedVoice)
                                            if (r == TextToSpeech.ERROR) {
                                                Toasts.error(ctx, "TTS 请求失败")
                                            } else {
                                                binding.tvVoice.text = TTS.instance.voice.toString()
                                            }
                                        }.setNegativeButton(android.R.string.cancel, null)
                                        .show()
                                }.setNegativeButton(android.R.string.cancel, null)
                                .show()
                        }.setNegativeButton(android.R.string.cancel, null)
                        .show()
                }
                AlertDialog.Builder(wc)
                    .setView(binding.root)
                    .setTitle("TTS 高级")
                    .show()
            }
        }
    }

}
