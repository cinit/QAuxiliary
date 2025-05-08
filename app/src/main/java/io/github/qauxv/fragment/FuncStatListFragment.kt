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

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cc.ioctl.util.LayoutHelper
import cc.ioctl.util.Reflex
import cc.ioctl.util.ui.ThemeAttrUtils
import io.github.qauxv.R
import io.github.qauxv.activity.SettingsUiFragmentHostActivity
import io.github.qauxv.base.IDynamicHook
import io.github.qauxv.base.IUiItemAgentProvider
import io.github.qauxv.base.RuntimeErrorTracer
import io.github.qauxv.databinding.ItemFuncStatusBinding
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.dsl.func.IDslFragmentNode
import io.github.qauxv.util.Log
import kotlin.math.max

class FuncStatListFragment : BaseRootLayoutFragment() {

    private var mRecyclerView: RecyclerView? = null
    private var mFunction: ArrayList<IUiItemAgentProvider> = ArrayList(160)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "功能状态"
        for (entry in FunctionEntryRouter.queryAnnotatedUiItemAgentEntries()) {
            mFunction.add(entry)
        }
    }

    override fun onResume() {
        super.onResume()
        sortAndUpdateStatus()
    }

    @UiThread
    @SuppressLint("NotifyDataSetChanged")
    private fun sortAndUpdateStatus() {
        mFunction.sortWith(Comparator { o1, o2 ->
            val r1: Int = kotlin.runCatching { calculateUiItemRank(o1) }.getOrDefault(114514)
            val r2: Int = kotlin.runCatching { calculateUiItemRank(o2) }.getOrDefault(114514)
            // descending order, so that the highest rank is at the top
            return@Comparator r2 - r1
        })
        // all items may be changed, so we need to update the adapter
        mRecyclerView?.adapter?.notifyDataSetChanged()
        // statistics
        var errorCount = 0
        var warningCount = 0
        for (item in mFunction) {
            val rank = kotlin.runCatching { calculateUiItemRank(item) }.getOrDefault(114514)
            if (rank >= 70) {
                errorCount++
            } else if (rank >= 10) {
                warningCount++
            }
        }
        val text = "$errorCount / $warningCount / ${mFunction.size} (E/W/T)"
        subtitle = text
    }

    private class FuncStatViewHolder(val binding: ItemFuncStatusBinding) : RecyclerView.ViewHolder(binding.root)

    private val mAdapter: RecyclerView.Adapter<FuncStatViewHolder> = object : RecyclerView.Adapter<FuncStatViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FuncStatViewHolder {
            val binding = ItemFuncStatusBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            binding.root.setOnClickListener(mOnItemClickListener)
            binding.root.setOnLongClickListener(mOnItemLongClickListener)
            return FuncStatViewHolder(binding)
        }

        override fun getItemCount() = mFunction.size

        override fun onBindViewHolder(holder: FuncStatViewHolder, position: Int) {
            val item = mFunction[position]
            updateViewItem(holder, item)
        }
    }

    @UiThread
    private fun updateViewItem(vh: FuncStatViewHolder, item: IUiItemAgentProvider) {
        val title = item.uiItemAgent.titleProvider(item.uiItemAgent)
        var iconType = 0
        val tags = ArrayList<String>()
        kotlin.runCatching {
            val enabled = item is IDynamicHook && item.isEnabled
            val reportError = item is IDynamicHook && item.isInitialized && !item.isInitializationSuccessful
            val errorCount = if (item is RuntimeErrorTracer) collectFunctionErrors(item).size else 0
            val unsupported = item is IDynamicHook && !item.isAvailable
            val isDepError = item is IDynamicHook && (item.isEnabled && item.isPreparationRequired)
            if (reportError) {
                iconType = 5
                tags.add("存在错误")
            }
            if (isDepError) {
                iconType = 5
                tags.add("依赖错误")
            }
            if (errorCount != 0) {
                iconType = max(iconType, 4)
                tags.add("$errorCount 个异常")
            }
            if (unsupported) {
                iconType = max(iconType, 3)
                tags.add("不兼容当前版本")
            }
            if (tags.isEmpty()) {
                if (enabled) {
                    iconType = 1
                    tags.add("未见明显异常")
                } else {
                    iconType = 2
                    tags.add("未启用")
                }
            }
        }.onFailure {
            Log.e("FuncStatListFragment updateViewItem", it)
            iconType = 5
            tags.add(Reflex.getShortClassName(it) + it.message.toString())
        }
        val desc = tags.joinToString(", ")
        vh.binding.apply {
            root.tag = item
            textTitle.text = title
            textStatus.text = desc
            val ctx = requireContext()
            when (iconType) {
                5 -> {
                    iconStatus.setImageResource(R.drawable.ic_error_filled)
                    iconStatus.setColorFilter(ThemeAttrUtils.resolveColorOrDefaultColorRes(ctx, R.attr.unusableColor, R.color.thirdTextColor))
                }
                4 -> {
                    iconStatus.setImageResource(R.drawable.ic_warn_filled)
                    iconStatus.setColorFilter(ThemeAttrUtils.resolveColorOrDefaultColorRes(ctx, R.attr.warnColor, R.color.thirdTextColor))
                }
                3 -> {
                    iconStatus.setImageResource(R.drawable.ic_info)
                    iconStatus.setColorFilter(ResourcesCompat.getColor(ctx.resources, R.color.thirdTextColor, ctx.theme))
                }
                2 -> {
                    iconStatus.setImageResource(R.drawable.ic_close_24)
                    iconStatus.setColorFilter(ResourcesCompat.getColor(ctx.resources, R.color.thirdTextColor, ctx.theme))
                }
                else -> {
                    iconStatus.setImageResource(R.drawable.ic_done)
                    iconStatus.setColorFilter(ResourcesCompat.getColor(ctx.resources, R.color.thirdTextColor, ctx.theme))
                }
            }
        }
    }

    private val mOnItemClickListener = View.OnClickListener { v ->
        val item = v.tag as? IUiItemAgentProvider
        if (item != null) {
            val identifier = item.itemAgentProviderUniqueIdentifier
            SettingsUiFragmentHostActivity.startFragmentWithContext(
                v.context,
                FuncStatusDetailsFragment::class.java,
                FuncStatusDetailsFragment.getBundleForLocation(identifier)
            )
        }
    }

    private val mOnItemLongClickListener = View.OnLongClickListener { v ->
        val itemAgentProvider = v.tag as? IUiItemAgentProvider
        if (itemAgentProvider != null) {
            val hostActivity = activity as? SettingsUiFragmentHostActivity ?: return@OnLongClickListener false

            val identifier = itemAgentProvider.itemAgentProviderUniqueIdentifier
            val baseContainerLocation = FunctionEntryRouter.resolveUiItemAnycastLocation(itemAgentProvider.uiItemLocation)
                ?: itemAgentProvider.uiItemLocation

            val absFullLocation = arrayOf(*baseContainerLocation, identifier)
            var containerForFragmentLookup = absFullLocation.dropLast(1).toTypedArray()
            var targetFragmentLocation: Array<String>? = null
            var node = FunctionEntryRouter.settingsUiItemDslTree.lookupHierarchy(containerForFragmentLookup)

            while (true) {
                if (node == null) {
                    // Should not happen if uiItemLocation is correct
                    Log.w("Failed to find node for location: " + containerForFragmentLookup.joinToString())
                    break
                }
                if (node is IDslFragmentNode) {
                    targetFragmentLocation = containerForFragmentLookup
                    break
                }
                if (containerForFragmentLookup.isEmpty()) {
                    // Reached root without finding a fragment node
                    Log.w("Reached root while looking for fragment node for: " + absFullLocation.joinToString())
                    break
                }
                containerForFragmentLookup = containerForFragmentLookup.dropLast(1).toTypedArray()
                node = FunctionEntryRouter.settingsUiItemDslTree.lookupHierarchy(containerForFragmentLookup)
            }

            if (targetFragmentLocation != null) {
                val fragment = SettingsMainFragment.newInstance(targetFragmentLocation, identifier)
                hostActivity.presentFragment(fragment)
                return@OnLongClickListener true
            } else {
                Log.e("Could not determine targetFragmentLocation for " + itemAgentProvider.itemAgentProviderUniqueIdentifier)
                // Fallback or error message if needed
            }
        }
        false
    }

    private fun calculateUiItemRank(item: IUiItemAgentProvider): Int {
        // enabled +1
        val enabled = item is IDynamicHook && item.isEnabled
        // report error +70
        val reportError = item is IDynamicHook && item.isInitialized && !item.isInitializationSuccessful
        // dep error +60
        val isDepError = item is IDynamicHook && (item.isEnabled && item.isPreparationRequired)
        val errorCount = if (item is RuntimeErrorTracer) collectFunctionErrors(item).size else 0
        // a error is 10, max is 50
        val errorRank = (errorCount * 10).coerceAtMost(50)
        // unsupported +2
        val unsupported = item is IDynamicHook && !item.isAvailable
        // sum up
        return (if (enabled) 2 else 0) + (if (isDepError) 60 else 0) + (if (reportError) 70 else 0) + errorRank + (if (unsupported) 1 else 0)
    }

    override fun doOnCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mRecyclerView = RecyclerView(inflater.context).apply {
            layoutManager = LinearLayoutManager(inflater.context, RecyclerView.VERTICAL, false)
            adapter = mAdapter
            LayoutHelper.initializeScrollbars(this)
            isVerticalScrollBarEnabled = true
        }
        rootLayoutView = mRecyclerView
        return mRecyclerView!!
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mRecyclerView?.adapter = null
        mRecyclerView = null
    }

    companion object {

        @JvmStatic
        fun collectFunctionWithDependencies(hook: RuntimeErrorTracer): Set<RuntimeErrorTracer> {
            val set = mutableSetOf(hook)
            var lastSize: Int
            do {
                lastSize = set.size
                val toAdd = set.flatMap { it.runtimeErrorDependentComponents ?: emptyList() }
                set.addAll(toAdd)
            } while (set.size != lastSize)
            return set
        }

        @JvmStatic
        fun collectFunctionErrors(hook: RuntimeErrorTracer): Set<Throwable> {
            val all = collectFunctionWithDependencies(hook)
            return all.flatMap { it.runtimeErrors }.toSet()
        }

    }
}
