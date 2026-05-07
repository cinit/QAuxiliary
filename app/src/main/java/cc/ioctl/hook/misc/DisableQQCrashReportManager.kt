/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the qwq233 Universal License
 * as published on https://github.com/qwq233/license; either
 * version 2 of the License, or any later version and our EULA as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the qwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
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
object DisableQQCrashReportManager : CommonSwitchFunctionHook(defaultEnabled = true, targetProc = SyncUtils.PROC_ANY) {
    // 因为被点名批评了，所以默认开启，以避免潜在的不必要的麻烦
    override val name = "禁用崩溃日志上报"
    override val description = "禁用 QQCrashReportManager 的崩溃日志上报功能，防止上报可能带有模块信息的崩溃日志"

    override val uiItemLocation = FunctionEntryRouter.Locations.DebugCategory.DEBUG_CATEGORY
    override val isApplicationRestartRequired = true
    override val isAvailable = true

    override fun initOnce(): Boolean {
        // com/tencent/qqperf/monitor/crash/QQCrashReportManager is added in QQ 8.?.?
        // and the class name is not obfuscated
        val kQQCrashReportManager = Initiator.load("com.tencent.qqperf.monitor.crash.QQCrashReportManager")
        if (kQQCrashReportManager != null) {
            val initCrashReport = kQQCrashReportManager.declaredMethods.single {
                it.isPublic && it.returnType == Void.TYPE && !it.isStatic && it.parameterTypes.size == 2
            }
            HookUtils.hookBeforeAlways(this, initCrashReport) {
                it.result = null
            }
        }
        val kStatisticCollector = Initiator.load("com.tencent.mobileqq.statistics.StatisticCollector")
        if (kStatisticCollector != null) {
            // for TIM 2.3.1.1834_1072 and QQ 8.0.0.4000_1024
            // there should be [abcd] 4 methods, select 'c'
            // public void .+\(String str\)
            val initCrashReport = kStatisticCollector.declaredMethods.single {
                it.isPublic && it.returnType == Void.TYPE && !it.isStatic && it.parameterTypes.size == 1 &&
                    it.name == "c" &&
                    it.parameterTypes[0] == java.lang.String::class.java
            }
            HookUtils.hookBeforeAlways(this, initCrashReport) {
                it.result = null
            }
        }
        return true
    }

}
