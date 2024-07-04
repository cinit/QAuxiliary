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

package io.github.duzhaokun123.util

import android.app.Dialog
import android.content.Context
import android.os.Parcelable
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.qauxv.bridge.ChatActivityFacade
import io.github.qauxv.databinding.DialogSendTtsBinding
import io.github.qauxv.databinding.Tts2DialogBinding
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.ptt.SilkEncodeUtils
import mqq.app.AppRuntime
import java.io.File
import java.io.FileOutputStream

object TTS {
    lateinit var instance: TextToSpeech
        private set
    lateinit var packageName : String
        private set
    private var isInit = false
    private var initResult: Int? = null
    private var initCallbacks = mutableListOf<(Int) -> Unit>()

    fun init(context: Context) {
        if (isInit) return
        isInit = true
        instance = TextToSpeech(context) { status ->
            initResult = status
            packageName = instance.defaultEngine
            initCallbacks.removeAll { it(status); true }
        }
    }

    fun addInitCallback(callback: (Int) -> Unit) {
        if (initResult != null) {
            callback(initResult!!)
            return
        }
        initCallbacks.add(callback)
    }

    fun checkInit(wc: Context): Boolean {
        if (initResult != TextToSpeech.SUCCESS) {
            AlertDialog.Builder(wc)
                .setTitle("TTS 未正常初始化")
                .setMessage("检查 系统设置 -> 无障碍 -> 文字转语言(TTS)")
                .show()
        }
        return initResult == TextToSpeech.SUCCESS
    }

    fun checkTTSRequestResult(wc: Context, result: Int) {
        if (result == TextToSpeech.ERROR) {
            AlertDialog.Builder(wc)
                .setTitle("TTS 请求失败")
                .setNegativeButton("TTS 设置") { _, _ ->
                    showConfigDialog(wc, "")
                }.show()
        }
    }

    fun speak(wc: Context, text: String): Boolean {
        if (!checkInit(wc)) return false
        val r = instance.speak(text, TextToSpeech.QUEUE_FLUSH, null)
        checkTTSRequestResult(wc, r)
        return r == TextToSpeech.SUCCESS
    }

    fun toFile(wc: Context, text: String, file: File, utteranceId: String): Boolean {
        if (!checkInit(wc)) return false
        val r = instance.synthesizeToFile(text, null, file, utteranceId)
        checkTTSRequestResult(wc, r)
        return r == TextToSpeech.SUCCESS
    }

    private fun changeVoice(wc: Context, onChange: (String) -> Unit) {
        val voices = instance.voices
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
                                val r = instance.setVoice(selectedVoice)
                                if (r == TextToSpeech.ERROR) {
                                    Toasts.error(wc, "TTS 请求失败")
                                } else {
                                    onChange(selectedVoice.toString())
                                }
                            }.setNegativeButton(android.R.string.cancel, null)
                            .show()
                    }.setNegativeButton(android.R.string.cancel, null)
                    .show()
            }.setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    fun showConfigDialog(wc: Context, text: String = "") {
        if (!checkInit(wc)) return
        val binding = Tts2DialogBinding.inflate(LayoutInflater.from(wc))
        binding.etMsg.setText(text)
        binding.tvVoice.text = instance.voice?.toString() ?: "null"
        binding.tvPackage.text = instance.defaultEngine
        binding.btnSpeak.setOnClickListener {
            speak(wc, binding.etMsg.text.toString())
        }
        binding.btnChange.setOnClickListener {
            changeVoice(wc) {
                binding.tvVoice.text = it
            }
        }
        AlertDialog.Builder(wc)
            .setView(binding.root)
            .setTitle("TTS 设置")
            .show()
    }

    fun showSendDialog(wc: Context, text: String, session: Parcelable, input: EditText, qqApp: AppRuntime) {
        var editDialog: Dialog? = null
        val binding = DialogSendTtsBinding.inflate(LayoutInflater.from(wc))
        binding.etMsg.setText(text)
        binding.tvVoice.text = instance.voice?.toString() ?: "null"
        binding.tvPackage.text = instance.defaultEngine
        binding.btnSpeak.setOnClickListener {
            speak(wc, binding.etMsg.text.toString())
        }
        binding.btnChange.setOnClickListener {
            changeVoice(wc) {
                binding.tvVoice.text = it
            }
        }
        binding.btnSend.setOnClickListener {
            instance.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                var sampleRateInHz = 0
                val pcm = File(wc.externalCacheDir, "send_tts/pcm")
                val silk = File(wc.externalCacheDir!!, "../Tencent/MobileQQ/tts/${TimeFormat.format1.format(System.currentTimeMillis())}.silk").apply { parentFile!!.mkdirs() }

                lateinit var dialog: Dialog

                override fun onStart(utteranceId: String?) {
                    SyncUtils.runOnUiThread {
                        dialog = AlertDialog.Builder(wc)
                            .setTitle("合成中")
                            .setView(ProgressBar(wc))
                            .setPositiveButton("取消发送") { _, _ ->
                                instance.setOnUtteranceProgressListener(null)
                            }.show()
                    }
                }

                override fun onDone(utteranceId: String?) {
                    instance.setOnUtteranceProgressListener(null)
                    runCatching {
                        SilkEncodeUtils.nativePcm16leToSilkSS(
                            pcm.absolutePath,
                            silk.absolutePath,
                            sampleRateInHz,
                            24000,
                            (sampleRateInHz * 20) / 1000,
                            true
                        )
                    }.onFailure {
                        SyncUtils.runOnUiThread {
                            dialog.dismiss()
                            if (it.message != null && it.message!!.endsWith("-2")) {
                                AlertDialog.Builder(wc)
                                    .setTitle("不支持的采样率 ${sampleRateInHz}Hz")
                                    .setMessage("仅支持 8000Hz 12000Hz 16000Hz 24000Hz")
                                    .show()
                            } else {
                                AlertDialog.Builder(wc)
                                    .setTitle(it.message)
                                    .setMessage(it.stackTraceToString())
                                    .show()
                            }
                        }
                    }.onSuccess {
                        SyncUtils.runOnUiThread {
                            dialog.dismiss()
                            ChatActivityFacade.sendPttMessage(qqApp, session, silk.absolutePath)
                            input.setText("")
                            Toasts.success(wc, "发送成功")
                            editDialog?.dismiss()
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {}

                override fun onBeginSynthesis(utteranceId: String?, sampleRateInHz: Int, audioFormat: Int, channelCount: Int) {
                    this.sampleRateInHz = sampleRateInHz
                    pcm.delete()
                }

                override fun onAudioAvailable(utteranceId: String?, audio: ByteArray) {
                    FileOutputStream(pcm, true).use { out ->
                        out.write(audio)
                    }
                }
            })
            toFile(wc, binding.etMsg.text.toString(), File(wc.externalCacheDir, "send_tts/audio").apply { parentFile!!.mkdirs() }, "send_tts")
        }
        editDialog = MaterialAlertDialogBuilder(wc)
            .setView(binding.root)
            .setTitle("TTS 发送")
            .show()
    }
}
