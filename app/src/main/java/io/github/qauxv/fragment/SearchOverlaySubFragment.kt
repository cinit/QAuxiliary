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
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cc.ioctl.util.LayoutHelper
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import io.github.qauxv.R
import io.github.qauxv.SyncUtils
import io.github.qauxv.activity.SettingsUiFragmentHostActivity
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.IUiItemAgentProvider
import io.github.qauxv.databinding.FragmentSettingSearchBinding
import io.github.qauxv.databinding.SearchResultItemBinding
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.dsl.func.IDslFragmentNode
import io.github.qauxv.dsl.func.IDslParentNode
import io.github.qauxv.util.NonUiThread
import io.github.qauxv.util.UiThread

/**
 * The search sub fragment of [SettingsMainFragment]
 */
class SearchOverlaySubFragment {

    var parent: SettingsMainFragment? = null
    var arguments: Bundle? = null
    var context: Context? = null
    var settingsHostActivity: SettingsUiFragmentHostActivity? = null
    private var mView: View? = null

    private var mSearchView: SearchView? = null
    private var binding: FragmentSettingSearchBinding? = null
    private var currentKeyword: String = ""
    private var lastSearchKeyword: String = ""
    private var searchHistoryList: List<String> = listOf()
    private val searchResults: ArrayList<SearchResult> = ArrayList()
    private val allItemsContainer: ArrayList<SearchResult> by lazy {
        val items = FunctionEntryRouter.queryAnnotatedUiItemAgentEntries()
        ArrayList<SearchResult>(items.size).apply {
            items.forEach {
                add(SearchResult(it))
            }
        }
    }

    fun requireContext(): Context {
        return context!!
    }

    fun requireActivity(): SettingsUiFragmentHostActivity {
        return settingsHostActivity!!
    }

    fun requireView(): View {
        return mView!!
    }

    fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = doOnCreateView(inflater, container, savedInstanceState)
        return mView
    }

    fun initForSearchView(searchView: SearchView) {
        mSearchView = searchView.apply {
            queryHint = "搜索..."
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    search(newText)
                    return false
                }
            })

            setIconifiedByDefault(false)
            findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon).apply {
                setImageDrawable(null)
            }
            // hide search plate
            findViewById<View>(androidx.appcompat.R.id.search_plate).apply {
                setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    @UiThread
    fun updateSearchResultForView() {
        binding?.let {
            if (currentKeyword.isEmpty()) {
                it.searchSettingNoResultLayout.visibility = View.GONE
                it.searchSettingSearchResultLayout.visibility = View.GONE
                if (searchHistoryList.isNotEmpty()) {
                    it.searchSettingSearchHistoryLayout.visibility = View.VISIBLE
                }
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

    private val mOnSearchHistoryItemClickListener = View.OnClickListener {
        val keyword = (it as TextView).text.toString()
        if (keyword != currentKeyword && keyword.isNotEmpty()) {
            mSearchView!!.setQuery(keyword, true)
            currentKeyword = keyword
        }
    }

    private val mOnSearchHistoryItemLongClickListener = View.OnLongClickListener {
        val keyword = (it as TextView).text.toString()
        if (keyword.isNotEmpty()) {
            AlertDialog.Builder(context!!)
                .setTitle("删除搜索历史")
                .setMessage("确定要删除搜索历史 '$keyword' 吗？")
                .setPositiveButton("确定") { _, _ ->
                    ConfigEntrySearchHistoryManager.removeHistory(keyword)
                    updateHistoryListForView()
                }
                .setCancelable(true)
                .setNegativeButton("取消", null)
                .show()
            true
        } else {
            false
        }
    }

    private class SearchHistoryItemViewHolder(val context: Context, r: TextView) : RecyclerView.ViewHolder(r) {
        val textView: TextView = r

        companion object {
            @JvmStatic
            fun newInstance(that: SearchOverlaySubFragment): SearchHistoryItemViewHolder {
                val context = that.requireContext()
                val v = TextView(context).apply {
                    textSize = 14f
                    isClickable = true
                    isLongClickable = true
                    isFocusable = true
                    gravity = Gravity.CENTER
                    minHeight = LayoutHelper.dip2px(context, 32f)
                    val dp16 = LayoutHelper.dip2px(context, 16f)
                    setPadding(dp16, 0, dp16, 0)
                    setTextColor(ResourcesCompat.getColor(context.resources, R.color.firstTextColor, context.theme))
                    background = ResourcesCompat.getDrawable(context.resources, R.drawable.bg_item_light_grey_r16, context.theme)
                    this.setOnClickListener(that.mOnSearchHistoryItemClickListener)
                    this.setOnLongClickListener(that.mOnSearchHistoryItemLongClickListener)
                }
                return SearchHistoryItemViewHolder(context, v)
            }
        }
    }

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

    private val mHistoryRecyclerAdapter = object : RecyclerView.Adapter<SearchHistoryItemViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchHistoryItemViewHolder {
            return SearchHistoryItemViewHolder.newInstance(this@SearchOverlaySubFragment)
        }

        override fun onBindViewHolder(holder: SearchHistoryItemViewHolder, position: Int) {
            val keyword = searchHistoryList[position]
            holder.textView.text = keyword
        }

        override fun getItemCount(): Int {
            return searchHistoryList.size
        }
    }

    private fun bindSearchResultItem(binding: SearchResultItemBinding, item: SearchResult) {
        val title: String = item.agent.uiItemAgent.titleProvider.invoke(item.agent.uiItemAgent)
        val description: String = "[${item.score}] " +
            (item.agent.uiItemAgent.summaryProvider?.invoke(item.agent.uiItemAgent, requireContext())?:"")
        binding.title.text = title
        binding.summary.text = description
        val locationString = item.shownLocation!!.joinToString(separator = " > ")
        binding.description.text = locationString
        binding.root.setTag(R.id.tag_searchResultItem, item)
        binding.root.setOnClickListener{ v ->
            val result = v?.getTag(R.id.tag_searchResultItem) as SearchResult?
            result?.let {
                navigateToTargetSearchResult(it)
            }
        }
    }

    @NonUiThread
    fun search(query: String?) {
        if (query == lastSearchKeyword) return
        currentKeyword = query ?: ""
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
        lastSearchKeyword = currentKeyword
        // update the view
        SyncUtils.runOnUiThread { updateSearchResultForView() }
    }

    private fun calculatePartialScoreBySingleKeyword(keyword: String, item: IUiItemAgent): Int {
        var score = 0
        val context = requireContext()
        val title: String = item.titleProvider.invoke(item).replace(" ", "")
        val summary: String? = item.summaryProvider?.invoke(item, context)?.toString()?.replace(" ", "")?.replace("\n", "")
        val extraKeywords: Array<String>? = item.extraSearchKeywordProvider?.invoke(item, context)
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
        ConfigEntrySearchHistoryManager.addHistory(currentKeyword)
        updateHistoryListForView()
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
            if (node is IDslFragmentNode) {
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
            // hide IME
            val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(requireView().windowToken, 0)
            val fragment = SettingsMainFragment.newInstance(targetFragmentLocation, identifier)
            parent!!.onNavigateToOtherFragment()
            settingsHostActivity!!.presentFragment(fragment)
        }
    }

    private fun doOnCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSettingSearchBinding.inflate(inflater, container, false).apply {
            searchSettingSearchResultRecyclerView.apply {
                adapter = mRecyclerAdapter
                layoutManager = LinearLayoutManager(inflater.context).apply {
                    orientation = LinearLayoutManager.VERTICAL
                }
                id = R.id.fragmentMainRecyclerView // id is used to allow saving state
            }
            searchSettingSearchHistoryRecyclerView.apply {
                adapter = mHistoryRecyclerAdapter
                layoutManager = FlexboxLayoutManager(inflater.context).apply {
                    flexWrap = FlexWrap.WRAP
                    flexDirection = FlexDirection.ROW
                    justifyContent = JustifyContent.FLEX_START
                }
                val dp5 = LayoutHelper.dip2px(inflater.context, 5f)
                addItemDecoration(object : RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                        outRect.set(dp5, dp5, dp5, dp5)
                    }
                })
            }
            searchSettingClearHistory.setOnClickListener {
                AlertDialog.Builder(requireContext()).apply {
                    setTitle("清除历史记录")
                    setMessage("确定要清除历史记录吗？")
                    setPositiveButton(android.R.string.ok) { _, _ ->
                        ConfigEntrySearchHistoryManager.clearHistoryList()
                        searchHistoryList = ConfigEntrySearchHistoryManager.historyList
                        binding!!.searchSettingSearchHistoryLayout.visibility = View.GONE
                        mHistoryRecyclerAdapter.notifyDataSetChanged()
                    }
                    setNegativeButton(android.R.string.cancel) { _, _ -> }
                    setCancelable(true)
                }.show()
            }
        }
        updateHistoryListForView()
        updateSearchResultForView()
        return binding!!.root
    }

    private fun updateHistoryListForView() {
        searchHistoryList = ConfigEntrySearchHistoryManager.historyList
        if (searchHistoryList.isEmpty()) {
            binding!!.searchSettingSearchHistoryLayout.visibility = View.GONE
        }
        mHistoryRecyclerAdapter.notifyDataSetChanged()
    }

    fun onDestroyView() {
        binding = null
        mView = null
        mSearchView = null
    }

    fun onResume() = Unit
}
