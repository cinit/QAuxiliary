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
import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion

@FunctionHookEntry
@UiItemAgentEntry
object DisableBigSticker : CommonSwitchFunctionHook() {

    override val name = "屏蔽大号Emoji"
    override val description = "屏蔽别人发送的超级表情, 仅显示小号Emoji"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    private fun changeFace(msgElements: ArrayList<*>) {
        for (element in msgElements) {
            val msgElement = (element as MsgElement)
            msgElement.faceElement?.apply {
                if (faceType == 3) stickerId = null
            }
        }
    }

    override fun initOnce(): Boolean {
        if (requireMinQQVersion(QQVersion.QQ_9_0_15)) {
            val msgItemClass = Initiator.loadClass("com.tencent.mobileqq.aio.msg.AIOMsgItem")
            hookAfterIfEnabled(msgItemClass.getDeclaredMethod("getMsgRecord")) { param ->
                val msgRecord = param.result as MsgRecord
                val msgElements = msgRecord.elements
                changeFace(msgElements)
            }
        } else {
            hookAfterIfEnabled(MsgRecord::class.java.getDeclaredMethod("getElements")) { param ->
                val msgElements = param.result as ArrayList<*>
                changeFace(msgElements)
            }
        }
        return true
    }
}