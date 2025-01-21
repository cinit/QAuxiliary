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

import cc.hicore.QApp.QAppUtils
import com.github.kyuubiran.ezxhelper.utils.getFieldByType
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.isAbstract
import com.github.kyuubiran.ezxhelper.utils.paramCount
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.TIMVersion
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.requireMinTimVersion
import xyz.nextalone.base.MultiItemDelayableHook
import xyz.nextalone.util.clazz
import xyz.nextalone.util.method
import xyz.nextalone.util.throwOrTrue
import java.lang.reflect.Method

@FunctionHookEntry
@UiItemAgentEntry
object SimplifyChatLongItem : MultiItemDelayableHook("na_simplify_chat_long_item_multi") {

    override val preferenceTitle = "精简聊天气泡长按菜单"
    override val allItems = setOf(
        "复制",
        "转发",
        "收藏",
        "回复",
        "引用",
        "多选",
        "撤回",
        "删除",
        "一起写",
        "设为精华",
        "待办",
        "私聊",
        "截图",
        "存表情",
        "相关表情",
        "复制链接",
        "存微云",
        "发给电脑",
        "静音播放",
        "复制文字",
        "转发文字",
        "免提播放",
        "2X",
        "保存",
        "群待办",
        "提醒",
        "装扮",
    )
    override val defaultItems = setOf<String>()

    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.UI_CHAT_MSG

    private var getName: Method? = null

    override fun initOnce() = throwOrTrue {
        if (QAppUtils.isQQnt() || requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA)) {
            mutableListOf("com/tencent/qqnt/aio/menu/ui/QQCustomMenuNoIconLayout").apply {
                if (requireMinQQVersion(QQVersion.QQ_9_0_0) || requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA)) {
                    add(0, "com/tencent/qqnt/aio/menu/ui/QQCustomMenuExpandableLayout")
                }
            }.firstNotNullOf { it.clazz }
                .method("setMenu")!!
                .hookBefore {
                    val list = it.args[0].javaClass.getFieldByType(List::class.java).get(it.args[0]) as MutableList<*>
                    if (list.isEmpty()) return@hookBefore
                    if (getName == null) {
                        getName = list[0]?.javaClass!!.superclass!!.declaredMethods.last { m ->
                            m.returnType == String::class.java && m.isAbstract
                        }!!
                    }
                    list.forEach { item ->
                        val str = getName!!.invoke(item)!! as String
                        if (activeItems.contains(str))
                            list.remove(item)
                    }
                }
        } else {
            "com.tencent.mobileqq.utils.dialogutils.QQCustomMenuImageLayout".clazz?.declaredMethods.run {
                this?.forEach { method ->
                    if (method.name == "setMenu") {
                        val customMenu = method.parameterTypes[0].name
                        runCatching {
                            customMenu.clazz?.method {
                                it.paramCount == 1 && it.parameterTypes[0] != String::class.java && it.returnType == Void.TYPE
                            }?.hookBefore {
                                val str = it.args[0].getFieldByType(String::class.java).get(it.args[0]) as String
                                if (activeItems.contains(str))
                                    it.result = null
                            }
                        }
                        return@forEach
                    }
                }
            }
        }
    }
}
