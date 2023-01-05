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
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Parcelable
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import cc.ioctl.util.HostInfo
import io.github.duzhaokun123.util.TTS
import io.github.duzhaokun123.util.TimeFormat
import io.github.qauxv.base.IDynamicHook
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.bridge.ChatActivityFacade
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.router.decorator.BaseSwitchFunctionDecorator
import io.github.qauxv.router.decorator.IInputButtonDecorator
import io.github.qauxv.router.dispacher.InputButtonHookDispatcher
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.dexkit.CArkAppItemBubbleBuilder
import io.github.qauxv.util.dexkit.CFaceDe
import io.github.qauxv.util.dexkit.NBaseChatPie_init
import io.github.qauxv.util.ptt.SilkEncodeUtils
import mqq.app.AppRuntime
import java.io.File
import java.util.Locale

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

    const val MI_TTS = "com.xiaomi.mibrain.speech"

    override val name: String
        get() = "文字转语音发送 (使用系统 TTS)"

    override val description: String
        get() = "mp3 格式 电脑无法播放, 时长不正确, 有时无法播放\n" +
            "用法 (长按\"发送\"发送, [language]省略时使用上次选择的语言)\n" +
            "#tts [language]\n" +
            "<text>\n" +
            "\n" +
            "Example:\n" +
            "#tts zh-CN\n" +
            "你好"

    override val dispatcher: IDynamicHook
        get() = InputButtonHookDispatcher.INSTANCE

    override fun initOnce(): Boolean {
        super.initOnce()
        TTS.addInitCallback {
            if (it == TextToSpeech.ERROR) {
                Toasts.error(HostInfo.getApplication(), "TTS 初始化失败")
                traceError(RuntimeException("TTS init failed"))
            } else if (TTS.packageName == MI_TTS) {
                traceError(RuntimeException("init with xiaomi tts ($MI_TTS), which may not work"))
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
        if (line1.startsWith("#tts", ignoreCase = true).not()) return false
        if (lines.size < 2 && line1.startsWith("#tts", ignoreCase = true)) {
            AlertDialog.Builder(wc)
                .setTitle("Usage")
                .setMessage("#tts [language]\n<text>\n\nExample:\n#tts zh-CN\n你好")
                .show()
            return false
        }
        val language = line1.substring(4).trim()
        if (language.isNotBlank()) {
            TTS.instance.language = Locale.forLanguageTag(language)
        } else {
            TTS.instance.language = Locale.getDefault()
        }
        val toSend = lines.slice(1 until lines.size).joinToString("\n")
        val mp3 = File(wc.externalCacheDir, "send_tts/mp3")
        mp3.parentFile!!.mkdirs()
        var tryCount = 0
        fun trySend(retry: Boolean = false) {
            if (tryCount != 0 && tryCount % 5 == 0 && retry.not()) {
                AlertDialog.Builder(wc)
                    .setTitle("发送失败 (tryCount=$tryCount)")
                    .setMessage("你的 TTS 引擎产生的 mp3 文件可能无法播放, 尝试更换 TTS 引擎, 或继续重试" +
                        ("\n* 不要使用 MIUI 自带的 \"系统语音引擎\", 因为它的 mp3 文件无法播放".takeIf { TTS.packageName == MI_TTS } ?: ""))
                    .setNegativeButton("TTS 设置") { _, _ ->
                        TTS.showConfigDialog(wc, toSend)
                    }.setPositiveButton("重试") { _, _ ->
                        trySend(true)
                    }.show()
                return
            }
            tryCount++
            TTS.instance.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {}

                override fun onError(utteranceId: String?) {}

                override fun onAudioAvailable(utteranceId: String?, audio: ByteArray) {
                    TTS.instance.setOnUtteranceProgressListener(null)
                    runCatching { MediaExtractor().apply { setDataSource(mp3.absolutePath) } }
                        .onFailure {
                            SyncUtils.runOnUiThread {
                                trySend()
                            }
                        }.onSuccess {
                            val time = TimeFormat.format1.format(System.currentTimeMillis())
                            val save = File(wc.externalCacheDir!!, "../Tencent/MobileQQ/tts/$time.silk")
                            val trackFormat = it.getTrackFormat(0)
                            runCatching {
                                SilkEncodeUtils.nativePcm16leToSilkSS(
                                    mp3.absolutePath,
                                    save.absolutePath,
                                    trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                                    24000,
                                    (trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) * 20) / 1000,
                                    true
                                )
                                it.release()
                            }.onFailure {
                                SyncUtils.runOnUiThread {
                                    AlertDialog.Builder(wc)
                                        .setTitle(it.message)
                                        .setMessage(it.stackTraceToString())
                                        .show()
                                }
                            }.onSuccess {
                                SyncUtils.runOnUiThread {
                                    ChatActivityFacade.sendPttMessage(qqApp, session, save.absolutePath)
                                    input.setText("")
                                    Toasts.success(wc, "发送成功 (tryCount=$tryCount)")
                                }
                            }
                        }
                }
            })
            TTS.toFile(wc, toSend, mp3, "send_tts")
        }
        trySend()
        return true
    }
}
