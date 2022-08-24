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

package io.github.qauxv.fragment

import android.app.Activity
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import com.jaredrummler.android.colorpicker.ColorShape
import io.github.qauxv.activity.SettingsUiFragmentHostActivity
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.BasePlainUiAgentItem
import io.github.qauxv.ui.ModuleThemeManager
import io.github.qauxv.util.SyncUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@UiItemAgentEntry
object ThemeSelectDialog : BasePlainUiAgentItem(title = "主题色", description = null) {

    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.ConfigCategory.CONFIG_CATEGORY

    override val valueState: MutableStateFlow<String?> by lazy {
        MutableStateFlow(ModuleThemeManager.getCurrentThemeName())
    }

    override val onClickListener: ((IUiItemAgent, Activity, View) -> Unit) = { _, activity, _ ->
        showThemeSelectDialog(activity)
    }

    private fun showThemeSelectDialog(activity: Activity) {
        val dialog = ColorPickerDialog.newBuilder()
                .setDialogType(ColorPickerDialog.TYPE_PRESETS)
                .setDialogTitle(com.jaredrummler.android.colorpicker.R.string.cpv_default_title)
                .setColorShape(ColorShape.CIRCLE)
                .setPresets(ModuleThemeManager.getColors(activity))
                .setAllowPresets(true)
                .setAllowCustom(false)
                .setShowAlphaSlider(false)
                .setShowColorShades(false)
                .setColor(ModuleThemeManager.getCurrentThemeColor(activity))
                .create()
        dialog.setColorPickerDialogListener(object : ColorPickerDialogListener {
            override fun onColorSelected(dialogId: Int, color: Int) {
                updateThemeColor(activity, color)
            }

            override fun onDialogDismissed(dialogId: Int) {
                // nothing to do
            }
        })
        (activity as FragmentActivity).supportFragmentManager
                .beginTransaction()
                .add(dialog, "color_picker_dialog")
                .commitAllowingStateLoss()
    }

    private fun updateThemeColor(activity: Activity, color: Int) {
        ModuleThemeManager.setCurrentThemeByColor(activity, color)
        valueState.update { ModuleThemeManager.getCurrentThemeName() }
        if (activity is SettingsUiFragmentHostActivity) {
            // refresh ui, wait we are finished
            SyncUtils.postDelayed(100) {
                activity.recreate()
            }
        }
    }
}
