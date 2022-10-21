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

import android.annotation.SuppressLint
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object ForcePadMode : CommonSwitchFunctionHook(targetProc = SyncUtils.PROC_ANY) {

    override val name = "强制平板模式"
    override val description = "仅支持 QQ 8.9.15, 未经测试, 谨慎使用"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY
    override val isApplicationRestartRequired = true

    override val isAvailable: Boolean
        get() = requireMinQQVersion(QQVersion.QQ_8_9_15)

    override fun initOnce() = throwOrTrue {
        check(isAvailable) { "ForcePadMode is not available" }
        @SuppressLint("PrivateApi")
        val cls = Class.forName("android.os.SystemProperties")
        cls.getMethod("get", String::class.java)
            .hookAfter(this) {
                if (it.args[0] == "ro.build.characteristics") {
                    it.result = "tablet"
                }
            }
    }

}
