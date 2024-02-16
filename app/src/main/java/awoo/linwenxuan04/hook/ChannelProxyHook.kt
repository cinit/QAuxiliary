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

import com.github.kyuubiran.ezxhelper.utils.loadClass
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Log
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.util.method
import xyz.nextalone.util.replace

@FunctionHookEntry
@UiItemAgentEntry
object ChannelProxyHook : CommonSwitchFunctionHook() {

    override val name = "环境检测包(trpc.o3.*)拦截"
    override val description = "拦截trpc.o3*包体以防止QQ上报环境检测(其中包含root/magisk/xposed安装情况)，防止新号封号，不建议打开。"
    override val targetProcesses: Int = SyncUtils.PROC_MSF

    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_83)
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY

    override val isApplicationRestartRequired = true

    override fun initOnce(): Boolean {
        val clazz = loadClass("com.tencent.mobileqq.channel.ChannelProxy")
        clazz.method("sendMessage", Void::class.java, String::class.java, ByteArray::class.java, Long::class.java)!!
            .replace(this) {
                Log.i("已拦截包: ${it.args[0]}，包ID: ${it.args[2]}")
            }
        return true
    }
}