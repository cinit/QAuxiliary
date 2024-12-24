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

package awoo.linwenxuan04.hook

import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Log
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.TIMVersion
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.requireMinTimVersion
import xyz.nextalone.util.method
import xyz.nextalone.util.replace

@FunctionHookEntry
@UiItemAgentEntry
object ChannelProxyHook : CommonSwitchFunctionHook() {

    override val name = "环境检测包(trpc.o3.*)拦截"
    override val description =
        "拦截 trpc.o3.* 包体以防止 QQ 上报环境检测(其中包含 root/Magisk/Xposed 安装情况)，理论上降低新号封号概率，未经测试，如无特殊情况不建议打开。"
    override val targetProcesses: Int = SyncUtils.PROC_MSF

    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_83) || requireMinTimVersion(TIMVersion.TIM_4_0_95)
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY

    override val isApplicationRestartRequired = true

    override fun initOnce(): Boolean {
        val clazz = Initiator.loadClass("com.tencent.mobileqq.channel.ChannelProxyExt")
        clazz.method("sendMessage", Void.TYPE, String::class.java, ByteArray::class.java, Long::class.java)!!
            .replace(this) {
                Log.i("已拦截包: ${it.args[0]}，包ID: ${it.args[2]}")
            }
        return true
    }
}
