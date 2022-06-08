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

import android.content.Context
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import io.github.qauxv.databinding.Tts2DialogBinding
import io.github.qauxv.util.Toasts
import java.io.File

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

    fun showConfigDialog(wc: Context, text: String = "") {
        val binding = Tts2DialogBinding.inflate(LayoutInflater.from(wc))
        binding.etMsg.setText(text)
        binding.tvVoice.text = instance.voice?.toString() ?: "null"
        binding.tvPackage.text = instance.defaultEngine
        binding.btnSpeak.setOnClickListener {
            speak(wc, binding.etMsg.text.toString())
        }
        binding.btnChange.setOnClickListener {
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
                                        binding.tvVoice.text = instance.voice.toString()
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
            .setTitle("TTS 设置")
            .show()
    }
}
