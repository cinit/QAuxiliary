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
import android.os.Bundle
import cc.ioctl.util.hookBeforeIfEnabled
import com.afollestad.materialdialogs.MaterialDialog
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
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.Hd_FakePhone_Method
import io.github.qauxv.util.requireMinQQVersion
import me.ketal.data.ConfigData
import me.ketal.ui.view.ConfigView
import me.ketal.util.ignoreResult

@FunctionHookEntry
@UiItemAgentEntry
object FakePhone : BaseFunctionHook(
    hookKey = "hd_FakePhone",
    targets = arrayOf(Hd_FakePhone_Method)
) {

    private val phoneKey = ConfigData<String>("hd_FakePhone_phone")
    private var phone: String
        get() = phoneKey.getOrDefault("100******00")
        set(value) {
            phoneKey.value = value
        }

    override val uiItemAgent by lazy {
        uiClickableItem {
            title = "伪装手机号码"
            summary = "伪装设置页手机号码显示内容"
            onClickListener = { _, activity, _ ->
                showDialog(activity)
            }
        }
    }
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.UI_MISC
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    @SuppressLint("RestrictedApi")
    private fun showDialog(activity: Context) {
        val configView = ConfigView(activity)
        val dialog = MaterialDialog(activity).show {
            title(text = "伪装手机号码")
            input(
                hint = "请输入手机号码...",
                prefill = phone
            ).ignoreResult()
            positiveButton(text = "保存") {
                isEnabled = configView.isChecked
                if (isEnabled) {
                    phone = getInputField().text.toString()
                    if (!isInitialized) HookInstaller.initializeHookForeground(context, this@FakePhone)
                }
            }
            negativeButton(text = "取消")
        }
        configView.setText("伪装手机号码")
        configView.view = dialog.getCustomView()
        configView.isChecked = isEnabled
        dialog.view.contentLayout.customView = null
        dialog.customView(view = configView)
    }

    override fun initOnce(): Boolean {
        val onUpdateMethod = DexKit.requireMethodFromCache(Hd_FakePhone_Method)
        hookBeforeIfEnabled(onUpdateMethod) { param ->
            if (param.args[0] == 5) {
                (param.args[2])?.let { obj ->
                    val bundle = obj as Bundle
                    bundle.putString("phone", phone)
                    param.args[2] = bundle
                }
            }
        }
        return true
    }
}