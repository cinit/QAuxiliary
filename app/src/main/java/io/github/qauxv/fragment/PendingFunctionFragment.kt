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

class PendingFunctionFragment : BaseRootRecyclerFragment() {

    override fun getTitle() = "开发中的功能"

    private var mDslListViewController: RecyclerListViewController? = null

    override fun doOnCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
        rootRecyclerView = recyclerView
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
        if (item.switch) {
            return TextSwitchItem(item.title, item.desc, dummySwitch)
        } else {
            return TextListItem(item.title, item.desc, item.value)
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
            Item("无视QQ电话与语音冲突", "允许在QQ电话时播放语音和短视频", null, true),
            Item("QQ电话关麦时解除占用", "再开麦时如麦被其他程序占用可能崩溃", null, true),
            Item("QQ视频通话旋转锁定", "可在通话界面设置旋转方向", null, true),
            Item("屏蔽回执消息的通知", null, null, true),
            Item("隐藏联系人", "和自带的\"隐藏会话\"有所不同", "0人"),
            Item("自定义本地头像", "仅本机生效", "禁用"),
            Item("QQ电话睡眠模式", "仅保持连麦, 暂停消息接收, 减少电量消耗", null, true),
            Item("禁用QQ公交卡", "如果QQ在后台会干扰NFC的话", null, true),
            Item("AddFriendReq.sourceID", "自定义加好友来源", "[不改动]"),
            Item("DelFriendReq.delType", "只能为1或2", "[不改动]"),
    )

    private data class Item(val title: String, val desc: String?, val value: String? = null, val switch: Boolean = false)
}
