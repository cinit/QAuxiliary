/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2025 QAuxiliary developers
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

package io.github.horange321

import com.github.kyuubiran.ezxhelper.utils.field
import com.github.kyuubiran.ezxhelper.utils.findField
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify
import io.github.qauxv.util.Initiator.loadClass
import xyz.nextalone.base.MultiItemDelayableHook
import xyz.nextalone.util.method


@FunctionHookEntry
@UiItemAgentEntry
object SimplifyQQSettingMe2 : MultiItemDelayableHook(
    "SimplifyQQSettingMe2"
) {
    val map = hashMapOf(
        "d_album" to "相册",
        "d_favorite" to "收藏",
        "d_document" to "文件",
        "d_qqwallet" to "钱包",
        "d_vip_identity" to "会员中心",
        "d_decoration" to "个性装扮",
        "d_vip_card" to "免流量"
    )
    override val preferenceTitle = "新版侧滑栏精简"
    override val allItems = map.values.toSet()
    override val uiItemLocation = Simplify.SLIDING_UI


    private fun needRemove(bean: Any): Boolean {
        bean::class.java.declaredFields.forEach { f ->
            if (f.type == java.lang.String::class.java) {
                f.isAccessible = true
                if (map[f.get(bean)!! as String] in activeItems) {
                    return true
                }
            }
        }
        return false
    }


    override fun initOnce(): Boolean {
        val clazz = loadClass("com.tencent.mobileqq.parts.QQSettingMeMenuPanelPartV3")
        clazz.method("onInitView")!!.hookAfter {
            val obj = it.thisObject
//ArrayList<com.tencent.mobileqq.adapter.u> com.tencent.mobileqq.parts.QQSettingMeMenuPanelPartV3$bizData
            val bizDataList = clazz.findField {
                type == java.util.ArrayList::class.java
            }.run {
                isAccessible = true
                get(obj)!! as ArrayList<Any>
            }

            // 删掉要去掉的东西
            val i = bizDataList.iterator()
            while (i.hasNext()) {
                val v = i.next()
//com.tencent.mobileqq.activity.qqsettingme.config.QQSettingMeBizBean com.tencent.mobileqq.adapter.u:bean
                // 是唯一的 field，可以直接 hardcode
                val bean = v.field("a").run {
                    isAccessible = true
                    get(v)
                }
                // 有的会有 null 出现。需要特殊处理
                if (bean == null) continue
                if (needRemove(bean))
                    i.remove()
            }

//com.tencent.mobileqq.adapter.t com.tencent.mobileqq.parts.QQSettingMeMenuPanelPartV3$listItemAdapter
            val listItemAdapter = clazz.findField {
                type.name.contains("adapter")
            }.run {
                isAccessible = true
                get(obj)!!
            }

            // set adapter items
            loadClass("com.tencent.biz.richframework.part.adapter.AsyncListDifferDelegationAdapter")
                .method("setItems")!!.run {
                    isAccessible = true
                    invoke(listItemAdapter, bizDataList)
                }
        }
        return true
    }
}