/*
 * QNotified - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 dmca@ioctl.cc
 * https://github.com/ferredoxin/QNotified
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/ferredoxin/QNotified/blob/master/LICENSE.md>.
 */
package me.singleneuron.hook

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.res.Resources
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import cc.ioctl.util.HostInfo.PACKAGE_NAME_QQ
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedHelpers
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.hostInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object ChangeDrawerWidth : CommonConfigFunctionHook("changeDrawerWidth") {

    override val name = "修改侧滑边距"
    override val description = "感谢祈无，支持8.4.1及更高，重启后生效"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY

    override val valueState: MutableStateFlow<String?> by lazy {
        MutableStateFlow("$width dp")
    }

    override fun initOnce() = throwOrTrue {
        XposedHelpers.findAndHookMethod(Resources::class.java, "getDimensionPixelSize", Int::class.javaPrimitiveType, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam?) {
                if (param!!.args[0] == hostInfo.application.resources.getIdentifier(
                                "akx",
                                "dimen",
                                PACKAGE_NAME_QQ
                        )
                ) {
                    param.result = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            width.toFloat(),
                            (param.thisObject as Resources).displayMetrics
                    ).toInt()
                }
            }
        })
    }

    override var isEnabled: Boolean
        get() = width != 0
        set(value) {
            // not set here
        }

    private const val ChangeDrawerWidth_width = "ChangeDrawerWidth_width"

    var width: Int
        get() {
            return ConfigManager.getDefaultConfig().getIntOrDefault(ChangeDrawerWidth_width, 0)
        }
        set(value) {
            ConfigManager.getDefaultConfig()
                    .apply { putInt(ChangeDrawerWidth_width, value); save() }
            valueState.update { "$value dp" }
        }

    fun getMaxWidth(context: Context): Float {
        val dm = DisplayMetrics()
        val windowManager: WindowManager =
                context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(dm)
        return (dm.widthPixels / dm.density)
    }

    override val onUiItemClickListener: (IUiItemAgent, Activity, View) -> Unit = { _, activity, _ ->
        val dialogContext = CommonContextWrapper.createMaterialDesignContext(activity)
        MaterialAlertDialogBuilder(dialogContext).apply {
            val slider = Slider(dialogContext)
            slider.valueFrom = 0f
            slider.valueTo = getMaxWidth(dialogContext).toInt().toFloat()
            slider.stepSize = 1f
            slider.value = width.toFloat()

            setPositiveButton("确定") { dialog: DialogInterface, _: Int ->
                width = slider.value.toInt()
                dialog.dismiss()
            }
            setTitle("修改侧滑边距（设置为0dp以禁用）")
            setView(slider)
        }.show()
    }
}
