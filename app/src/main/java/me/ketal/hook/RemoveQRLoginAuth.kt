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

package me.ketal.hook

import com.github.kyuubiran.ezxhelper.utils.findMethod
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.QQVersion.QQ_8_9_0
import io.github.qauxv.util.QQVersion.QQ_8_9_70
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.util.hookBefore
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object RemoveQRLoginAuth : CommonSwitchFunctionHook() {

    override val name = "去除相册扫码登录检验"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MISC_CATEGORY
    override val isAvailable: Boolean get() = requireMinQQVersion(QQVersion.QQ_8_5_0)

    override fun initOnce() = throwOrTrue {
        val clazz = if (requireMinQQVersion(QQ_8_9_70)) {
            "com/tencent/open/agent/QrAgentLoginManager"
        } else if (requireMinQQVersion(QQ_8_9_0)) {
            "com/tencent/open/agent/QrAgentLoginManager\$a"
        } else {
            "com/tencent/open/agent/QrAgentLoginManager\$2"
        }
        Initiator.loadClass(clazz).declaredMethods
            .findMethod {
                returnType == Void.TYPE && parameterTypes.isNotEmpty() && parameterTypes[0] == Boolean::class.java
            }.hookBefore(this) {
                it.args[0] = false
            }
    }
}
