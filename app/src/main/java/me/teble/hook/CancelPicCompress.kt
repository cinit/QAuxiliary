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

package me.teble.hook

import android.content.Context
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReplace
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.ForwardSendPicUtil

@FunctionHookEntry
@UiItemAgentEntry
object CancelPicCompress : CommonSwitchFunctionHook(
    SyncUtils.PROC_MAIN or SyncUtils.PROC_PEAK,
    arrayOf(ForwardSendPicUtil)
) {

    override val name = "分享图片不压缩"

    override val description: CharSequence = "通过QQ分享图片时以原图发送"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY

    override val isApplicationRestartRequired = true

    override fun initOnce(): Boolean {
        val util = DexKit.loadClassFromCache(ForwardSendPicUtil)!!
        findMethod(util) {
            (parameterTypes[0] == Context::class.java) and (parameterTypes[1] == String::class.java)
        }.hookReplace { param ->
            param.args[1] as String?
        }
        return true
    }
}
