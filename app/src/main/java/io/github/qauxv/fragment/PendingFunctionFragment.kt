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
import android.widget.FrameLayout
import androidx.lifecycle.lifecycleScope
import cc.ioctl.util.LayoutHelper
import cc.ioctl.util.ui.dsl.RecyclerListViewController
import io.github.qauxv.R
import io.github.qauxv.base.ISwitchCellAgent
import io.github.qauxv.dsl.item.DescriptionItem
import io.github.qauxv.dsl.item.DslTMsgListItemInflatable
import io.github.qauxv.dsl.item.TextListItem
import io.github.qauxv.dsl.item.TextSwitchItem
import io.github.qauxv.util.Toasts

class PendingFunctionFragment : BaseRootLayoutFragment() {

    private var mDslListViewController: RecyclerListViewController? = null

    override fun doOnCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        title = "开发中的功能"
        val context = inflater.context
        mDslListViewController = RecyclerListViewController(context, lifecycleScope)
        mDslListViewController!!.items = hierarchy
        mDslListViewController!!.initAdapter()
        mDslListViewController!!.initRecyclerListView()
        val recyclerView = mDslListViewController!!.recyclerListView!!
        recyclerView.apply {
            id = R.id.fragmentMainRecyclerView
        }
        val rootView: FrameLayout = FrameLayout(context).apply {
            addView(mDslListViewController!!.recyclerListView, FrameLayout.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))
        }
        rootLayoutView = recyclerView
        return rootView
    }

    private val hierarchy: Array<DslTMsgListItemInflatable> by lazy {
        val header = DescriptionItem("牙膏要一点一点挤, 显卡要一刀一刀切, PPT 要一张一张放, 代码要一行一行写, 单个功能预计自出现在 commit 之日起, 三年内开发完毕")
        val list: MutableList<DslTMsgListItemInflatable> = ArrayList()
        pendingFunctionList.forEach {
            list.add(convertItem(it))
        }
        list.add(header)
        list.toTypedArray()
    }

    private fun convertItem(item: Item): DslTMsgListItemInflatable {
        return if (item.switch) {
            TextSwitchItem(item.title, item.desc, dummySwitch)
        } else {
            TextListItem(item.title, item.desc, item.value)
        }
    }

    private val dummySwitch: ISwitchCellAgent = object : ISwitchCellAgent {
        override val isCheckable = false
        override var isChecked: Boolean
            get() = false
            set(value) {
                if (value) {
                    Toasts.show(context, "暂未开放", Toasts.LENGTH_SHORT)
                }
            }
    }

    private val pendingFunctionList: Array<Item> = arrayOf(
        Item("屏蔽卡片消息 IP 探针", "可能导致部分卡片消息无法正常显示", null, true),
        Item("QQ电话关麦时解除占用", "再开麦时如麦被其他程序占用可能崩溃", null, true),
        Item("QQ视频通话旋转锁定", "可在通话界面设置旋转方向", null, true),
        Item("阻挡QQ获取位置信息", "重定向到(0,0)或PEK中心", null, true),
        Item("阻挡QQ检测权限未授予", "如 定位 联系人等隐私权限 ", null, true),
    )

    private data class Item(val title: String, val desc: String?, val value: String? = null, val switch: Boolean = false)
}
