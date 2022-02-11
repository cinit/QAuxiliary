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
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.qauxv.R
import io.github.qauxv.SyncUtils
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.IUiItemAgentProvider
import io.github.qauxv.databinding.FragmentSettingSearchBinding
import io.github.qauxv.databinding.SearchResultItemBinding
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.dsl.func.FragmentDescription
import io.github.qauxv.dsl.func.IDslParentNode
import io.github.qauxv.util.NonUiThread
import io.github.qauxv.util.UiThread

/**
 * The search fragment. It will cover the whole screen including the AppBarLayout.
 */
class SearchOverlayFragment : BaseSettingFragment() {

    private var binding: FragmentSettingSearchBinding? = null
    private var currentKeyword: String = ""
    private var lastSearchKeyword: String = ""
    private val searchResults: ArrayList<SearchResult> = ArrayList()
    private val allItemsContainer: ArrayList<SearchResult> by lazy {
        val items = FunctionEntryRouter.queryAnnotatedUiItemAgentEntries()
        ArrayList<SearchResult>(items.size).apply {
            items.forEach {
                add(SearchResult(it))
            }
        }
    }

    override fun getTitle() = "搜索"

    @UiThread
    fun updateSearchResultForView() {
        binding?.let {
            if (currentKeyword.isEmpty()) {
                it.searchSettingNoResultLayout.visibility = View.GONE
                it.searchSettingSearchResultLayout.visibility = View.GONE
                it.searchSettingSearchHistoryLayout.visibility = View.VISIBLE
            } else {
                if (searchResults.isEmpty()) {
                    it.searchSettingNoResultLayout.visibility = View.VISIBLE
                    it.searchSettingSearchResultLayout.visibility = View.GONE
                    it.searchSettingSearchHistoryLayout.visibility = View.GONE
                } else {
                    it.searchSettingNoResultLayout.visibility = View.GONE
                    it.searchSettingSearchResultLayout.visibility = View.VISIBLE
                    it.searchSettingSearchHistoryLayout.visibility = View.GONE
                    mRecyclerAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private class SearchResultViewHolder(val binding: SearchResultItemBinding) : RecyclerView.ViewHolder(binding.root)

    private val mRecyclerAdapter = object : RecyclerView.Adapter<SearchResultViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
            return SearchResultViewHolder(SearchResultItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
            val item = searchResults[position]
            bindSearchResultItem(holder.binding, item)
        }

        override fun getItemCount(): Int {
            return searchResults.size
        }
    }

    private fun bindSearchResultItem(binding: SearchResultItemBinding, item: SearchResult) {
        val title: String = item.agent.uiItemAgent.titleProvider.invoke(item.agent.uiItemAgent)
        val description: String = "[${item.score}] " +
                (item.agent.uiItemAgent.summaryProvider?.invoke(item.agent.uiItemAgent, requireContext()).orEmpty())
        binding.title.text = title
        binding.summary.text = description
        val locationString = item.shownLocation!!.joinToString(separator = " > ")
        binding.description.text = locationString
        binding.root.setTag(R.id.tag_searchResultItem, item)
        binding.root.setOnClickListener(mSearchResultOnClickListener)
    }

    private val mSearchResultOnClickListener = View.OnClickListener { v ->
        val item = v?.getTag(R.id.tag_searchResultItem) as SearchResult?
        item?.let {
            navigateToTargetSearchResult(it)
        }
    }

    @NonUiThread
    private fun doFullTextFunctionSearch() {
        val currKeyword = currentKeyword
        // search is performed by calculating the score of each item and sort the result by the score
        val keywords: List<String> = currentKeyword.replace("\r", "")
                .replace("\n", "").replace("\t", "")
                .split(" ").filter { it.isNotBlank() && it.isNotEmpty() }
        // update the score of each item
        allItemsContainer.forEach {
            it.score = 0
            keywords.forEach { keyword ->
                it.score += calculatePartialScoreBySingleKeyword(keyword, it.agent.uiItemAgent)
            }
        }
        // find score > 0
        searchResults.clear()
        allItemsContainer.forEach {
            if (it.score > 0) {
                searchResults.add(it)
            }
        }
        // sort by score
        searchResults.sortByDescending { it.score }
        // update the item location if missing
        searchResults.forEach {
            if (it.location == null) {
                updateUiItemAgentLocation(it)
            }
        }
        lastSearchKeyword = currKeyword
        // update the view
        SyncUtils.runOnUiThread { updateSearchResultForView() }
    }

    private fun calculatePartialScoreBySingleKeyword(keyword: String, item: IUiItemAgent): Int {
        var score = 0
        val context = requireContext()
        val title: String = item.titleProvider.invoke(item).replace(" ", "")
        val summary: String? = item.summaryProvider?.invoke(item, context)?.replace(" ", "")?.replace("\n", "")
        val extraKeywords: List<String>? = item.extraSearchKeywordProvider?.invoke(item, context)
        if (title == keyword) {
            score += 80
        } else if (title.contains(keyword, true)) {
            score += 50
        }
        summary?.let {
            if (it == keyword) {
                score += 40
            } else if (it.contains(keyword, true)) {
                score += 20
            }
        }
        extraKeywords?.let { words ->
            words.forEach {
                if (it == keyword) {
                    score += 10
                } else if (it.contains(keyword, true)) {
                    score += 5
                }
            }
        }
        return score
    }

    private fun updateUiItemAgentLocation(item: SearchResult) {
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

    private data class SearchResult(
            val agent: IUiItemAgentProvider,
            var score: Int = 0,
            var location: Array<String>? = null,
            var shownLocation: Array<String>? = null
    )

    @UiThread
    private fun navigateToTargetSearchResult(item: SearchResult) {
        if (item.location == null) {
            updateUiItemAgentLocation(item)
        }
        // find containing fragment
        val absFullLocation = item.location!!
        val identifier = absFullLocation.last()
        var container = absFullLocation.dropLast(1)
        var targetFragmentLocation: Array<String>? = null
        var node = FunctionEntryRouter.settingsUiItemDslTree.lookupHierarchy(container.toTypedArray())
        // lookup the parent container, until we find the parent is a fragment
        while (true) {
            if (node == null) {
                // we are lost!!!
                break
            }
            if (node is FragmentDescription) {
                // found
                targetFragmentLocation = container.toTypedArray()
                break
            }
            if (container.isEmpty()) {
                // we are lost!!!
                break
            }
            // not a fragment, keep looking up parent
            container = container.dropLast(1)
            // get current node
            node = FunctionEntryRouter.settingsUiItemDslTree.lookupHierarchy(container.toTypedArray())
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSettingSearchBinding.inflate(inflater, container, false).apply {
            searchSettingSearchResultRecyclerView.apply {
                adapter = mRecyclerAdapter
                layoutManager = LinearLayoutManager(inflater.context).apply {
                    orientation = LinearLayoutManager.VERTICAL
                }
                id = R.id.fragmentMainRecyclerView // id is used to allow saving state
                itemAnimator = null
                layoutAnimation = null
            }
            searchKeyWords.addTextChangedListener {
                currentKeyword = it.toString()
                if (currentKeyword != lastSearchKeyword) {
                    SyncUtils.async { doFullTextFunctionSearch() }
                }
            }
        }
        updateSearchResultForView()
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
