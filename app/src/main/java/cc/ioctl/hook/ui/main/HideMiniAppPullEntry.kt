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
package cc.ioctl.hook.ui.main

import cc.ioctl.util.HostInfo
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.config.ConfigItems
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.step.Step
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Log
import io.github.qauxv.util.dexkit.DexDeobfsProvider.getCurrentBackend
import io.github.qauxv.util.dexkit.DexKitFinder
import io.github.qauxv.util.dexkit.impl.DexKitDeobfs
import io.luckypray.dexkit.builder.BatchFindArgs.Companion.builder
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor
import io.luckypray.dexkit.enums.MatchType
import io.luckypray.dexkit.util.DexDescriptorUtil.getTypeSig

@FunctionHookEntry
@UiItemAgentEntry
object HideMiniAppPullEntry : CommonSwitchFunctionHook(ConfigItems.qn_hide_msg_list_miniapp), DexKitFinder {

    override val name = "隐藏下拉小程序"
    override val description = "生成屏蔽下拉小程序解决方案"
    override val isApplicationRestartRequired = true
    override val isAvailable: Boolean get() = !HostInfo.isTim()
    override val uiItemLocation = Simplify.MAIN_UI_TITLE

    override fun initOnce(): Boolean {
        if (HostInfo.isTim()) {
            return false
        }
        val methodName = initMiniAppObfsName
        if (methodName == null) {
            traceError(RuntimeException("getInitMiniAppObfsName() == null"))
            return false
        }
        XposedHelpers.findAndHookMethod(
            Initiator._Conversation(), methodName,
            XC_MethodReplacement.returnConstant(null)
        )
//        val m = Reflex.findMethod(Initiator._Conversation(), Void.TYPE, "j0")
//        XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(null))
        return true
    }

    private val initMiniAppObfsName: String?
        /**
         * Fast fail
         */
        get() {
            val cache = ConfigManager.getCache()
            val lastVersion = cache.getIntOrDefault("qn_hide_miniapp_v2_version_code", 0)
            val methodName = cache.getString("qn_hide_miniapp_v2_method_name")
            return if (HostInfo.getVersionCode() == lastVersion) {
                methodName
            } else null
        }

    private val mStep: Step = object : Step {

        override fun step(): Boolean {
            return doFind()
        }

        override fun isDone(): Boolean {
            return !isNeedFind
        }

        override fun getPriority() = 0

        override fun getDescription() = "生成隐藏下拉小程序解决方案"

    }

    override fun makePreparationSteps() = arrayOf(mStep)

    override val isNeedFind: Boolean
        get() = initMiniAppObfsName == null

    override fun doFind(): Boolean {
        val clz = Initiator._Conversation() ?: return false
        val conversationSig = getTypeSig(clz)
        (getCurrentBackend() as DexKitDeobfs).use { dexKitDeobfs ->
            val strings = arrayOf(
                "initMiniAppEntryLayout.",
                "initMicroAppEntryLayout.",
                "init Mini App, cost="
            )
            val fnVerifyAndSaveResult: (DexMethodDescriptor) -> Boolean = { methodDesc ->
                if (methodDesc.declaringClassSig == conversationSig && "()V" == methodDesc.signature) {
                    // save and return
                    val cache = ConfigManager.getCache()
                    cache.putInt("qn_hide_miniapp_v2_version_code", HostInfo.getVersionCode())
                    cache.putString("qn_hide_miniapp_v2_method_name", methodDesc.name)
                    cache.save()
                    true
                } else {
                    false
                }
            }
            // non-NT QQ
            val map: MutableMap<String, Set<String>> = HashMap(16)
            for (i in strings.indices) {
                val set: MutableSet<String> = HashSet(1)
                set.add(strings[i])
                map["Conversation_$i"] = set
            }
            val res = dexKitDeobfs.getDexKitBridge()
                .batchFindMethodsUsingStrings(
                    builder()
                        .queryMap(map)
                        .matchType(MatchType.CONTAINS)
                        .build()
                )
            for (methods in res.values) {
                for (methodDesc in methods) {
                    if (fnVerifyAndSaveResult(methodDesc)) {
                        return true
                    }
                }
            }
            // for NT QQ 8.9.58.11040 (4054)+
            val candidates = dexKitDeobfs.getDexKitBridge().findMethodCaller {
                methodDeclareClass = "com.tencent.mobileqq.mini.api.IMiniAppService"
                methodName = "createMiniAppEntryManager"
            }.filter { (caller, _) ->
                Log.d("HideMiniAppPullEntry: caller = $caller")
                caller.declaringClassSig == conversationSig
            }
            if (candidates.size != 1) {
                Log.e("HideMiniAppPullEntry: candidates.size expected 1 but got ${candidates.size}")
            }
            for ((caller, _) in candidates) {
                if (fnVerifyAndSaveResult(caller)) {
                    return true
                }
            }
            return false
        }
    }

}
