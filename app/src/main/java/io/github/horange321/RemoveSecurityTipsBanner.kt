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

package io.github.horange321

import com.github.kyuubiran.ezxhelper.utils.hookReplace
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.step.Step
import io.github.qauxv.util.dexkit.DexDeobfsProvider.getCurrentBackend
import io.github.qauxv.util.dexkit.DexKit.requireMethodFromCache
import io.github.qauxv.util.dexkit.DexKitFinder
import io.github.qauxv.util.dexkit.RemoveSecurityTipsBanner_Method


@FunctionHookEntry
@UiItemAgentEntry
object RemoveSecurityTipsBanner :
    CommonSwitchFunctionHook("removeSecurityTipsBanner", arrayOf(RemoveSecurityTipsBanner_Method)),
    DexKitFinder {
    override val name = "隐藏群聊风险提醒"
    override val uiItemLocation: Array<String> = Simplify.CHAT_GROUP_TITLE
    override val isNeedFind = RemoveSecurityTipsBanner_Method.descCache == null
    private val steps = object : Step {
        override fun step() = doFind()
        override fun isDone() = !isNeedFind
        override fun getPriority() = 0
        override fun getDescription() = "移除群聊顶部风险提醒"
    }
    override val description = steps.description

    override fun makePreparationSteps(): Array<Step> = arrayOf(steps)


    override fun initOnce(): Boolean {
        requireMethodFromCache(RemoveSecurityTipsBanner_Method).hookReplace { }
        return true
    }


    override fun doFind(): Boolean {
        getCurrentBackend().use { backend ->
            val dkb = backend.getDexKitBridge()
            dkb.findMethod {
                // void com.tencent.mobileqq.troop.tipsbar.TroopSecurityTipsBanner::doOnCreate(com.tencent.mobileqq.aio.notification.c)
                matcher {
                    declaredClass("com.tencent.mobileqq.troop.tipsbar.TroopSecurityTipsBanner")
                    returnType("void")
                    paramTypes("com.tencent.mobileqq.aio.notification.c")
                }
            }.single().let {
                RemoveSecurityTipsBanner_Method.descCache = it.descriptor
            }
        }
        return true
    }
}