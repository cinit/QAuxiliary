/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2026 QAuxiliary developers
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

package top.suzhelan.hook.item

import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.xpcompat.XposedHelpers
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.throwOrTrue

// 排序逻辑出现异常时不干涉原始顺序 返回默认权重
@FunctionHookEntry
@UiItemAgentEntry
object SortAtList : CommonSwitchFunctionHook() {

    override val name = "@列表重新排序"
    override val description = "将@列表中群主和管理员排在最前面"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY
    override val extraSearchKeywords: Array<String> = arrayOf("艾特", "at")
    override val isAvailable: Boolean
        get() = requireMinQQVersion(QQVersion.QQ_8_9_68)

    override fun initOnce() = throwOrTrue {
        val clazz = Initiator.load("com.tencent.mobileqq.aio.input.at.common.SubmitListEvent")
            ?: return@throwOrTrue

        for (method in clazz.declaredMethods) {
            if (method.name == "getItemList"
                && method.parameterTypes.isEmpty()
                && method.returnType == List::class.java
            ) {
                method.hookAfter(this) { param ->
                    val originalList = param.result as? List<Any> ?: return@hookAfter
                    val sortedList = originalList.sortedBy { item -> rank(item) }
                    param.result = sortedList
                }
                break
            }
        }
    }

    private fun rank(item: Any?): Int {
        if (item == null) return 99
        try {
            val memberInfoField = item.javaClass.declaredFields.find { field ->
                field.type.name.contains("MemberInfo")
            } ?: return 0

            memberInfoField.isAccessible = true
            val memberInfo = memberInfoField.get(item) ?: return 0

            val isRobot = XposedHelpers.getBooleanField(memberInfo, "isRobot")
            if (isRobot) return 3

            val role = XposedHelpers.getObjectField(memberInfo, "role")?.toString() ?: ""
            return when {
                role.contains("OWNER") -> 1
                role.contains("ADMIN") -> 2
                role.contains("MEMBER") -> 4
                else -> 5
            }
        } catch (e: Exception) {
            return 0
        }
    }
}
