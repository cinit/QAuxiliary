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

package io.github.qauxv.dsl.func

import android.os.Bundle
import io.github.qauxv.fragment.BaseSettingFragment

class FragmentImplDescription(
        override val identifier: String,
        override val name: String?,
        private val targetClass: Class<out BaseSettingFragment>,
        categoryTitleSearchable: Boolean = true,
) : BaseParentNode(), IDslFragmentNode {

    override val isSearchable: Boolean = categoryTitleSearchable

    override fun getTargetFragmentClass(location: Array<String>): Class<out BaseSettingFragment> {
        return targetClass
    }

    override fun getTargetFragmentArguments(location: Array<String>, targetItemId: String?): Bundle? {
        // TODO: 2022-02-13: add search navigation support for custom fragment
        return null
    }
}
