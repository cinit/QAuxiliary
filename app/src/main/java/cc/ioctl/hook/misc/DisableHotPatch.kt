/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2024 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is an opensource software: you can redistribute it
 * and/or modify it under the terms of the General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the General Public License for more details.
 *
 * You should have received a copy of the General Public License
 * along with this software.
 * If not, see
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package cc.ioctl.hook.misc

import cc.ioctl.util.hookBeforeIfEnabled
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.hostInfo
import java.io.File

@FunctionHookEntry
@UiItemAgentEntry
object DisableHotPatch : CommonSwitchFunctionHook(
    targetProc = SyncUtils.PROC_MAIN or SyncUtils.PROC_MSF or SyncUtils.PROC_TOOL
) {

    override val name by lazy { "禁用 " + hostInfo.hostName + " 热补丁" }
    override val description by lazy { "禁用 " + hostInfo.hostName + " 云控热补丁/热更新" }
    override val extraSearchKeywords = arrayOf("热补丁", "热更新", "云控", "热修复")
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MISC_CATEGORY
    private val configFile: File by lazy { File(hostInfo.application.filesDir, "qn_disable_hot_patch") }

    override var isEnabled: Boolean
        get() {
            return configFile.exists()
        }
        set(value) {
            val current = isEnabled
            if (value == current) {
                return
            }
            if (value) {
                configFile.createNewFile()
            } else {
                configFile.delete()
            }
        }

    override fun initOnce(): Boolean {
        // 9.0.35+
        Initiator.load("com.tencent.rfix.lib.download.PatchDownloadTask")?.let { kPatchDownloadTask ->
            val run = kPatchDownloadTask.getDeclaredMethod("run")
            hookBeforeIfEnabled(run) {
                it.result = null
            }
        }
        Initiator.load("com.tencent.rfix.lib.engine.PatchEngineBase")?.let { kPatchEngineBase ->
            val kPatchConfig = Initiator.loadClass("com.tencent.rfix.lib.config.PatchConfig")
            val onPatchReceived = kPatchEngineBase.declaredMethods.single {
                val argt = it.parameterTypes
                it.returnType == Void.TYPE && argt.size == 2 && argt[0] == String::class.java && argt[1] == kPatchConfig
            }
            hookBeforeIfEnabled(onPatchReceived) {
                it.result = null
            }
        }
        return true
    }

}
