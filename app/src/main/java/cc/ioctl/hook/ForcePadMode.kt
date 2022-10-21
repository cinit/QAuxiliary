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

package cc.ioctl.hook

import cc.ioctl.util.Reflex
import cc.ioctl.util.hookBeforeIfEnabled
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.util.method
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object ForcePadMode : CommonSwitchFunctionHook() {

    override val name = "强制平板模式"
    override val description = "仅支持 QQ 8.9.15, 未经测试, 谨慎使用"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY
    override val isApplicationRestartRequired = true

    override val isAvailable: Boolean
        get() = requireMinQQVersion(QQVersion.QQ_8_9_15)

    override fun initOnce() = throwOrTrue {
        check(isAvailable) { "ForcePadMode is not available" }
        val k = Initiator.loadClass("com/tencent/common/config/DeviceType")
        val deviceTypePad = Reflex.getStaticObject(k, "TABLET")
        val initDeviceType = "Lcom/tencent/common/config/e;->a(Landroid/content/Context;)Lcom/tencent/common/config/DeviceType;".method
        hookBeforeIfEnabled(initDeviceType) {
            it.result = deviceTypePad
        }
    }

}
