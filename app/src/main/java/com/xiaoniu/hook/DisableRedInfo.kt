/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
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

package com.xiaoniu.hook

import cc.hicore.QApp.QAppUtils
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.util.Log
import xyz.nextalone.base.MultiItemDelayableHook
import xyz.nextalone.util.clazz

@FunctionHookEntry
@UiItemAgentEntry
object DisableRedInfo : MultiItemDelayableHook("disable_red_info_multi") {
    override val preferenceTitle = "禁用红字提示"
    override val allItems = setOf(
        "有新文件",
        "QQ红包",
        "有人@我",
        "@全体成员",
        "新公告",
        "待确认的新公告",
    )
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.MAIN_UI_MSG_LIST

    override val isAvailable = QAppUtils.isQQnt()

    override val dialogDesc = "禁用"
    override fun initOnce(): Boolean {
        "com.tencent.qqnt.chats.biz.summary.highlight.AtTypeHelper".clazz!!.declaredMethods
            .filter { it.returnType == String::class.java && it.parameterTypes.size == 1 }
            .forEach {
                it.hookAfter { param ->
                    Log.d(param.result.toString())
                    activeItems.forEach {
                        if (param.result.toString().contains(it)) {
                            param.result = ""
                            return@hookAfter
                        }
                    }
                }
            }
        // 需要hook的方法只有一个，但是有多个签名相似的，多hook了点问题不大
        return true
    }
}
