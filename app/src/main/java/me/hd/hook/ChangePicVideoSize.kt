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

import cc.ioctl.util.hookBeforeIfEnabled
import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.util.method

@FunctionHookEntry
@UiItemAgentEntry
object ChangePicVideoSize : CommonSwitchFunctionHook() {

    override val name = "篡改[图片/视频]比例"
    override val description = "使发送的[图片/视频]在群聊中显示的非常小"
    override val uiItemLocation = FunctionEntryRouter.Locations.Entertainment.ENTERTAIN_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override fun initOnce(): Boolean {
        val msgServiceClass = Initiator.loadClass("com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService\$CppProxy")
        hookBeforeIfEnabled(msgServiceClass.method("sendMsg")!!) { param ->
            val elements = param.args[2] as ArrayList<*>
            for (element in elements) {
                val msgElement = (element as MsgElement)
                msgElement.picElement?.apply {
                    picWidth = -1
                    picHeight = -1
                }
                msgElement.videoElement?.apply {
                    thumbWidth = -1
                    thumbHeight = -1
                }
            }
        }
        return true
    }
}