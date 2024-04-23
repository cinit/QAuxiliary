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
import de.robv.android.xposed.XposedHelpers
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion

@FunctionHookEntry
@UiItemAgentEntry
object ChangeGroupStatus : CommonSwitchFunctionHook() {

    override val name = "更改群聊状态"
    override val description = "查看消息列表中因涉嫌违规被停用群聊的消息"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.GROUP_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override fun initOnce(): Boolean {
        val groupStatusClass = Initiator.loadClass("com.tencent.qqnt.kernel.nativeinterface.GroupStatus")
        val KENABLE = XposedHelpers.getStaticObjectField(groupStatusClass, "KENABLE")
        val KDELETE = XposedHelpers.getStaticObjectField(groupStatusClass, "KDELETE")
        val KDISABLE = XposedHelpers.getStaticObjectField(groupStatusClass, "KDISABLE")

        val groupSimpleInfoClass = Initiator.loadClass("com.tencent.qqnt.kernel.nativeinterface.GroupSimpleInfo")
        val getGroupStatusMethod = groupSimpleInfoClass.getDeclaredMethod("getGroupStatus")
        hookAfterIfEnabled(getGroupStatusMethod) { param ->
            val groupStatus = param.result
            if (groupStatus == KDISABLE) {
                param.result = KENABLE
            }
        }
        return true
    }
}