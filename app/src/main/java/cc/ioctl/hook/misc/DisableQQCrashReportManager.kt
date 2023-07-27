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

package cc.ioctl.hook.misc

import cc.ioctl.util.HookUtils
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.isTim
import xyz.nextalone.util.isPublic
import xyz.nextalone.util.isStatic

@FunctionHookEntry
@UiItemAgentEntry
object DisableQQCrashReportManager : CommonSwitchFunctionHook(targetProc = SyncUtils.PROC_ANY) {

    override val name = "禁用 QQCrashReportManager"
    override val description = "仅限调试，无实际用途"
    override val uiItemLocation = FunctionEntryRouter.Locations.DebugCategory.DEBUG_CATEGORY
    override val isApplicationRestartRequired = true
    override val isAvailable = !isTim()

    override fun initOnce(): Boolean {
        val kQQCrashReportManager = Initiator.loadClass("com.tencent.qqperf.monitor.crash.QQCrashReportManager")
        val initCrashReport = kQQCrashReportManager.declaredMethods.single {
            it.isPublic && it.returnType == Void.TYPE && !it.isStatic && it.parameterTypes.size == 2
        }
        HookUtils.hookBeforeAlways(this, initCrashReport) {
            it.result = null
        }
        return true
    }

}
