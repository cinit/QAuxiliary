/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2025 QAuxiliary developers
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

package cc.ioctl.hook.ui.chat

import cc.hicore.QApp.QAppUtils
import cc.ioctl.util.hookBeforeIfEnabled
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.step.Step
import io.github.qauxv.tlb.ConfigTable
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.PlayQQVersion
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.TIMVersion
import io.github.qauxv.util.dexkit.DexDeobfsProvider.getCurrentBackend
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.DexKitFinder
import io.github.qauxv.util.dexkit.Reply_At_QQNT
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.requireMinVersion
import java.lang.reflect.Modifier

@FunctionHookEntry
@UiItemAgentEntry
object ReplyNoAtHook : CommonSwitchFunctionHook(), DexKitFinder {

    override val name = "禁止回复自动@"
    override val description = "去除回复消息时自动@特性"
    override val extraSearchKeywords = arrayOf("艾特", "at")
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.UI_CHAT_MSG
    override val isAvailable = requireMinVersion(
        QQVersionCode = QQVersion.QQ_8_2_0,
        TimVersionCode = TIMVersion.TIM_3_1_1,
        PlayQQVersionCode = PlayQQVersion.PlayQQ_8_2_9,
    )

    override fun initOnce(): Boolean {
        if (QAppUtils.isQQnt()) {
            val method = DexKit.requireMethodFromCache(Reply_At_QQNT)
            hookBeforeIfEnabled(method) { param ->
                param.result = null
            }
        } else if (requireMinQQVersion(QQVersion.QQ_8_6_0)) {
            val className = ConfigTable.getConfig<String?>(ReplyNoAtHook::class.java.getSimpleName()) ?: return false
            val clazz = Initiator.loadClass(className)
            val method = clazz.declaredMethods.single { method ->
                val isStatic = Modifier.isStatic(method.modifiers)
                val isReturnVoid = method.returnType == Void::class.java
                val params = method.parameterTypes
                val isParamsTrue = params.size >= 3 && params[1] == Initiator._BaseSessionInfo() && params[2] == Boolean::class.java
                isStatic && isReturnVoid && isParamsTrue
            }
            hookBeforeIfEnabled(method) { param ->
                val p0 = param.args[2] as Boolean
                if (!p0) {
                    param.result = null
                }
            }
        } else {
            val methodName = ConfigTable.getConfig<String?>(ReplyNoAtHook::class.java.getSimpleName()) ?: return false
            val method = Initiator._BaseChatPie().getDeclaredMethod(methodName, Boolean::class.java)
            hookBeforeIfEnabled(method) { param ->
                val p0 = param.args[0] as Boolean
                if (!p0) {
                    param.result = null
                }
            }
        }
        return true
    }

    override val isNeedFind = QAppUtils.isQQnt() && Reply_At_QQNT.descCache == null

    override fun doFind(): Boolean {
        getCurrentBackend().use { backend ->
            val dexKit = backend.getDexKitBridge()
            val method = dexKit.findMethod {
                searchPackages("com.tencent.mobileqq.aio.input")
                matcher {
                    paramTypes("com.tencent.mobileqq.aio.msg.AIOMsgItem")
                    if (requireMinQQVersion(QQVersion.QQ_9_2_10)) {
                        usingStrings("mContext", "senderUid")
                    } else {
                        usingStrings("msgItem.msgRecord.senderUid")
                    }
                }
            }.firstOrNull() ?: return false
            Reply_At_QQNT.descCache = method.descriptor
            return true
        }
    }

    override fun makePreparationSteps(): Array<Step> = arrayOf(
        object : Step {
            override fun step() = doFind()
            override fun isDone() = !isNeedFind
            override fun getPriority() = 0
            override fun getDescription() = "${name}相关类查找中"
        }
    )
}