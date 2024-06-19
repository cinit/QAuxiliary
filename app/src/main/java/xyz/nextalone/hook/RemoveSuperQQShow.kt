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

import android.view.View
import cc.ioctl.util.msg.MessageManager
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.hookReturnConstant
import com.github.kyuubiran.ezxhelper.utils.paramCount
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.util.clazz
import xyz.nextalone.util.method
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object RemoveSuperQQShow : CommonSwitchFunctionHook() {

    override val name: String = "屏蔽消息界面标题栏超级QQ秀图标"

    override fun initOnce() = throwOrTrue {
        if (requireMinQQVersion(QQVersion.QQ_9_0_20)) {
            // ConversationTitleBtnConfig 中判断配置是否有效的方法
            "Lcom/tencent/mobileqq/util/conversationtitlebutton/a;".clazz!!.method {
                it.paramCount == 0 && it.returnType == Boolean::class.java
            }!!.hookReturnConstant(false)
        } else if (requireMinQQVersion(QQVersion.QQ_8_9_10)) {
            findMethod(Initiator._ZPlanBadgeManagerImpl()) {
                name == "onCreateView" && returnType == Void.TYPE && parameterTypes.contentEquals(arrayOf(View::class.java, MessageManager.booleanType))
            }.hookBefore {
                if (!isEnabled) return@hookBefore; it.result = null
            }
        } else if (requireMinQQVersion(QQVersion.QQ_8_9_3)) {
            findMethod(Initiator._ZPlanBadgeManagerImpl()) {
                name == "onCreateView" && returnType == Void.TYPE && parameterTypes.contentEquals(arrayOf(View::class.java))
            }.hookBefore {
                if (!isEnabled) return@hookBefore; it.result = null
            }
        } else {
            findMethod(Initiator._ConversationTitleBtnCtrl()) {
                (name == "b" || name == "D") && returnType == Void.TYPE && parameterTypes.contentEquals(arrayOf(View::class.java))
            }.hookBefore {
                if (!isEnabled) return@hookBefore; it.result = null
            }
        }
    }

    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Simplify.MAIN_UI_TITLE

    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_8_80)
}
