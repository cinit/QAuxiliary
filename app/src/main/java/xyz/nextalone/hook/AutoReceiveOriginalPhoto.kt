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

import io.github.qauxv.SyncUtils
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.DexKit
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.hostInfo
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.util.invoke
import xyz.nextalone.util.method
import xyz.nextalone.util.replace
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object AutoReceiveOriginalPhoto : CommonSwitchFunctionHook(
    SyncUtils.PROC_PEAK,
    intArrayOf(DexKit.C_AIOPictureView)
) {

    override val name: String = "聊天自动接收原图"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY

    override fun initOnce() = throwOrTrue {
        val method: String = when {
            requireMinQQVersion(QQVersion.QQ_8_6_0) -> {
                "j"
            }
            requireMinQQVersion(QQVersion.QQ_8_5_0) -> {
                "h"
            }
            else -> {
                "I"
            }
        }
        val clz = DexKit.doFindClass(DexKit.C_AIOPictureView)
        val m: String = if (hostInfo.versionCode >= QQVersion.QQ_8_6_0) {
            "g"
        } else {
            "f"
        }
        "L${clz?.name};->$m(Z)V".method.replace(this) {
            if (it.args[0] as Boolean) {
                it.thisObject.invoke(method)
            }
        }
    }

    override val isAvailable: Boolean get() = requireMinQQVersion(QQVersion.QQ_8_3_5)
}
