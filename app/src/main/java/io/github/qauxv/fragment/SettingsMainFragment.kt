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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.dsl.converter.convertToTMsgDslItemTree
import io.github.qauxv.dsl.converter.createRecyclerViewFromTMSgDslTree
import io.github.qauxv.dsl.func.FragmentDescription
import io.github.qauxv.dsl.item.DslTMsgListItemInflatable

class SettingsMainFragment : BaseSettingFragment() {

    override fun getTitle() = title
    private var title: String = "QAuxiliary"
    private lateinit var mFragmentLocations: Array<String>
    private lateinit var mFragmentDescription: FragmentDescription

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val location: Array<String> = arguments?.getStringArray(TARGET_FRAGMENT_LOCATION)
                ?: throw IllegalArgumentException("target fragment location is null")
        // fault, why start SettingsMainFragment but not no location?
        mFragmentLocations = location
        // find fragment description
        val desc = FunctionEntryRouter.findDescriptionByLocation(location)
                ?: throw IllegalArgumentException("unable to find fragment description by location: " + location.contentToString())
        if (desc !is FragmentDescription) {
            throw IllegalArgumentException("fragment description is not FragmentDescription, got: " + desc.javaClass.name)
        }
        mFragmentDescription = desc
        title = mFragmentDescription.name ?: title
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = layoutInflater.context
        val tmsgDslTree = convertToTMsgDslItemTree(context, mFragmentDescription)
        if (isRootFragmentDescription()) {
            addFlavorToRootDslTree(tmsgDslTree)
        }
        val recyclerView = createRecyclerViewFromTMSgDslTree(context, tmsgDslTree)
        return recyclerView
    }

    private fun isRootFragmentDescription(): Boolean {
        return mFragmentLocations.isEmpty() || mFragmentLocations.size == 1 && mFragmentLocations[0].isEmpty()
    }

    private fun addFlavorToRootDslTree(dslTree: DslTMsgListItemInflatable) {
        // TODO: 2022-02-22 add flavor to root dsl tree
    }

    companion object {
        const val TARGET_FRAGMENT_LOCATION = "SettingsMainFragment.TARGET_FRAGMENT_LOCATION"

        @JvmStatic
        fun newInstance(location: Array<String>): SettingsMainFragment {
            val fragment = SettingsMainFragment()
            val bundle = Bundle()
            bundle.putStringArray(TARGET_FRAGMENT_LOCATION, location)
            fragment.arguments = bundle
            return fragment
        }
    }
}
