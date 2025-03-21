/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2024 QAuxiliary developers
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

package io.github.qauxv.tips.newfeaturehint

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import io.github.qauxv.R
import io.github.qauxv.base.IUiItemAgentProvider
import io.github.qauxv.databinding.FragmentNewFeatureIntroBinding
import io.github.qauxv.databinding.SearchResultItemBinding
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.dsl.func.IDslFragmentNode
import io.github.qauxv.dsl.func.IDslParentNode
import io.github.qauxv.fragment.BaseRootLayoutFragment
import io.github.qauxv.fragment.SettingsMainFragment
import io.github.qauxv.util.UiThread

class NewFeatureIntroduceFragment : BaseRootLayoutFragment() {

    private lateinit var mBinding: FragmentNewFeatureIntroBinding

    override fun doOnCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        title = "新功能"
        mBinding = FragmentNewFeatureIntroBinding.inflate(inflater, container, false)
        val root = mBinding.root
        rootLayoutView = root
        initView()
        return root
    }

    private fun initView() {
        val disableBtn = mBinding.newFeatureIntroDisableButton
        disableBtn.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("禁用新功能提示")
                .setMessage("禁用后，您将不再收到新功能提示。\n您可用在 辅助 > 杂项 > 新功能提示 中重新开启。")
                .setPositiveButton("禁用") { _, _ ->
                    NewFeatureManager.newFeatureTipEnabled = false
                    NewFeatureManager.markAllFeaturesKnown()
                    finishFragment()
                }
                .setNegativeButton("取消", null)
                .show()
        }
        val itemLayout: LinearLayout = mBinding.newFeatureIntroItemList
        val featureNames = NewFeatureManager.queryNewFeatures()?.toSet() ?: setOf()
        val features = ArrayList<IUiItemAgentProvider>()
        io.github.qauxv.gen.getAnnotatedUiItemAgentEntryList().forEach { entry ->
            if (entry.itemAgentProviderUniqueIdentifier in featureNames) {
                features.add(entry)
            }
        }
        features.forEach { item ->
            itemLayout.addView(createItem(EntryItem(item), itemLayout))
        }
    }

    private var mMadeKnown = false

    override fun onResume() {
        super.onResume()
        if (!mMadeKnown) {
            mMadeKnown = true
            NewFeatureManager.markAllFeaturesKnown()
        }
    }

    private data class EntryItem(
        val agent: IUiItemAgentProvider,
        var location: Array<String>? = null,
        var shownLocation: Array<String>? = null
    )

    private fun createItem(item: EntryItem, container: ViewGroup): View {
        updateUiItemAgentLocation(item)
        val binding = SearchResultItemBinding.inflate(LayoutInflater.from(container.context), container, false)
        bindResultItem(binding, item)
        return binding.root
    }

    private fun bindResultItem(binding: SearchResultItemBinding, item: EntryItem) {
        val title: String = item.agent.uiItemAgent.titleProvider.invoke(item.agent.uiItemAgent)
        val description: String = ((item.agent.uiItemAgent.summaryProvider?.invoke(item.agent.uiItemAgent, requireContext()) ?: "").toString())
        binding.title.text = title
        val locationString = item.shownLocation!!.joinToString(separator = " > ")
        binding.summary.text = locationString
        binding.description.text = description
        binding.root.setTag(R.id.tag_searchResultItem, item)
        binding.root.setOnClickListener { v ->
            val result = v?.getTag(R.id.tag_searchResultItem) as EntryItem?
            result?.let {
                navigateToTargetEntry(it)
            }
        }
    }

    @UiThread
    private fun navigateToTargetEntry(item: EntryItem) {
        if (item.location == null) {
            updateUiItemAgentLocation(item)
        }
        // find containing fragment
        val absFullLocation = item.location!!
        val identifier = absFullLocation.last()
        var container = absFullLocation.dropLast(1).toTypedArray()
        var targetFragmentLocation: Array<String>? = null
        var node = FunctionEntryRouter.settingsUiItemDslTree.lookupHierarchy(container)
        // lookup the parent container, until we find the parent is a fragment
        while (true) {
            if (node == null) {
                // we are lost!!!
                break
            }
            if (node is IDslFragmentNode) {
                // found
                targetFragmentLocation = container
                break
            }
            if (container.isEmpty()) {
                // we are lost!!!
                break
            }
            // not a fragment, keep looking up parent
            container = container.dropLast(1).toTypedArray()
            // get current node
            node = FunctionEntryRouter.settingsUiItemDslTree.lookupHierarchy(container)
        }
        if (targetFragmentLocation == null) {
            // tell user we are lost
            AlertDialog.Builder(requireContext()).apply {
                setTitle("Navigation Error")
                setMessage("We are lost, can't find the target fragment: " + absFullLocation.joinToString("."))
                setPositiveButton(android.R.string.ok) { _, _ -> }
                setCancelable(true)
            }.show()
        } else {
            val fragment = SettingsMainFragment.newInstance(targetFragmentLocation, identifier)
            settingsHostActivity!!.presentFragment(fragment)
        }
    }

    private fun updateUiItemAgentLocation(item: EntryItem) {
        val agent = item.agent
        val containerLocation: Array<String> = FunctionEntryRouter.resolveUiItemAnycastLocation(agent.uiItemLocation)
            ?: agent.uiItemLocation
        val fullLocation = arrayOf(*containerLocation, agent.itemAgentProviderUniqueIdentifier)
        item.location = fullLocation
        // translate the container location to human readable string
        // e.g. arrayOf("home", "app", "search") -> "Home > App > Search" (and the the target item is "Item 0")
        // get the DSL element of each level to get the title
        // start from the first level
        val currentLocation: ArrayList<String> = ArrayList()
        var currentNode: IDslParentNode? = FunctionEntryRouter.settingsUiItemDslTree
        for (i in containerLocation.indices) {
            if (currentNode == null) {
                // we are lost!!! use raw identifier as the location
                currentLocation.add(containerLocation[i])
            } else {
                val nextNode = currentNode.findChildById(containerLocation[i])
                if (nextNode == null) {
                    currentNode = null
                    // we are lost!!! use raw identifier as the location
                    currentLocation.add(containerLocation[i])
                } else {
                    nextNode.name?.let { currentLocation.add(it) }
                    currentNode = if (nextNode is IDslParentNode) {
                        nextNode
                    } else {
                        // this is the target item
                        null
                    }
                }
            }
        }
        item.shownLocation = currentLocation.toTypedArray()
    }

}
