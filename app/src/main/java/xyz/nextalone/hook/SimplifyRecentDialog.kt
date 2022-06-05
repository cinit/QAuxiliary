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
object SimplifyRecentDialog : MultiItemDelayableHook("na_simplify_recent_dialog_multi") {

    override val preferenceTitle = "精简主页对话框"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.MAIN_UI_TITLE

    override val allItems = setOf("创建群聊", "加好友/群", "匹配聊天", "一起派对", "扫一扫", "面对面快传", "收付款")
    override val defaultItems = setOf<String>()

    override fun initOnce() = throwOrTrue {
        val methodName: String
        val titleName: String
        if (requireMinQQVersion(QQVersion.QQ_8_6_0)) {
            methodName = "conversationPlusBuild"
            titleName = "title"
        } else {
            methodName = "b"
            titleName = "a"
        }
        "com/tencent/widget/PopupMenuDialog".clazz?.method(methodName,
            4,
            "com.tencent.widget.PopupMenuDialog".clazz)?.hookBefore(this) {
            val list = (it.args[1] as List<*>).toMutableList()
            val iterator = list.iterator()
            while (iterator.hasNext()) {
                val string = iterator.next().get(titleName, String::class.java)
                if (activeItems.contains(string)) {
                    iterator.remove()
                }
            }
            it.args[1] = list.toList()
        }
    }

    override val isAvailable: Boolean get() = requireMinQQVersion(QQVersion.QQ_8_3_9)
}
