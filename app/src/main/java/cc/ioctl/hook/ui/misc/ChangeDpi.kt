/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the qwq233 Universal License
 * as published on https://github.com/qwq233/license; either
 * version 2 of the License, or any later version and our EULA as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the qwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package cc.ioctl.hook.ui.misc

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import android.text.InputType
import android.view.View
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatTextView
import cc.ioctl.util.HookUtils
import io.github.qauxv.util.xpcompat.XposedBridge
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.hostInfo
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.io.FileInputStream
import java.io.IOException

@UiItemAgentEntry
@FunctionHookEntry
object ChangeDpi : CommonConfigFunctionHook(targetProc = SyncUtils.PROC_ANY) {

    override val name = "修改 DPI"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MISC_CATEGORY
    override val isApplicationRestartRequired = true
    private const val KEY_TARGET_DPI = "qa_target_dpi"

    private var mDpiForThisLife: Int? = null

    override val valueState: MutableStateFlow<String?> by lazy {
        MutableStateFlow(targetDpi.let {
            if (it == 0) {
                "未设置"
            } else {
                it.toString()
            }
        })
    }

    override fun initOnce(): Boolean {
        // We don't hook getDisplayMetrics here, we did that in the very early startup stage of the app.
        mDpiForThisLife = currentDpiInProcess
        if (mDpiForThisLife != 0) {
            // handle config change
            val kCompatibilityInfo = Class.forName("android.content.res.CompatibilityInfo")
            val kActivityThread = Class.forName("android.app.ActivityThread")
            val hookCallback = HookUtils.beforeAlways(this, 49) { param ->
                val originConfig = param.args[0] as Configuration?
                    ?: // maybe a compatibilityInfo update, ignore
                    return@beforeAlways
                originConfig.densityDpi = mDpiForThisLife!!
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+, hook ConfigurationController.handleConfigurationChanged
                val kConfigurationController =
                    Class.forName("android.app.ConfigurationController")
                val handleConfigurationChanged =
                    kConfigurationController.getDeclaredMethod(
                        "handleConfigurationChanged",
                        Configuration::class.java, kCompatibilityInfo
                    )
                XposedBridge.hookMethod(handleConfigurationChanged, hookCallback)
            } else {
                // on Android 11 and below, hook ActivityThread.handleConfigurationChanged
                val handleConfigurationChanged = kActivityThread.getDeclaredMethod(
                    "handleConfigurationChanged",
                    Configuration::class.java, kCompatibilityInfo
                )
                XposedBridge.hookMethod(handleConfigurationChanged, hookCallback)
            }
        }
        return true
    }

    override val onUiItemClickListener: (IUiItemAgent, Activity, View) -> Unit = { _, context, _ ->
        val ctx = CommonContextWrapper.createAppCompatContext(context)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(ctx)
        // CheckBox: enabled
        // TextView: current value
        // EditText: target value (100-1600)
        // Button: OK
        val currentValueTextView = AppCompatTextView(ctx).apply {
            text = "当前值: ${currentDpiInProcess}\n修改 DPI 需要重启应用才能生效"
        }
        val targetValueEditText = androidx.appcompat.widget.AppCompatEditText(ctx).apply {
            setText(if (targetDpi == 0) "" else targetDpi.toString())
            inputType = InputType.TYPE_CLASS_NUMBER
            // dismiss error when text changed
            addTextChangedListener(object : android.text.TextWatcher {
                override fun afterTextChanged(s: android.text.Editable?) {
                    error = null
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // ignore
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // ignore
                }
            })
        }
        val enableCheckBox = AppCompatCheckBox(ctx).apply {
            text = "启用"
            hint = "请输入 DPI"
            setOnCheckedChangeListener { _, isChecked ->
                targetValueEditText.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
            isChecked = targetDpi != 0
        }
        dialog.setView(android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(enableCheckBox)
            addView(currentValueTextView)
            addView(targetValueEditText)
        })
        targetValueEditText.visibility = if (enableCheckBox.isChecked) View.VISIBLE else View.GONE
        dialog.setTitle("修改 DPI")
        dialog.setPositiveButton("确定", null) // set later
        dialog.setNegativeButton("取消", null)
        dialog.setNeutralButton("复原") { _, _ ->
            this.targetDpi = 0
            valueState.value = "未设置"
            Toasts.info(ctx, "重启 " + hostInfo.hostName + " 生效")
        }
        val alertDialog = dialog.create()
        alertDialog.setOnShowListener {
            alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (enableCheckBox.isChecked) {
                    val targetDpi = targetValueEditText.text.toString().toIntOrNull()
                    if (targetDpi == null || targetDpi < 100 || targetDpi > 1600) {
                        targetValueEditText.error = "请输入 100-1600 之间的整数"
                        return@setOnClickListener
                    }
                    this.targetDpi = targetDpi
                    valueState.value = ChangeDpi.targetDpi.toString()
                } else {
                    this.targetDpi = 0
                    valueState.value = "未设置"
                }
                alertDialog.dismiss()
                Toasts.info(ctx, "重启 " + hostInfo.hostName + " 生效")
            }
        }
        alertDialog.show()
    }

    private val currentDpiInProcess: Int
        get() {
            val ctx = hostInfo.application
            val res = ctx.resources
            val dm = res.displayMetrics
            return dm.densityDpi
        }

    var targetDpi: Int
        get() {
            val file = File(hostInfo.application.filesDir, KEY_TARGET_DPI)
            if (!file.exists()) {
                return 0;
            }
            try {
                // read exact 4 bytes, little endian
                FileInputStream(file).use {
                    val b = ByteArray(4)
                    if (it.read(b) != 4) {
                        return 0
                    }
                    return b[0].toInt() and 0xff or
                        (b[1].toInt() and 0xff shl 8) or
                        (b[2].toInt() and 0xff shl 16) or
                        (b[3].toInt() and 0xff shl 24)
                }
            } catch (e: IOException) {
                traceError(e)
                return 0
            }
        }
        set(value) {
            val file = File(hostInfo.application.filesDir, KEY_TARGET_DPI)
            try {
                if (!file.exists()) {
                    file.createNewFile()
                }
                // write exact 4 bytes, little endian
                file.outputStream().use {
                    it.write(
                        byteArrayOf(
                            (value and 0xff).toByte(),
                            (value shr 8 and 0xff).toByte(),
                            (value shr 16 and 0xff).toByte(),
                            (value shr 24 and 0xff).toByte()
                        )
                    )
                }
            } catch (e: IOException) {
                traceError(e)
            }
        }

    override var isEnabled: Boolean
        get() = targetDpi != 0
        set(value) {
            // ignore
        }

}
