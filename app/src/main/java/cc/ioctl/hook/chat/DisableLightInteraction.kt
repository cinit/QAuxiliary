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

package cc.ioctl.hook.chat

import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import com.github.kyuubiran.ezxhelper.utils.paramCount
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.DisableLightInteractionMethod
import io.github.qauxv.util.requireMinQQVersion

@FunctionHookEntry
@UiItemAgentEntry
object DisableLightInteraction : CommonSwitchFunctionHook(
    targets = arrayOf(DisableLightInteractionMethod)
) {

    override val name = "禁用轻互动"
    override val description = "隐藏聊天列表有时出现的表情 (早上好, 戳一戳, 晚安) 点一下发一条消息然后消失"
    override val extraSearchKeywords: Array<String> = arrayOf("开始全新的一天，早上好啊", "戳一戳，看看他在干嘛", "夜深了，和他道一声晚安吧")
    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Simplify.MAIN_UI_MSG_LIST
    override val isAvailable: Boolean get() = requireMinQQVersion(QQVersion.QQ_8_9_78)

    override fun initOnce(): Boolean {
        val kLIAConfigManager = Initiator.loadClass("com.tencent.qqnt.biz.lightbusiness.lightinteraction.LIAConfigManager")
        if (requireMinQQVersion(QQVersion.QQ_9_0_8)) {
            kLIAConfigManager.declaredMethods.single {
                it.paramCount == 1 && it.returnType == java.util.List::class.java
            }.hookReturnConstant(emptyList<Any>())
        } else {
            DexKit.requireMethodFromCache(DisableLightInteractionMethod).hookReturnConstant(null)
        }
        return true
    }

}