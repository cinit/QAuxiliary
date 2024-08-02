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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.*
import android.view.animation.AlphaAnimation
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cc.ioctl.dialog.WsaWarningDialog
import cc.ioctl.util.LayoutHelper.MATCH_PARENT
import cc.ioctl.util.ui.drawable.BackgroundDrawableUtils
import io.github.qauxv.R
import io.github.qauxv.bridge.AppRuntimeHelper
import io.github.qauxv.bridge.ContactUtils
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.config.SafeModeManager
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.dsl.func.*
import io.github.qauxv.dsl.item.*
import io.github.qauxv.tips.newfeaturehint.NewFeatureIntroduceFragment
import io.github.qauxv.tips.newfeaturehint.NewFeatureManager
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.SyncUtils.async
import io.github.qauxv.util.SyncUtils.runOnUiThread
import io.github.qauxv.util.UiThread
import io.github.qauxv.util.hostInfo
import io.github.qauxv.util.isInHostProcess
import kotlinx.coroutines.flow.StateFlow
import me.singleneuron.util.forSuBanXia

class SettingsMainFragment : BaseRootLayoutFragment() {

    private lateinit var mFragmentLocations: Array<String>
    private lateinit var mFragmentDescription: FragmentDescription
    private var mTargetUiAgentNavId: String? = null
    private var mTargetUiAgentNavigated: Boolean = false

    // search
    private var mSearchSubFragment: SearchOverlaySubFragment? = null
    private var mSearchRootLayout: ViewGroup? = null
    private var mSearchMenuItem: MenuItem? = null

    // DSL stuff below
    private var adapter: RecyclerView.Adapter<*>? = null
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
        title = mFragmentDescription.name ?: "QAuxiliary"
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

        recyclerListView!!.adapter = adapter

        // collect all StateFlow and observe them in case of state change
        for (i in itemList.indices) {
            val item = itemList[i]
            if (item is UiAgentItem) {
                val valueStateFlow: StateFlow<String?>? = item.agentProvider.uiItemAgent.valueState
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
        if (isInHostProcess) {
            WsaWarningDialog.showWsaWarningDialogIfNecessary(requireContext())
        }
        return rootView
    }

    override fun onResume() {
        super.onResume()
        try {
            val buddyName = ContactUtils.getBuddyName(AppRuntimeHelper.getAppRuntime()!!, AppRuntimeHelper.getAccount())
            if ((buddyName?.contains("\u26A7\uFE0F") == true || buddyName?.contains("\uD83C\uDF65") == true) &&
                ConfigManager.forAccount(AppRuntimeHelper.getLongAccountUin()).getBoolean("ForSuBanXia", true)
            ) {
                AlertDialog.Builder(requireContext())
                    .setTitle(forSuBanXia.first)
                    .setMessage(forSuBanXia.second + "\n\n当你心情低落的时候，就在QA的搜索里输入MtF/FtM回来看看我吧！ ^_^")
                    .setPositiveButton("OK", null)
                    .create()
                    .show()
                ConfigManager.forAccount(AppRuntimeHelper.getLongAccountUin()).putBoolean("ForSuBanXia", false)
            }
        } catch (e: Exception) {
            //ignored
        }
        if (!mTargetUiAgentNavId.isNullOrEmpty() && !mTargetUiAgentNavigated) {
            navigateToTargetUiAgentItem()
        }
        val hostName = hostInfo.hostName
        val safeModeNow = SafeModeManager.getManager().isEnabledForThisTime
        val safeModeNextTime = SafeModeManager.getManager().isEnabledForNextTime
        subtitle = when {
            safeModeNow && safeModeNextTime -> "安全模式（停用所有功能）"
            !safeModeNow && safeModeNextTime -> "安全模式需要重启 $hostName 生效"
            safeModeNow && !safeModeNextTime -> "重新启动 $hostName 后将退出安全模式"
            else -> null
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
        val context = requireContext()
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
            setBackgroundColor(ResourcesCompat.getColor(context.resources, R.color.rippleColor, context.theme))
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
                    ) ?: throw IllegalStateException("can not resolve anycast location for '${endNode.identifier}'")
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
        if (NewFeatureManager.newFeatureTipEnabled) {
            val newFeatureCount = NewFeatureManager.queryNewFeatures()?.size ?: 0
            if (newFeatureCount > 0) {
                val newFeatureBanner = TextBannerItem(
                    text = "本次更新增加了 $newFeatureCount 项新功能，点击查看",
                    isCloseable = true,
                    onClick = {
                        val fragment = NewFeatureIntroduceFragment()
                        requireSettingsHostActivity().presentFragment(fragment)
                    }
                )
                // add to the beginning
                dslTree.add(0, newFeatureBanner)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.main_settings_toolbar, menu)
        menu.findItem(R.id.menu_item_action_search)?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                if (item.itemId == R.id.menu_item_action_search) {
                    exitSearchMode()
                }
                return true
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_item_action_search) {
            // always use global search, or search in current fragment?
            mSearchMenuItem = item
            val searchView = item.actionView as SearchView
            enterSearchMode(searchView)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun doOnBackPressed(): Boolean {
        return if (mSearchSubFragment != null) {
            mSearchMenuItem?.collapseActionView() ?: exitSearchMode()
            true
        } else {
            super.doOnBackPressed()
        }
    }

    /**
     * Enter search mode with animation
     */
    private fun enterSearchMode(searchView: SearchView) {
        if (mSearchSubFragment == null) {
            mSearchSubFragment = SearchOverlaySubFragment().also {
                it.parent = this
                it.context = this.requireContext()
                it.settingsHostActivity = settingsHostActivity!!

                mSearchRootLayout = it.onCreateView(requireActivity().layoutInflater, mSearchRootLayout, null) as ViewGroup
                it.onResume()
                // hide the recycler view and show the search view
                recyclerListView!!.animate().alpha(0f).setDuration(300).setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        recyclerListView!!.visibility = View.GONE
                    }
                }).start()
                rootFrameLayout!!.addView(mSearchRootLayout)
                mSearchRootLayout!!.alpha = 0f
                mSearchRootLayout!!.animate().alpha(1f).setDuration(300).setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        mSearchRootLayout!!.visibility = View.VISIBLE
                    }
                }).start()
                searchView.setOnCloseListener {
                    exitSearchMode()
                    true
                }
                rootLayoutView = mSearchRootLayout
                applyRootLayoutPaddingFor(mSearchRootLayout!!)
            }
        }
        mSearchSubFragment!!.initForSearchView(searchView)
    }

    /**
     * Exit search mode with animation
     */
    private fun exitSearchMode() {
        mSearchSubFragment?.let { fragment ->
            mSearchRootLayout!!.animate().alpha(0f).setDuration(300).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    rootFrameLayout!!.removeView(mSearchRootLayout)
                    fragment.onDestroyView()
                    mSearchRootLayout = null
                    mSearchSubFragment = null
                }
            }).start()
            recyclerListView!!.visibility = View.VISIBLE
            recyclerListView!!.alpha = 0f
            recyclerListView!!.animate().alpha(1f).setDuration(300).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    recyclerListView?.let { v ->
                        v.alpha = 1f
                    }
                }
            }).start()
        }
        rootLayoutView = recyclerListView
        applyRootLayoutPaddingFor(recyclerListView!!)
    }

    /**
     * Abort search mode without animation
     */
    private fun abortSearchMode() {
        mSearchSubFragment?.let {
            rootFrameLayout!!.removeView(mSearchRootLayout)
            it.onDestroyView()
            mSearchRootLayout = null
            mSearchSubFragment = null
        }
        recyclerListView!!.apply {
            alpha = 1f
            visibility = View.VISIBLE
        }
        rootLayoutView = recyclerListView
        applyRootLayoutPaddingFor(recyclerListView!!)
    }

    fun onNavigateToOtherFragment() {
        abortSearchMode()
    }

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
