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
package cc.ioctl.hook.ui.main

import cc.ioctl.util.HostInfo
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookAllConstructorAfter
import com.github.kyuubiran.ezxhelper.utils.paramCount
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.config.ConfigItems
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.step.Step
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Log
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.dexkit.DexDeobfsProvider.getCurrentBackend
import io.github.qauxv.util.dexkit.DexKitFinder
import io.github.qauxv.util.dexkit.impl.DexKitDeobfs
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.xpcompat.XC_MethodReplacement
import io.github.qauxv.util.xpcompat.XposedHelpers
import org.luckypray.dexkit.result.MethodData
import kotlin.collections.set

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

//        val zQQChatListTwoLevelHeader = Initiator.load("com.tencent.qqnt.chats.view.QQChatListTwoLevelHeader")
//        zQQChatListTwoLevelHeader?.findAllMethods { !Modifier.isStatic(modifiers) }?.hookAfter {
//            /*if (it.thisObject is ViewGroup) {
//                val vg = it.thisObject as ViewGroup
//                vg.visibility = View.GONE
//                vg.removeAllViews()
//            }*/
//            XposedHelpers.callMethod(it.thisObject, "g")
//              // lliiooll：自动收回，二层包装了，在这个方法里面调用的finishTwoLevelHeader
//        }


        Initiator.load("com.tencent.qqnt.chats.view.MiniOldStyleHeaderNew")?.let { clazz ->
            clazz.hookAllConstructorAfter {
                // Lcom/scwang/smart/refresh/header/TwoLevelHeader;
                // com.qqnt.widget.smartrefreshlayout.header.TwoLevelHeader
                it.thisObject.javaClass.superclass.superclass.superclass.declaredFields.first { field ->// mEnableTwoLevel
                    field.name == when {// RefreshState.ReleaseToTwoLevel
                        requireMinQQVersion(QQVersion.QQ_9_1_70) -> "I"// 9.1.70 ~ 9.1.75
                        requireMinQQVersion(QQVersion.QQ_9_1_30) -> "E"// 9.1.30 ~ 9.1.65
                        else -> "D"
                    }
                }.apply { isAccessible = true }.set(it.thisObject, false)
            }
//            val name0 = when {
//                requireMinQQVersion(QQVersion.QQ_9_0_50) -> "c"
//                else -> "a"
//            }
            clazz.findMethod { name == miniOldStyleHeaderNewMethod && paramCount == 3 }.hookAfter {
                XposedHelpers.callMethod(it.args[0], "finishRefresh")
            }
        } ?: run {
            Initiator.load("com.tencent.qqnt.chats.view.MiniOldStyleHeader")?.let {
                it.hookAllConstructorAfter {
                    // Lcom/scwang/smart/refresh/header/TwoLevelHeader;
                    it.thisObject.javaClass.superclass.superclass.superclass.declaredFields.first {
                        it.name == "D"  //mEnableTwoLevel
                    }.apply { isAccessible = true }.set(it.thisObject, false)
                }
                it.findMethod { name == "a" && paramCount == 3 }.hookAfter {
                    XposedHelpers.callMethod(it.args[0], "finishRefresh")
                }
            }
        }


        XposedHelpers.findAndHookMethod(
            Initiator._Conversation(), methodName,
            XC_MethodReplacement.returnConstant(null)
        )
        /*
        Initiator.load("com.tencent.mobileqq.activity.home.chats.c")
            ?.findAllMethods { true }
            ?.hookAfter {
                for (f in it.thisObject.javaClass.declaredFields) {
                    if (f.name == "o") {
                        f.isAccessible = true
                        val lObj = f.get(it.thisObject)
                        if (lObj is List<*>) {
                            val list = lObj.toMutableList()
                            list.clear()
                            f.set(it.thisObject, list.toList())
                        }
                    }
                }
            }

         */

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

    private val miniOldStyleHeaderNewMethod: String?
        get() {
            return ConfigManager.getCache().getString("qn_hide_miniapp_v2_mini_old_style_header_method_name")
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
        get() = initMiniAppObfsName == null || (Initiator.load("com.tencent.qqnt.chats.view.MiniOldStyleHeaderNew") != null && miniOldStyleHeaderNewMethod == null)

    override fun doFind(): Boolean {
        (getCurrentBackend() as DexKitDeobfs).use { dexKitDeobfs ->

            dexKitDeobfs.getDexKitBridge().findMethod {
                matcher {
                    usingStrings("refreshLayout", "oldState", "newState")
                    declaredClass("com.tencent.qqnt.chats.view.MiniOldStyleHeaderNew")
                    paramCount = 3
                }
            }.firstOrNull()?.let { ConfigManager.getCache().putString("qn_hide_miniapp_v2_mini_old_style_header_method_name", it.name) }

            val clz = Initiator._Conversation() ?: return false
            val conversationClassName = clz.name
            val strings = arrayOf(
                "initMiniAppEntryLayout.",
                "initMicroAppEntryLayout.",
                "init Mini App, cost="
            )
            val fnVerifyAndSaveResult: (MethodData) -> Boolean = { methodData ->
                if (methodData.className == conversationClassName && "()V" == methodData.methodSign) {
                    // save and return
                    val cache = ConfigManager.getCache()
                    cache.putInt("qn_hide_miniapp_v2_version_code", HostInfo.getVersionCode())
                    cache.putString("qn_hide_miniapp_v2_method_name", methodData.name)
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
                .batchFindMethodUsingStrings {
                    groups(map)
                }
            for (methods in res.values) {
                for (methodData in methods) {
                    if (fnVerifyAndSaveResult(methodData)) {
                        return true
                    }
                }
            }
            // for NT QQ 8.9.58.11040 (4054)+
            val candidates = dexKitDeobfs.getDexKitBridge().findMethod {
                matcher {
                    addInvoke {
                        declaredClass = "com.tencent.mobileqq.mini.api.IMiniAppService"
                        name = "createMiniAppEntryManager"
                    }
                }
            }.filter { caller ->
                Log.d("HideMiniAppPullEntry: caller = $caller")
                caller.className == conversationClassName
            }
            if (candidates.size != 1) {
                Log.e("HideMiniAppPullEntry: candidates.size expected 1 but got ${candidates.size}")
            }
            for (caller in candidates) {
                if (fnVerifyAndSaveResult(caller)) {
                    return true
                }
            }
            return false
        }
    }

}
