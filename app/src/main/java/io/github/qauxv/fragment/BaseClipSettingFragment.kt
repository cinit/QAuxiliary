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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

abstract class BaseClipSettingFragment : BaseSettingFragment() {

    private var mView: View? = null

    @Deprecated("not to be used", ReplaceWith("BaseSettingFragment.doOnCreateView"))
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = super.onCreateView(inflater, container, savedInstanceState)
        applyPaddings()
        return mView
    }

    override fun onLayoutPaddingsChanged() {
        super.onLayoutPaddingsChanged()
        applyPaddings()
    }

    override fun onResume() {
        super.onResume()
        applyPaddings()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mView = null
    }

    private fun applyPaddings() {
        mView?.apply {
            setPadding(
                paddingLeft,
                layoutPaddingTop,
                paddingRight,
                layoutPaddingBottom
            )
        }
    }
}
