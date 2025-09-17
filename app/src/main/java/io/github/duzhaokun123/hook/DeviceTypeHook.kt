/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2025 QAuxiliary developers
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

package io.github.duzhaokun123.hook

import android.app.Activity
import android.app.AlertDialog
import android.view.View
import cc.ioctl.util.hookBeforeIfEnabled
import com.github.kyuubiran.ezxhelper.utils.getStaticObject
import com.github.kyuubiran.ezxhelper.utils.invokeStaticMethodAuto
import com.github.kyuubiran.ezxhelper.utils.loadClass
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.PadUtil_getDeviceType
import io.github.qauxv.util.xpcompat.XposedBridge
import kotlinx.coroutines.flow.MutableStateFlow

@FunctionHookEntry
@UiItemAgentEntry
object DeviceTypeHook : CommonConfigFunctionHook(targets = arrayOf(PadUtil_getDeviceType)) {
    override val name = "设备类型修改"
    override val description =
        """
        |修改设备类型为 PHONE/PAD/FOLD
        |并不能实现多设备登陆 但能影响 UI 布局
        |重启生效
        """.trimMargin()
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY

    override val valueState = MutableStateFlow(if (isEnabled) ConfigManager.getDefaultConfig().getString("io.github.duzhaokun123.hook.DeviceTypeHook.deviceType") else "禁用")

    val enmu_DeviceType by lazy { loadClass("com.tencent.common.config.pad.DeviceType") }

    override fun initOnce(): Boolean {
        val deviceType =
            enmu_DeviceType
                .invokeStaticMethodAuto("valueOf", ConfigManager.getDefaultConfig().getString("io.github.duzhaokun123.hook.DeviceTypeHook.deviceType"))
        hookBeforeIfEnabled(DexKit.requireMethodFromCache(PadUtil_getDeviceType)) {
            it.result = deviceType
        }
        return true
    }

    override val onUiItemClickListener = { _: IUiItemAgent, activity: Activity, _: View ->
        @Suppress("UNCHECKED_CAST")
        val deviceTypes = enmu_DeviceType.getStaticObject("\$VALUES") as Array<Enum<*>>
        val deviceTypeNames = deviceTypes.map { it.name }.toTypedArray()
        val method_PadUtil_getDeviceType = runCatching { DexKit.requireMethodFromCache(PadUtil_getDeviceType) }.getOrNull()
        val originalType =
            if (method_PadUtil_getDeviceType != null) {
                XposedBridge.invokeOriginalMethod(method_PadUtil_getDeviceType, null, arrayOf(activity))
            } else {
                "未知"
            }
        AlertDialog.Builder(activity)
            .setTitle("原始设备类型: $originalType")
            .setItems(deviceTypeNames) { _, which ->
                valueState.value = deviceTypeNames[which]
                ConfigManager.getDefaultConfig().putString("io.github.duzhaokun123.hook.DeviceTypeHook.deviceType", deviceTypeNames[which])
                isEnabled = true
            }.setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton("恢复默认") { _, _ ->
                valueState.value = null
                isEnabled = false
            }.show()
        Unit
    }
}