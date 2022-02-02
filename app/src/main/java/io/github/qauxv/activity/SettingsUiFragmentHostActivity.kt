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

package io.github.qauxv.activity

import android.os.Bundle
import io.github.qauxv.R
import io.github.qauxv.base.IUiItemAgentProvider
import io.github.qauxv.fragment.BaseSettingFragment
import io.github.qauxv.util.UiThread

class SettingsUiFragmentHostActivity : AppCompatTransferActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_MaiTungTMDesign)
        // TODO 2022-01-31: update day night color according to the host app
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_ui_host)
        if (savedInstanceState == null) {
        }
    }

    /**
     * Navigate to the specified UI item.
     */
    @UiThread
    fun navigateToFunctionUiItemEntry(targetItem: IUiItemAgentProvider) {
        TODO("not implemented")
    }

    fun finishFragment(fragment: BaseSettingFragment) {
        supportFragmentManager.beginTransaction()
            .remove(fragment)
            .commitNow()
    }

    fun popCurrentFragment() {
        supportFragmentManager.popBackStack()
    }
}
