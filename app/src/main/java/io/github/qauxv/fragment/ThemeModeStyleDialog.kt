/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2026 QAuxiliary developers
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

package io.github.qauxv.fragment

import android.app.Activity
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
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
object ThemeModeStyleDialog : BasePlainUiAgentItem(title = "主题模式") {

    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.ConfigCategory.THEME_CATEGORY

    override val valueState: MutableStateFlow<String?> by lazy {
        MutableStateFlow(ModuleThemeManager.getCurrentThemeModeName())
    }

    override val onClickListener: ((IUiItemAgent, Activity, View) -> Unit) = { _, activity, _ ->
        showSelectDialog(activity)
    }

    fun showSelectDialog(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle("主题样式")
            .setSingleChoiceItems(ModuleThemeManager.getThemeModes(), ModuleThemeManager.getCurrentThemeModeIndex()) { dialog, which ->
                updateThemeMode(activity, which)
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateThemeMode(activity: Activity, selectedIndex: Int) {
        ModuleThemeManager.setCurrentThemeMode(selectedIndex)
        AppCompatDelegate.setDefaultNightMode(ModuleThemeManager.getCurrentNightMode())
        valueState.update { ModuleThemeManager.getCurrentThemeModeName() }
        if (activity is SettingsUiFragmentHostActivity) {
            // refresh ui, wait we are finished
            SyncUtils.postDelayed(100) {
                activity.recreate()
            }
        }
    }
}
