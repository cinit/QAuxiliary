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
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cc.ioctl.util.LayoutHelper.MATCH_PARENT
import cc.ioctl.util.ui.drawable.BackgroundDrawableUtils
import io.github.qauxv.R
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.dsl.func.*
import io.github.qauxv.dsl.item.*
import io.github.qauxv.util.Log

class SettingsMainFragment : BaseSettingFragment() {

    override fun getTitle() = title
    private var title: String = "QAuxiliary"
    private lateinit var mFragmentLocations: Array<String>
    private lateinit var mFragmentDescription: FragmentDescription

    // DSL stuff below
    protected lateinit var adapter: RecyclerView.Adapter<*>
    protected lateinit var recyclerListView: RecyclerView
    protected lateinit var typeList: Array<Class<*>>
    protected lateinit var itemList: ArrayList<TMsgListItem>
    protected lateinit var itemTypeIds: Array<Int>
    protected lateinit var itemTypeDelegate: Array<TMsgListItem>

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
        val rootView = FrameLayout(context)
        val tmsgDslTree = convertFragmentDslToTMsgDslItemTree(context, mFragmentDescription)
        if (isRootFragmentDescription()) {
            addHeaderItemToRootDslTree(tmsgDslTree)
        }
        // inflate DSL tree, the most awful code in the world
        itemList = ArrayList()
        // inflate hierarchy recycler list view items, each item will have its own view holder type
        tmsgDslTree.forEach {
            itemList.addAll(it.inflateTMsgListItems(context))
        }
        // group items by java class
        typeList = itemList.map { it.javaClass }.distinct().toTypedArray()
        // item id to type id mapping
        itemTypeIds = Array(itemList.size) {
            typeList.indexOf(itemList[it].javaClass)
        }
        // item type delegate is used to create view holder
        itemTypeDelegate = Array(typeList.size) {
            itemList[itemTypeIds.indexOf(it)]
        }

        // init view
        recyclerListView = RecyclerView(context).apply {
            itemAnimator = null
            layoutAnimation = null
            layoutManager = object : LinearLayoutManager(context, VERTICAL, false) {
                override fun supportsPredictiveItemAnimations() = false
            }
            isVerticalScrollBarEnabled = false
        }
        // init adapter
        adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(
                    parent: ViewGroup,
                    viewType: Int
            ): RecyclerView.ViewHolder {
                val delegate = itemTypeDelegate[viewType]
                val vh = delegate.createViewHolder(context, parent)
                if (!delegate.isVoidBackground && delegate.isClickable) {
                    // add ripple effect
                    val rippleColor: Int = ResourcesCompat.getColor(context.resources, R.color.rippleColor, parent.context.theme)
                    vh.itemView.background = BackgroundDrawableUtils.getRoundRectSelectorDrawable(parent.context, rippleColor)
                }
                return vh
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val item = itemList[position]
                item.bindView(holder, position, context)
            }

            override fun getItemCount() = itemList.size

            override fun getItemViewType(position: Int) = itemTypeIds[position]
        }
        recyclerListView.adapter = adapter

        rootView.addView(recyclerListView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        return rootView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::recyclerListView.isInitialized) {
            recyclerListView.adapter = null
        }
    }

    private fun convertFragmentDslToTMsgDslItemTree(context: Context, fragmentDsl: FragmentDescription): ArrayList<DslTMsgListItemInflatable> {
        val resultDslItems: ArrayList<DslTMsgListItemInflatable> = ArrayList()
        for (child: IDslItemNode in fragmentDsl.children) {
            if (child.isEndNode()) {
                val item: DslTMsgListItemInflatable = convertEndNode(context, child)
                resultDslItems.add(item)
            } else {
                val item: DslTMsgListItemInflatable = convertParentNodeRecursive(context, child as IDslParentNode)
                resultDslItems.add(item)
            }
        }
        return resultDslItems
    }

    private fun convertParentNodeRecursive(context: Context, parentNode: IDslParentNode): DslTMsgListItemInflatable {
        if (parentNode is CategoryDescription) {
            return CategoryItem(parentNode.name, null).also {
                // init category item, which should only be end nodes here
                for (item: IDslItemNode in parentNode.children) {
                    val child = convertEndNode(context, item)
                    it.add(child)
                }
            }
        }
        if (parentNode.isEndNode()) {
            return convertEndNode(context, parentNode)
        }
        throw UnsupportedOperationException("unsupported node type: " + parentNode.javaClass.name)
    }

    private fun convertEndNode(context: Context, endNode: IDslItemNode): DslTMsgListItemInflatable {
        if (endNode is UiItemAgentDescription) {
            return UiAgentItem(endNode.identifier, endNode.name, endNode.itemAgentProvider)
        } else if (endNode is FragmentDescription) {
            return SimpleListItem(endNode.identifier, endNode.name ?: endNode.toString(), null).apply {
                onClickListener = {
                    // jump to target fragment
                    val location: Array<String> = arrayOf(*mFragmentLocations, endNode.identifier)
                    val fragment = SettingsMainFragment.newInstance(location)
                    settingsHostActivity!!.presentFragment(fragment)
                }
            }
        }
        throw UnsupportedOperationException("unsupported node type: " + endNode.javaClass.name)
    }

    private fun IDslItemNode.isEndNode(): Boolean {
        return this !is IDslParentNode || this is FragmentDescription
    }

    private fun isRootFragmentDescription(): Boolean {
        return mFragmentLocations.isEmpty() || mFragmentLocations.size == 1 && mFragmentLocations[0].isEmpty()
    }

    private fun addHeaderItemToRootDslTree(dslTree: ArrayList<DslTMsgListItemInflatable>) {
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
