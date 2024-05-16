/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2024 QAuxiliary developers
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

package me.hd.hook

import android.annotation.SuppressLint
import android.content.Context
import cc.ioctl.util.hookBeforeIfEnabled
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.core.HookInstaller
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.dsl.uiClickableItem
import io.github.qauxv.hook.BaseFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import me.ketal.data.ConfigData
import me.ketal.ui.view.ConfigView
import me.ketal.util.ignoreResult

@FunctionHookEntry
@UiItemAgentEntry
object FakeVoiceTime : BaseFunctionHook("hd_FakeVoiceTime") {

    private val timeKey = ConfigData<String>("hd_FakeVoiceTime_phone")
    private var time: String
        get() = timeKey.getOrDefault("1")
        set(value) {
            timeKey.value = value
        }

    override val uiItemAgent by lazy {
        uiClickableItem {
            title = "伪装语音时间"
            summary = "伪装发送语音文件时长[1s~300s]"
            onClickListener = { _, activity, _ ->
                showDialog(activity)
            }
        }
    }
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    @SuppressLint("RestrictedApi")
    private fun showDialog(activity: Context) {
        val configView = ConfigView(activity)
        val dialog = MaterialDialog(activity).show {
            title(text = "伪装语音时间")
            input(
                hint = "请输入语音时间...",
                prefill = time,
                waitForPositiveButton = false
            ) { dialog, text ->
                val inputField = dialog.getInputField()
                dialog.setActionButtonEnabled(
                    WhichButton.POSITIVE,
                    try {
                        if (text.toString().toInt() in 1..300) {
                            inputField.error = null
                            true
                        } else {
                            inputField.error = "无效数值，仅可以输入1-300范围内的数字"
                            false
                        }
                    } catch (e: NumberFormatException) {
                        inputField.error = "请输入有效的数据"
                        false
                    }
                )
            }.ignoreResult()
            positiveButton(text = "保存") {
                isEnabled = configView.isChecked
                if (isEnabled) {
                    time = getInputField().text.toString()
                    if (!isInitialized) HookInstaller.initializeHookForeground(context, this@FakeVoiceTime)
                }
            }
            negativeButton(text = "取消")
        }
        configView.setText("伪装语音时间")
        configView.view = dialog.getCustomView()
        configView.isChecked = isEnabled
        dialog.view.contentLayout.customView = null
        dialog.customView(view = configView)
    }

    override fun initOnce(): Boolean {
        val recorderApiClass = Initiator.loadClass("com.tencent.mobileqq.ptt.temp.api.impl.QQRecorderTempApiImpl")
        val messageRecordClass = Initiator.loadClass("com.tencent.mobileqq.data.MessageRecord")
        val getFilePlayTimeMethod = recorderApiClass.getDeclaredMethod("getFilePlayTime", messageRecordClass)
        hookBeforeIfEnabled(getFilePlayTimeMethod) { param ->
            param.result = time.toInt()
        }
        return true
    }
}