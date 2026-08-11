/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2026 QAuxiliary developers
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

package me.hd.hook

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayout
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.ui.CommonContextWrapper.createAppCompatContext
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.dexkit.Hd_ChatsListAdapter_CLASS
import io.github.qauxv.util.dexkit.Hd_ChatsListVB_OnCreateView_Method
import io.github.qauxv.util.requireMinQQVersion
import me.hd.util.hookAfterIfEnabled
import me.hd.util.hookBeforeIfEnabled
import me.hd.util.name
import me.hd.util.parameters
import me.hd.util.singleMethod
import me.hd.util.toHostClass
import me.hd.util.toHostMethod
import xyz.nextalone.util.get

@FunctionHookEntry
@UiItemAgentEntry
object ChatGroupTab : CommonSwitchFunctionHook(
    targets = arrayOf(Hd_ChatsListAdapter_CLASS, Hd_ChatsListVB_OnCreateView_Method)
) {
    override val name = "聊天分组标签"
    override val description = "添加分组顶部标签栏，顶部栏无法固定"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.GROUP_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_9_0_0)
    override val isApplicationRestartRequired = true

    private enum class Type(
        val title: String,
        val condition: (chatItem: String) -> Boolean = { true },
    ) {
        ALL("全部"),
        UNREAD("未读", { !it.contains("unreadCount=UnreadInfo(type=1, count=0).count") }),
        STRANGER("临时会话", { it.contains("contactType=100,") }),
        FRIEND("好友", { it.contains("contactType=1,") }),
        GROUP("群聊", { it.contains("contactType=2,") }),
        GUILD("频道", { it.contains("contactType=16,") });

        inline fun <reified T> filter(dataList: List<T>): List<T> {
            return dataList.filter { condition("$it") }
        }
    }

    private var selectedType = Type.ALL
    private val cacheListMap = mutableMapOf<Any, List<*>>()

    private fun createTabsView(context: Context, adapter: Any): ViewGroup {
        return TabLayout(createAppCompatContext(context)).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            tabMode = TabLayout.MODE_AUTO
            tabGravity = TabLayout.GRAVITY_CENTER
            tabRippleColor = ColorStateList.valueOf(Color.TRANSPARENT)
            isInlineLabel = true
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabReselected(tab: TabLayout.Tab) = Unit
                override fun onTabUnselected(tab: TabLayout.Tab) = Unit
                override fun onTabSelected(tab: TabLayout.Tab) {
                    selectedType = tab.tag as Type
                    adapter.singleMethod {
                        name("submitList") &&
                            parameters(List::class.java)
                    }.invoke(adapter, cacheListMap[adapter] ?: emptyList<Any>())
                }
            })
            Type.entries.forEach { type ->
                addTab(newTab().setTag(type).setText(type.title))
            }
        }
    }

    override fun initOnce(): Boolean {
        val adapterClass = Hd_ChatsListAdapter_CLASS.toHostClass()
        adapterClass.singleMethod {
            parameters(List::class.java, Boolean::class.java)
        }.hookBeforeIfEnabled(this) { param ->
            val dataList = param.args[0] as List<*>
            cacheListMap[param.thisObject] = dataList
            param.args[0] = selectedType.filter(dataList)
        }
        Hd_ChatsListVB_OnCreateView_Method.toHostMethod()
            .hookAfterIfEnabled(this) { param ->
                val vb = param.thisObject
                val context = vb.get(Context::class.java) as Context
                val adapter = vb.get(adapterClass) as Any
                val tabsView = createTabsView(context, adapter)
                adapter.singleMethod {
                    when {
                        requireMinQQVersion(QQVersion.QQ_9_1_0) -> parameters(Int::class.java, View::class.java, Boolean::class.java)
                        else -> parameters(Int::class.java, View::class.java)
                    }
                }.apply {
                    when {
                        requireMinQQVersion(QQVersion.QQ_9_1_0) -> invoke(adapter, -4, tabsView, true)
                        else -> invoke(adapter, -4, tabsView)
                    }
                }
            }
        return true
    }
}
