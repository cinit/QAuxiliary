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

import android.view.View
import cc.ioctl.util.hookAfterIfEnabled
import com.github.kyuubiran.ezxhelper.utils.setViewZeroSize
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
object HideFriendChatVoice : CommonSwitchFunctionHook() {

    override val name = "隐藏好友聊天语音"
    override val description = "对聊天右上角的语音图标进行简单隐藏"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.CHAT_GROUP_TITLE
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override fun initOnce(): Boolean {
        val setOnClickClass = if (requireMinQQVersion(QQVersion.QQ_9_0_70)) {
            Initiator.loadClass("com.tencent.mobileqq.aio.title.right2.b")
        } else if (requireMinQQVersion(QQVersion.QQ_9_0_65)) {
            Initiator.loadClass("com.tencent.mobileqq.aio.title.c.c")
        } else {
            Initiator.loadClass("com.tencent.mobileqq.aio.title.c.d")
        }
        val bindMethod = setOnClickClass.getDeclaredMethod("bindViewAndData")
        hookAfterIfEnabled(bindMethod) { param ->
            val redDotImageView = XposedHelpers.getObjectField(
                param.thisObject,
                if (requireMinQQVersion(QQVersion.QQ_9_0_65)) "f" else "e"
            ) as View
            redDotImageView.setViewZeroSize()
        }
        return true
    }
}