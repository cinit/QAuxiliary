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
import cc.ioctl.util.ui.ThemeAttrUtils
import io.github.qauxv.R
import io.github.qauxv.activity.SettingsUiFragmentHostActivity
import io.github.qauxv.base.IDynamicHook
import io.github.qauxv.base.IUiItemAgentProvider
import io.github.qauxv.databinding.ItemFuncStatusBinding
import io.github.qauxv.dsl.FunctionEntryRouter
import kotlin.math.max

class FuncStatListFragment : BaseRootLayoutFragment() {

    private var mRecyclerView: RecyclerView? = null
    private var mFunction: ArrayList<IUiItemAgentProvider> = ArrayList(160)

    override fun getTitle() = "功能状态"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            val r1 = calculateUiItemRank(o1)
            val r2 = calculateUiItemRank(o2)
            // descending order, so that the highest rank is at the top
            return@Comparator r2 - r1
        })
        // all items may be changed, so we need to update the adapter
        mRecyclerView?.adapter?.notifyDataSetChanged()
        // statistics
        var errorCount = 0
        var warningCount = 0
        for (item in mFunction) {
            val rank = calculateUiItemRank(item)
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
        val enabled = item is IDynamicHook && item.isEnabled
        val reportError = item is IDynamicHook && item.isInitialized && !item.isInitializationSuccessful
        val errorCount = if (item is IDynamicHook) item.runtimeErrors.size else 0
        val unsupported = item is IDynamicHook && !item.isAvailable
        val tags = ArrayList<String>()
        var iconType = 0
        if (reportError) {
            iconType = 5
            tags.add("存在错误")
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

    private fun calculateUiItemRank(item: IUiItemAgentProvider): Int {
        // enabled +1
        val enabled = item is IDynamicHook && item.isEnabled
        // report error +70
        val reportError = item is IDynamicHook && item.isInitialized && !item.isInitializationSuccessful
        val errorCount = if (item is IDynamicHook) item.runtimeErrors.size else 0
        // a error is 10, max is 50
        val errorRank = (errorCount * 10).coerceAtMost(50)
        // unsupported +2
        val unsupported = item is IDynamicHook && !item.isAvailable
        // sum up
        return (if (enabled) 2 else 0) + (if (reportError) 70 else 0) + errorRank + (if (unsupported) 1 else 0)
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
}
