/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2026 QAuxiliary developers
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

package me.hd.hook.auxiliary.experimental

import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import me.hd.util.hookAfterIfEnabled
import me.hd.util.name
import me.hd.util.parameterCount
import me.hd.util.parameters
import me.hd.util.singleMethod
import me.hd.util.toHostClass

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
            "com.tencent.mobileqq.aio.msg.AIOMsgItem".toHostClass()
                .singleMethod {
                    name("getMsgRecord") &&
                        parameterCount(0)
                }.hookAfterIfEnabled(this) { param ->
                    val msgRecord = param.result as MsgRecord
                    val msgElements = msgRecord.elements
                    changeFace(msgElements)
                }
        } else {
            MsgRecord::class.java
                .singleMethod {
                    name("getElements") &&
                        parameterCount(0)
                }.hookAfterIfEnabled(this) { param ->
                    val msgElements = param.result as ArrayList<*>
                    changeFace(msgElements)
                }
        }
        return true
    }
}
