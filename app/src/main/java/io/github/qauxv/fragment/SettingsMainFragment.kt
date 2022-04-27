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
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cc.ioctl.util.LayoutHelper.MATCH_PARENT
import cc.ioctl.util.ui.drawable.BackgroundDrawableUtils
import io.github.qauxv.R
import io.github.qauxv.SyncUtils
import io.github.qauxv.SyncUtils.async
import io.github.qauxv.SyncUtils.runOnUiThread
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.IUiItemAgentProvider
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.core.MainHook
import io.github.qauxv.databinding.SearchResultItemBinding
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.dsl.func.CategoryDescription
import io.github.qauxv.dsl.func.FragmentDescription
import io.github.qauxv.dsl.func.IDslFragmentNode
import io.github.qauxv.dsl.func.IDslItemNode
import io.github.qauxv.dsl.func.IDslParentNode
import io.github.qauxv.dsl.func.UiItemAgentDescription
import io.github.qauxv.dsl.item.CategoryItem
import io.github.qauxv.dsl.item.DslTMsgListItemInflatable
import io.github.qauxv.dsl.item.SimpleListItem
import io.github.qauxv.dsl.item.TMsgListItem
import io.github.qauxv.dsl.item.UiAgentItem
import io.github.qauxv.util.Log
import io.github.qauxv.util.NonUiThread
import io.github.qauxv.util.UiThread
import kotlinx.coroutines.flow.MutableStateFlow

class SettingsMainFragment : BaseRootLayoutFragment() {

    override fun getTitle() = title
    private var title: String = "QAuxiliary"
    private lateinit var mFragmentLocations: Array<String>
    private lateinit var mFragmentDescription: FragmentDescription
    private var mTargetUiAgentNavId: String? = null
    private var mTargetUiAgentNavigated: Boolean = false

    // DSL stuff below
    private var adapter: RecyclerView.Adapter<*>? = null
    private var searchAdapter: SearchAdapter? = null
    private var emptyAdapter: SearchAdapter? = null
    private var listLayoutManager: LinearLayoutManager? = null
    private var recyclerListView: RecyclerView? = null
    private var rootFrameLayout: FrameLayout? = null

    private lateinit var typeList: Array<Class<*>>
    private lateinit var itemList: ArrayList<TMsgListItem>
    private lateinit var itemTypeIds: Array<Int>
    private lateinit var itemTypeDelegate: Array<TMsgListItem>

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
        mTargetUiAgentNavId = arguments?.getString(TARGET_UI_AGENT_IDENTIFIER)
    }

    override fun doOnCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = layoutInflater.context
        val rootView = FrameLayout(context)
        rootFrameLayout = rootView
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
        this@SettingsMainFragment.listLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        // init view
        recyclerListView = RecyclerView(context).apply {
            id = R.id.fragmentMainRecyclerView // id is used to allow saving state
            layoutManager = this@SettingsMainFragment.listLayoutManager
            clipToPadding = false
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
        searchAdapter = SearchAdapter(this)
        recyclerListView!!.adapter = adapter

        // collect all StateFlow and observe them in case of state change
        for (i in itemList.indices) {
            val item = itemList[i]
            if (item is UiAgentItem) {
                val valueStateFlow: MutableStateFlow<String?>? = item.agentProvider.uiItemAgent.valueState
                if (valueStateFlow != null) {
                    lifecycleScope.launchWhenResumed {
                        valueStateFlow.collect {
                            runOnUiThread { adapter?.notifyItemChanged(i) }
                        }
                    }
                }
            }
        }
        rootLayoutView = recyclerListView
        rootView.addView(recyclerListView!!, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        return rootView
    }

    override fun onResume() {
        super.onResume()
        if (!mTargetUiAgentNavId.isNullOrEmpty() && !mTargetUiAgentNavigated) {
            navigateToTargetUiAgentItem()
        }
        val safeMode = ConfigManager.getDefaultConfig().getBooleanOrDefault(MainHook.KEY_SAFE_MODE, false)
        subtitle = if (safeMode) {
            "安全模式（停用所有功能）"
        } else {
            null
        }
    }

    @UiThread
    private fun navigateToTargetUiAgentItem() {
        if (!isResumed) {
            return
        }
        // wait for the view to be created and animation to finish
        SyncUtils.postDelayed(300) {
            var index = -1
            // find the UI agent index
            for (i in itemList.indices) {
                val item = itemList[i]
                if (item is UiAgentItem && item.agentProvider.itemAgentProviderUniqueIdentifier == mTargetUiAgentNavId) {
                    index = i
                    break
                }
            }
            if (index >= 0) {
                // scroll it to the center
                val layoutManager = recyclerListView!!.layoutManager as LinearLayoutManager
                val firstVisibleItemPosition: Int = layoutManager.findFirstVisibleItemPosition()
                val lastVisibleItemPosition: Int = layoutManager.findLastVisibleItemPosition()
                val centerPosition = (firstVisibleItemPosition + lastVisibleItemPosition) / 2
                var scrollTargetIndex = index
                if (scrollTargetIndex > centerPosition) {
                    scrollTargetIndex++
                } else if (scrollTargetIndex < centerPosition) {
                    scrollTargetIndex--
                }
                if (scrollTargetIndex < 0) {
                    scrollTargetIndex = 0
                }
                if (scrollTargetIndex > itemList.size - 1) {
                    scrollTargetIndex = itemList.size - 1
                }
                recyclerListView!!.scrollToPosition(scrollTargetIndex)
                SyncUtils.postDelayed(100) {
                    // wait for scrolling to finish
                    val itemView = layoutManager.findViewByPosition(index)!!
                    // calculate the position of the item, startY and endY of the recyclerView
                    val startY = itemView.top
                    val endY = itemView.bottom
                    val width = itemView.width
                    val rect = Rect(0, startY, width, endY)
                    highlightRect(rect)
                }
                mTargetUiAgentNavigated = true
            }
        }
    }

    @UiThread
    private fun highlightRect(rect: Rect) {
        if (!isResumed) {
            return
        }
        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 300
            fillAfter = true
        }
        val fadeOut = AlphaAnimation(1f, 0f).apply {
            duration = 300
            fillAfter = true
        }
        val view = View(context).apply {
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = false
            isLongClickable = false
            setBackgroundColor(ResourcesCompat.getColor(context!!.resources, R.color.rippleColor, context!!.theme))
        }
        val layoutParams = FrameLayout.LayoutParams(rect.width(), rect.height()).apply {
            setMargins(rect.left, rect.top, 0, 0)
        }
        val parent = rootFrameLayout ?: return
        async {
            var isAddedToParent = false
            // in out and repeat
            for (i in 0..3) {
                val animation = if (i % 2 == 0) {
                    fadeIn
                } else {
                    fadeOut
                }
                runOnUiThread {
                    if (!isAddedToParent) {
                        parent.addView(view, layoutParams)
                        isAddedToParent = true
                    }
                    view.startAnimation(animation)
                }
                Thread.sleep(300)
            }
            runOnUiThread {
                rootFrameLayout?.removeView(view)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerListView?.let {
            it.adapter = null
        }
        recyclerListView = null
        rootFrameLayout = null
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
        } else if (endNode is IDslFragmentNode) {
            return SimpleListItem(endNode.identifier, endNode.name ?: endNode.toString(), null).apply {
                onClickListener = {
                    val targetLocation = FunctionEntryRouter.resolveUiItemAnycastLocation(
                        arrayOf(
                            FunctionEntryRouter.Locations.ANY_CAST_PREFIX, endNode.identifier
                        )
                    )?: throw IllegalStateException("can not resolve anycast location for '${endNode.identifier}'")
                    // jump to target fragment
                    val location: Array<String> = targetLocation
                    val fragmentClass = endNode.getTargetFragmentClass(location)
                    val bundle = endNode.getTargetFragmentArguments(location)
                    val fragment = fragmentClass.newInstance() as BaseSettingFragment
                    fragment.arguments = bundle
                    settingsHostActivity!!.presentFragment(fragment)
                }
            }
        }
        throw UnsupportedOperationException("unsupported node type: " + endNode.javaClass.name)
    }

    private fun IDslItemNode.isEndNode(): Boolean {
        return this !is IDslParentNode || this is IDslFragmentNode
    }

    private fun isRootFragmentDescription(): Boolean {
        return mFragmentLocations.isEmpty() || mFragmentLocations.size == 1 && mFragmentLocations[0].isEmpty()
    }

    private fun addHeaderItemToRootDslTree(dslTree: ArrayList<DslTMsgListItemInflatable>) {
        // TODO: 2022-02-22 add flavor to root dsl tree
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.main_settings_toolbar, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_item_action_search) {
            // always use global search, or search in current fragment?
            val searchView = item.actionView as SearchView
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    searchAdapter?.search(query)
                    return false
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    searchAdapter?.search(newText)
                    return false
                }

            })
            searchView.setOnCloseListener {
                Log.d("setOnCloseListener")
                searchAdapter?.search(null)
                false
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private class SearchAdapter(private val fragment: SettingsMainFragment) : RecyclerView.Adapter<SearchResultViewHolder>() {
        private var currentKeyword: String = ""
        private var lastSearchKeyword: String = ""
        private val searchResults: ArrayList<SearchResult> = ArrayList()
        private val allItemsContainer: List<SearchResult> by lazy {
            FunctionEntryRouter.queryAnnotatedUiItemAgentEntries().map { SearchResult(it) }
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
            runOnUiThread { updateSearchResultForView() }
        }

        private fun updateSearchResultForView() {
            runOnUiThread {
                if(searchResults.isNotEmpty()) {
                    fragment.recyclerListView!!.adapter = this
                } else {
                    // TODO: 2020-02-22 show empty view
                    fragment.recyclerListView!!.adapter = fragment.adapter
                }
            }
        }

        private fun calculatePartialScoreBySingleKeyword(keyword: String, item: IUiItemAgent): Int {
            var score = 0
            val context = fragment.requireContext()
            val title: String = item.titleProvider.invoke(item).replace(" ", "")
            val summary: String? = item.summaryProvider?.invoke(item, context)?.replace(" ", "")?.replace("\n", "")
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

        @UiThread
        private fun navigateToTargetSearchResult(item: SearchResult) {
            ConfigEntrySearchHistoryManager.addHistory(currentKeyword)
            // todo: 2020-02-22 support history
            // updateHistoryListForView()
            if (item.location == null) {
                updateUiItemAgentLocation(item)
            }
            // find containing fragment
            val absFullLocation = item.location!!
            val identifier = absFullLocation.last()
            var container = absFullLocation.dropLast(1)
            var targetFragmentLocation: Array<String>? = null
            val context = fragment.requireContext()
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
                AlertDialog.Builder(context).apply {
                    setTitle("Navigation Error")
                    setMessage("We are lost, can't find the target fragment: " + absFullLocation.joinToString("."))
                    setPositiveButton(android.R.string.ok) { _, _ -> }
                    setCancelable(true)
                }.show()
            } else {
                // hide IME
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(fragment.requireView().windowToken, 0)
                val fragment = newInstance(targetFragmentLocation, identifier)
                this.fragment.settingsHostActivity!!.presentFragment(fragment)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
            return SearchResultViewHolder(SearchResultItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
            val item = searchResults[position]
            bindSearchResultItem(holder.binding, item)
        }

        private fun bindSearchResultItem(binding: SearchResultItemBinding, item: SearchResult) {
            val title: String = item.agent.uiItemAgent.titleProvider.invoke(item.agent.uiItemAgent)
            val description: String = "[${item.score}] " +
                (item.agent.uiItemAgent.summaryProvider?.invoke(item.agent.uiItemAgent, fragment.requireContext()).orEmpty())
            binding.title.text = title
            binding.summary.text = description
            val locationString = item.shownLocation!!.joinToString(separator = " > ")
            binding.description.text = locationString
            binding.root.setTag(R.id.tag_searchResultItem, item)
            binding.root.setOnClickListener { v ->
                (v.getTag(R.id.tag_searchResultItem) as SearchResult?)
                    ?.let {
                        navigateToTargetSearchResult(it)
                    }
            }
        }

        override fun getItemCount() = searchResults.size
    }

    private class SearchResultViewHolder(val binding: SearchResultItemBinding) : RecyclerView.ViewHolder(binding.root)

    private data class SearchResult(
        val agent: IUiItemAgentProvider,
        var score: Int = 0,
        var location: Array<String>? = null,
        var shownLocation: Array<String>? = null
    )

    companion object {
        const val TARGET_FRAGMENT_LOCATION = "SettingsMainFragment.TARGET_FRAGMENT_LOCATION"
        const val TARGET_UI_AGENT_IDENTIFIER = "SettingsMainFragment.TARGET_UI_AGENT_IDENTIFIER"

        @JvmStatic
        @JvmOverloads
        fun newInstance(location: Array<String>, targetUiAgentId: String? = null): SettingsMainFragment {
            val fragment = SettingsMainFragment()
            val bundle = getBundleForLocation(location, targetUiAgentId)
            fragment.arguments = bundle
            return fragment
        }

        @JvmStatic
        @JvmOverloads
        fun getBundleForLocation(location: Array<String>, targetUiAgentId: String? = null): Bundle {
            // check destination fragment
            val desc = FunctionEntryRouter.findDescriptionByLocation(location)
                ?: throw IllegalArgumentException("unable to find fragment description by location: " + location.contentToString())
            if (desc !is FragmentDescription) {
                throw IllegalArgumentException("fragment description is not FragmentDescription, got: " + desc.javaClass.name)
            }
            val bundle = Bundle()
            bundle.putStringArray(TARGET_FRAGMENT_LOCATION, location)
            targetUiAgentId?.let { bundle.putString(TARGET_UI_AGENT_IDENTIFIER, it) }
            return bundle
        }
    }
}
