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
package xyz.nextalone.hook

import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.TIMVersion
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.requireMinTimVersion
import xyz.nextalone.base.MultiItemDelayableHook
import xyz.nextalone.util.*

@FunctionHookEntry
@UiItemAgentEntry
object SimplifyContactTabs : MultiItemDelayableHook("na_simplify_contact_tabs_multi") {

    override val preferenceTitle = "精简联系人页面"
    override val allItems = setOf("好友", "分组", "群聊", "设备", "通讯录", "订阅号", "推荐", "频道", "机器人")
    override val defaultItems = setOf<String>()
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.MAIN_UI_CONTACT
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_5_5) || requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA)

    override fun initOnce() = throwOrTrue {
        val nameContactsTabs = if (requireMinQQVersion(QQVersion.QQ_8_9_2) || requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA)) "b" else "ContactsTabs"
        "Lcom.tencent.mobileqq.activity.contacts.base.tabs.$nameContactsTabs;->a()V".method.hookAfter(this) {

            val listTabId: ArrayList<Int> = arrayListOf()
            val listTabText: ArrayList<String> = arrayListOf()
            val listTabInfoTmp: ArrayList<Any> = arrayListOf()
            val listTabInfo = it.thisObject.get(ArrayList::class.java) as ArrayList<Any>
            listTabInfo.forEach { tabInfo ->
                val id = tabInfo.get(Int::class.java) as Int
                val tabText = tabInfo.get(String::class.java) as String
                if (!activeItems.contains(tabText)) {
                    listTabId.add(id)
                    listTabText.add(tabText)
                    listTabInfoTmp.add(tabInfo)
                }
            }
            listTabInfo.clear()
            listTabInfo.addAll(listTabInfoTmp)

            var nameStrArray = "a"
            var nameIntArray = "b"
            it.thisObject.javaClass.declaredFields.forEach { field ->
                when (field.type) {
                    Array<String>::class.java -> nameStrArray = field.name
                    IntArray::class.java -> nameIntArray = field.name
                }
            }
            it.thisObject.set(nameStrArray, Array<String>::class.java, listTabText.toTypedArray())
            it.thisObject.set(nameIntArray, IntArray::class.java, listTabId.toIntArray())
        }
    }
}