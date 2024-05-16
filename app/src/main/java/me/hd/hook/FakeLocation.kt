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
object FakeLocation : BaseFunctionHook("hd_FakeLocation") {

    private val locationKey = ConfigData<String>("hd_FakeLocation_phone")
    private var location: String
        get() = locationKey.getOrDefault("116.39773,39.90307")//(经度,纬度)
        set(value) {
            locationKey.value = value
        }

    override val uiItemAgent by lazy {
        uiClickableItem {
            title = "虚拟共享位置"
            summary = "虚拟群聊内共享位置定位内容"
            onClickListener = { _, activity, _ ->
                showDialog(activity)
            }
        }
    }
    override val uiItemLocation = FunctionEntryRouter.Locations.Entertainment.ENTERTAIN_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    @SuppressLint("RestrictedApi")
    private fun showDialog(activity: Context) {
        val configView = ConfigView(activity)
        val dialog = MaterialDialog(activity).show {
            title(text = "虚拟共享位置")
            input(
                hint = "经度,纬度",
                prefill = location,
                waitForPositiveButton = false
            ) { dialog, text ->
                val inputField = dialog.getInputField()
                dialog.setActionButtonEnabled(
                    WhichButton.POSITIVE,
                    try {
                        val locationArray = text.toString().split(",")
                        if (locationArray.size == 2) {
                            val longitude = locationArray[0].toDouble()
                            val latitude = locationArray[1].toDouble()
                            if (longitude in -180.0..180.0 && latitude in -90.0..90.0) {
                                inputField.error = null
                                true
                            } else {
                                inputField.error = "无效数值，经度范围为-180~180，纬度范围为-90~90"
                                false
                            }
                        } else {
                            inputField.error = "无效格式，格式应为经度,纬度"
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
                    location = getInputField().text.toString()
                    if (!isInitialized) HookInstaller.initializeHookForeground(context, this@FakeLocation)
                }
            }
            negativeButton(text = "取消")
        }
        configView.setText("虚拟共享位置")
        configView.view = dialog.getCustomView()
        configView.isChecked = isEnabled
        dialog.view.contentLayout.customView = null
        dialog.customView(view = configView)
    }

    override fun initOnce(): Boolean {
        val locationManagerClass = Initiator.loadClass("com.tencent.mobileqq.location.net.LocationShareLocationManager\$a")
        val locationInterfaceClass = Initiator.loadClass("com.tencent.map.geolocation.TencentLocation")
        val onChangedMethod = locationManagerClass.getDeclaredMethod("onLocationChanged", locationInterfaceClass, Int::class.java, String::class.java)
        hookBeforeIfEnabled(onChangedMethod) { param ->
            val locationClass = param.args[0]::class.java
            hookBeforeIfEnabled(locationClass.getDeclaredMethod("getLongitude")) { paramLongitude ->
                paramLongitude.result = location.split(",")[0].toDouble()
            }
            hookBeforeIfEnabled(locationClass.getDeclaredMethod("getLatitude")) { paramLatitude ->
                paramLatitude.result = location.split(",")[1].toDouble()
            }
        }
        return true
    }
}