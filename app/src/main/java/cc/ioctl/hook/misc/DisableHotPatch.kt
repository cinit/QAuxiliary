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

import android.content.Intent
import cc.ioctl.util.hookBeforeIfEnabled
import com.github.kyuubiran.ezxhelper.utils.isStatic
import com.tencent.mobileqq.app.QQAppInterface
import com.tencent.mobileqq.pb.PBInt32Field
import com.tencent.mobileqq.pb.PBRepeatMessageField
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.hostInfo
import io.github.qauxv.util.xpcompat.XposedBridge
import mqq.app.AppRuntime
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
        // below 9.0.35
        Initiator.load("com.tencent.mobileqq.msf.core.net.utils.MsfHandlePatchUtils")?.let { kMsfHandlePatchUtils ->
            val handlePatchConfig = kMsfHandlePatchUtils.getDeclaredMethod(
                "handlePatchConfig",
                Int::class.javaPrimitiveType,
                java.util.List::class.java
            )
            hookBeforeIfEnabled(handlePatchConfig) {
                it.result = null
            }
        }
        Initiator.load("com.tencent.mobileqq.config.splashlogo.ConfigServlet")?.let { kConfigServlet ->
            val kRespGetConfig = Initiator.loadClass("com.tencent.mobileqq.config.struct.splashproto.ConfigurationService\$RespGetConfig")
            val kRespGetConfig_config_list = kRespGetConfig.getDeclaredField("config_list")
            val kConfig = Initiator.loadClass("com.tencent.mobileqq.config.struct.splashproto.ConfigurationService\$Config")
            val kConfig_type = kConfig.getDeclaredField("type")
            //public void ?(AppRuntime appRuntime, ConfigurationService.RespGetConfig respGetConfig, Intent intent, List<Integer> list, int[] iArr, boolean z)
            val m1 = kConfigServlet.declaredMethods.single {
                val argt = it.parameterTypes
                it.returnType == Void.TYPE && argt.size == 6
                    && argt[0] == AppRuntime::class.java && argt[1] == kRespGetConfig
                    && argt[2] == Intent::class.java && argt[3] == java.util.List::class.java
                    && argt[4] == IntArray::class.java && argt[5] == Boolean::class.javaPrimitiveType
            }
            //public void ?(AppRuntime appRuntime, List<Integer> list)
            val m2 = kConfigServlet.declaredMethods.single {
                val argt = it.parameterTypes
                it.returnType == Void.TYPE && argt.size == 2
                    && argt[0] == AppRuntime::class.java && argt[1] == java.util.List::class.java
            }
            //public void ?(AppRuntime appRuntime, ConfigurationService.RespGetConfig respGetConfig, Intent intent, int[] iArr, boolean z)
            val m3 = kConfigServlet.declaredMethods.single {
                val argt = it.parameterTypes
                it.returnType == Void.TYPE && argt.size == 5
                    && argt[0] == AppRuntime::class.java && argt[1] == kRespGetConfig
                    && argt[2] == Intent::class.java && argt[3] == IntArray::class.java
                    && argt[4] == Boolean::class.javaPrimitiveType
            }
            XposedBridge.deoptimizeMethod(m1)
            XposedBridge.deoptimizeMethod(m2)
            XposedBridge.deoptimizeMethod(m3)
            hookBeforeIfEnabled(m1) {
                val respGetConfig = it.args[1] ?: return@hookBeforeIfEnabled
                val config_list = kRespGetConfig_config_list.get(respGetConfig)
                    as? PBRepeatMessageField<*> ?: return@hookBeforeIfEnabled
                val arrayList = config_list.get() as java.util.ArrayList<*>
                if (arrayList.isEmpty()) {
                    return@hookBeforeIfEnabled
                }
                // debug dump type
                arrayList.forEach { config ->
                    val type = (kConfig_type.get(config) as PBInt32Field).get()
                }
                // remove all hotpatch config, type == 46
                arrayList.removeIf { config ->
                    val type = (kConfig_type.get(config) as PBInt32Field).get()
                    type == 46
                }
                // if the array is empty, do not call the original method
                if (arrayList.isEmpty()) {
                    it.result = null
                }
            }
        }
        Initiator.load("com.tencent.mobileqq.msf.core.net.patch.PatchReporter")?.let { kPatchReporter ->
            kPatchReporter.declaredMethods.filter {
                it.name.startsWith("report") && it.returnType == Void.TYPE
            }.forEach { m ->
                hookBeforeIfEnabled(m) { p ->
                    p.result = null
                }
            }
        }
        run {
            arrayOf(
                "com.tencent.hotpatch.PatchFileManager",
                "com.tencent.hotpatch.c",
                "com.tencent.hotpatch.a",
                "com.tencent.hotpatch.b"
            ).mapNotNull { Initiator.load(it) }.singleOrNull { klass ->
                if (klass.superclass != java.lang.Object::class.java) {
                    return@singleOrNull false
                }
                val methods = klass.declaredMethods
                if (methods.size > 4) {
                    return@singleOrNull false
                }
                methods.filter { m ->
                    m.isStatic && m.returnType == Void.TYPE && run {
                        val argt = m.parameterTypes
                        argt.size == 1 && argt[0] == QQAppInterface::class.java
                    }
                }.size == 2
            }?.let { kPatchFileManager ->
                kPatchFileManager.declaredMethods.filter { m ->
                    m.isStatic && m.returnType == Void.TYPE && run {
                        val argt = m.parameterTypes
                        argt.size == 1 && argt[0] == QQAppInterface::class.java
                    }
                }.forEach { m ->
                    hookBeforeIfEnabled(m) { p ->
                        p.result = null
                    }
                }
            }
        }
        return true
    }

}
