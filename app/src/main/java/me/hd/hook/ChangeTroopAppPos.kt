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

package me.hd.hook

import cc.ioctl.util.hookAfterIfEnabled
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion

@FunctionHookEntry
@UiItemAgentEntry
object ChangeTroopAppPos : CommonSwitchFunctionHook() {

    override val name = "更改群应用卡片位置"
    override val description = "群聊成员下面(✅), 个性设置上面(❌)"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.GROUP_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override fun initOnce(): Boolean {
        val settingFragmentClass = Initiator.loadClass("com.tencent.mobileqq.troop.troopsetting.activity.TroopSettingFragmentV2")
        val memberInfoPartClass = Initiator.loadClass("com.tencent.mobileqq.troop.troopsetting.part.TroopSettingMemberInfoPart")
        val appPartClass = Initiator.loadClass("com.tencent.mobileqq.troop.troopsetting.part.TroopSettingAppPart")
        val getListMethod = settingFragmentClass.getDeclaredMethod("assembleParts")
        hookAfterIfEnabled(getListMethod) { param ->
            val oldList = param.result as ArrayList<*>
            val newList = ArrayList(oldList)
            var memberInfoPartIndex = -1
            var appPart: Any? = null
            newList.forEachIndexed { index, part ->
                if (part.javaClass == memberInfoPartClass) memberInfoPartIndex = index
                if (part.javaClass == appPartClass) appPart = part
            }
            if (memberInfoPartIndex != -1 && appPart != null) {
                newList.remove(appPart)
                newList.add(memberInfoPartIndex + 1, appPart!!)
                param.result = newList
            }
        }
        return true
    }
}