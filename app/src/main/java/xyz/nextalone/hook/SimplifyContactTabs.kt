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
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.base.MultiItemDelayableHook
import xyz.nextalone.util.*

@FunctionHookEntry
@UiItemAgentEntry
object SimplifyContactTabs : MultiItemDelayableHook("na_simplify_contact_tabs_multi") {

    override val preferenceTitle = "精简联系人页面"
    override val allItems = setOf("好友", "分组", "群聊", "设备", "通讯录", "订阅号")
    override val defaultItems = setOf<String>()

    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.MAIN_UI_CONTACT

    override fun initOnce() = throwOrTrue {
        val contactsTabs = if(requireMinQQVersion(QQVersion.QQ_8_9_2)) "b" else "ContactsTabs"
        val tabinfo = if (requireMinQQVersion(QQVersion.QQ_8_9_2)) "f" else "TabInfo"
        "Lcom.tencent.mobileqq.activity.contacts.base.tabs.$contactsTabs;->a()V".method.hookAfter(
            this
        ) {
            val list = it.thisObject.get(ArrayList::class.java) as ArrayList<Any>
            val tabList = list.toMutableList()
            list.clear()
            val stringList: ArrayList<String> = arrayListOf()
            val intList: ArrayList<Int> = arrayListOf()
            val cls = "com.tencent.mobileqq.activity.contacts.base.tabs.$tabinfo".clazz
            tabList.forEach { obj ->
                val str = obj.get(String::class.java) as String
                if (str == "好友" && !activeItems.contains(str)) {
                    val id = obj.get(Int::class.java) as Int
                    list.add(obj)
                    stringList.add(str)
                    intList.add(id)
                } else if (!activeItems.contains(str)) {
                    val id = obj.get(Int::class.java) as Int
                    val instance = cls.instance(items.indexOf(str), id, str)
                    list.add(instance)
                    stringList.add(str)
                    intList.add(id)
                }
            }
            var arrayListName = "a"
            var intListName = "a"
            it.thisObject.javaClass.declaredFields.forEach { field ->
                if (field.type == Array<String>::class.java) {
                    arrayListName = field.name
                }
                if (field.type == IntArray::class.java) {
                    intListName = field.name
                }
            }
            it.thisObject.set(arrayListName, Array<String>::class.java, stringList.toTypedArray())
            it.thisObject.set(intListName, IntArray::class.java, intList.toIntArray())
        }
    }

    override val isAvailable: Boolean get() = requireMinQQVersion(QQVersion.QQ_8_5_5)
}
