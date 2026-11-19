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

import android.content.Context
import android.content.SharedPreferences
import cc.ioctl.util.hookBeforeIfEnabled
import com.github.kyuubiran.ezxhelper.utils.invokeAs
import com.github.kyuubiran.ezxhelper.utils.isPublic
import com.github.kyuubiran.ezxhelper.utils.isStatic
import com.tencent.mmkv.MMKV
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
        // QQ 9.2.66 applies the staged dex patch in the startup LoadDexTask, which runs before the
        // module is initialized, so drop the staged config to make the next launch skip the patch.
        attempt { clearStagedPatchConfig() }
        attempt { hookPatchDownloadTask() }
        attempt { hookPatchEngineBase() }
        attempt { hookMsfPatchHandler() }
        attempt { hookConfigServlet() }
        attempt { hookDexPatchInstaller() }
        attempt { hookPatchReporter() }
        attempt { hookLegacyPatchFileManager() }
        return true
    }

    private inline fun attempt(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            traceError(e)
        } catch (e: LinkageError) {
            traceError(e)
        }
    }

    private fun hookPatchDownloadTask() {
        // 9.0.35+
        Initiator.load("com.tencent.rfix.lib.download.PatchDownloadTask")?.let { kPatchDownloadTask ->
            val run = kPatchDownloadTask.getDeclaredMethod("run")
            hookBeforeIfEnabled(run) {
                it.result = null
            }
        }
    }

    private fun hookPatchEngineBase() {
        Initiator.load("com.tencent.rfix.lib.engine.PatchEngineBase")?.let { kPatchEngineBase ->
            val kPatchConfig = Initiator.loadClass("com.tencent.rfix.lib.config.PatchConfig")
            val onPatchReceived = kPatchEngineBase.declaredMethods.singleOrNull {
                val argt = it.parameterTypes
                it.returnType == Void.TYPE && argt.size == 2 && argt[0] == String::class.java && argt[1] == kPatchConfig
            } ?: return
            hookBeforeIfEnabled(onPatchReceived) {
                it.result = null
            }
        }
    }

    private fun hookMsfPatchHandler() {
        // below 9.0.35, and the MSF download path is still used on 9.2.66
        Initiator.load("com.tencent.mobileqq.msf.core.net.utils.MsfHandlePatchUtils")?.let { kMsfHandlePatchUtils ->
            kMsfHandlePatchUtils.declaredMethods.singleOrNull { m ->
                m.name == "handlePatchConfig" && m.returnType == Void.TYPE && run {
                    val argt = m.parameterTypes
                    argt.size == 2 && argt[0] == Int::class.javaPrimitiveType && argt[1] == java.util.List::class.java
                }
            }?.let { handlePatchConfig ->
                hookBeforeIfEnabled(handlePatchConfig) {
                    it.result = null
                }
            }
            // 9.2.66 downloads and verifies the dex patch directly here
            kMsfHandlePatchUtils.declaredMethods.filter { m ->
                m.name == "downloadAndVerifyPatch" && m.returnType == Void.TYPE && m.parameterTypes.size == 1
            }.forEach { m ->
                XposedBridge.deoptimizeMethod(m)
                hookBeforeIfEnabled(m) {
                    it.result = null
                }
            }
        }
    }

    private fun hookConfigServlet() {
        Initiator.load("com.tencent.mobileqq.config.splashlogo.ConfigServlet")?.let { kConfigServlet ->
            val kRespGetConfig = Initiator.loadClass("com.tencent.mobileqq.config.struct.splashproto.ConfigurationService\$RespGetConfig")
            val kRespGetConfig_config_list = kRespGetConfig.getDeclaredField("config_list")
            val kConfig = Initiator.loadClass("com.tencent.mobileqq.config.struct.splashproto.ConfigurationService\$Config")
            val kConfig_type = kConfig.getDeclaredField("type")
            // QQ 9.2.66 reordered the parameters of the 6-arg handler, so match by parameter type
            // instead of by fixed positions.
            kConfigServlet.declaredMethods.filter { m ->
                m.returnType == Void.TYPE && m.parameterTypes.contains(kRespGetConfig)
            }.forEach { m ->
                XposedBridge.deoptimizeMethod(m)
                hookBeforeIfEnabled(m) {
                    val index = m.parameterTypes.indexOf(kRespGetConfig)
                    val respGetConfig = it.args.getOrNull(index) ?: return@hookBeforeIfEnabled
                    val config_list = kRespGetConfig_config_list.get(respGetConfig)
                        as? PBRepeatMessageField<*> ?: return@hookBeforeIfEnabled
                    val arrayList = config_list.get() as? java.util.ArrayList<*> ?: return@hookBeforeIfEnabled
                    if (arrayList.isEmpty()) {
                        return@hookBeforeIfEnabled
                    }
                    // remove all hotpatch config, type == 46
                    arrayList.removeIf { config ->
                        config != null && (kConfig_type.get(config) as? PBInt32Field)?.get() == 46
                    }
                    // if the array is empty, do not call the original method
                    if (arrayList.isEmpty()) {
                        it.result = null
                    }
                }
            }
            // deoptimize the variants without the proto object, e.g. the old (AppRuntime, List) one
            kConfigServlet.declaredMethods.filter { m ->
                m.returnType == Void.TYPE && m.parameterTypes.size == 2
                    && m.parameterTypes[0] == AppRuntime::class.java && m.parameterTypes[1] == java.util.List::class.java
            }.forEach { XposedBridge.deoptimizeMethod(it) }
        }
    }

    private fun hookDexPatchInstaller() {
        // 9.2.66: installs the dex patch staged by the MSF config
        Initiator.load("com.tencent.hotpatch.DexPatchInstaller")?.let { kDexPatchInstaller ->
            kDexPatchInstaller.declaredMethods.filter { m ->
                m.isStatic && m.returnType == Void.TYPE && m.parameterTypes.isNotEmpty()
                    && m.parameterTypes[0] == Context::class.java
            }.forEach { m ->
                XposedBridge.deoptimizeMethod(m)
                hookBeforeIfEnabled(m) {
                    it.result = null
                }
            }
        }
    }

    private fun hookPatchReporter() {
        Initiator.load("com.tencent.mobileqq.msf.core.net.patch.PatchReporter")?.let { kPatchReporter ->
            kPatchReporter.declaredMethods.filter {
                it.name.startsWith("report") && it.returnType == Void.TYPE
            }.forEach { m ->
                hookBeforeIfEnabled(m) { p ->
                    p.result = null
                }
            }
        }
    }

    private fun hookLegacyPatchFileManager() {
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

    private fun clearStagedPatchConfig() {
        if (!Initiator.checkHostHasClass("com.tencent.mmkv.MMKV")) {
            return
        }
        val kMMKV = Initiator.loadClass("com.tencent.mmkv.MMKV")
        val mmkvWithID = kMMKV.declaredMethods.singleOrNull {
            it.returnType == kMMKV && it.isStatic && it.isPublic && run {
                val argt = it.parameterTypes
                argt.size == 2 && argt[0] == String::class.java && argt[1] == Int::class.java
            }
        } ?: return
        val prefs = mmkvWithID.invokeAs<SharedPreferences>(
            null, "common_mmkv_configurations", MMKV.MULTI_PROCESS_MODE
        ) ?: return
        if (prefs.getString("key_msf_config_rfix_patch_content", null).isNullOrEmpty()) {
            return
        }
        prefs.edit()
            .putBoolean("rfix_patch_info#remove_patch", true)
            .remove("key_msf_config_rfix_patch_content")
            .remove("key_msf_config_rfix_version_patch")
            .commit()
    }

}
