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

package cc.ioctl.util.ui.dsl

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cc.ioctl.util.ui.drawable.BackgroundDrawableUtils
import io.github.qauxv.R
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.dsl.item.DslTMsgListItemInflatable
import io.github.qauxv.dsl.item.TMsgListItem
import io.github.qauxv.dsl.item.UiAgentItem
import kotlinx.coroutines.flow.StateFlow

class RecyclerListViewController(
        val context: Context,
        val lifecycleScope: LifecycleCoroutineScope,
) {

    // DSL stuff below

    // add, but never remove, item types to this list
    private val typeList: ArrayList<Class<*>> = ArrayList()
    private var itemList: ArrayList<TMsgListItem> = ArrayList()
    private var itemTypeIds: Array<Int> = emptyArray()
    var itemTypeDelegate: Array<TMsgListItem> = emptyArray()

    var recyclerListView: RecyclerView? = null


    var adapter: RecyclerView.Adapter<*>? = null
    val layoutManager: LinearLayoutManager by lazy {
        LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }

    var items: Array<DslTMsgListItemInflatable>? = null
        set(value) {
            field = value
            updateDslItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateDslItems() {
        // inflate DSL tree
        itemList = ArrayList()
        // inflate hierarchy recycler list view items, each item will have its own view holder type
        items?.forEach {
            itemList.addAll(it.inflateTMsgListItems(context))
        }
        // group items by java class
        val knownTypes = itemList.map { it.javaClass }.distinct().toTypedArray()
        // make sure that all knows types are added to typeList
        for (type in knownTypes) {
            if (type !in typeList) {
                typeList.add(type)
            }
        }
        // item id to type id mapping
        itemTypeIds = Array(itemList.size) {
            typeList.indexOf(itemList[it].javaClass)
        }
        // item type delegate is used to create view holder
        if (itemTypeDelegate.size != typeList.size) {
            val old = itemTypeDelegate
            itemTypeDelegate = Array(typeList.size) {
                if (it < old.size) {
                    old[it]
                } else {
                    itemList[itemTypeIds.indexOf(it)]
                }
            }
        }
        if (adapter != null && recyclerListView != null) {
            SyncUtils.runOnUiThread {
                adapter!!.notifyDataSetChanged()
            }
        }
        // collect all StateFlow and observe them in case of state change
        for (i in itemList.indices) {
            val item = itemList[i]
            if (item is UiAgentItem) {
                val valueStateFlow: StateFlow<String?>? = item.agentProvider.uiItemAgent.valueState
                if (valueStateFlow != null) {
                    lifecycleScope.launchWhenStarted {
                        valueStateFlow.collect {
                            SyncUtils.runOnUiThread { adapter?.notifyItemChanged(i) }
                        }
                    }
                }
            }
        }
    }

    fun initAdapter() {
        // init
        if (adapter == null) {
            // set adapter to default adapter if user does not set it
            adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
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
        }
    }

    fun initRecyclerListView() {
        // init
        if (recyclerListView == null) {
            recyclerListView = RecyclerView(context).apply {
                layoutManager = this@RecyclerListViewController.layoutManager
                adapter = this@RecyclerListViewController.adapter
                clipToPadding = false
            }
        }
    }

}
