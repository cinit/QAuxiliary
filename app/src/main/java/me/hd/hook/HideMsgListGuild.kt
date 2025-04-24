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

import android.view.ViewGroup
import android.widget.RelativeLayout
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedBridge

@FunctionHookEntry
@UiItemAgentEntry
object HideMsgListGuild : CommonSwitchFunctionHook() {

    override val name = "隐藏消息列表的QQ频道"
    override val description = "对消息列表中的QQ频道进行简单隐藏"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.CHAT_GROUP_OTHER
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override fun initOnce(): Boolean {
        val bindingClass = Initiator.loadClass(
            if (requireMinQQVersion(QQVersion.QQ_9_1_50)) "qu2.e" else "com.tencent.qqnt.chats.f.a.e"
        )
        XposedBridge.hookAllConstructors(
            bindingClass,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val field = bindingClass.declaredFields.single { field -> field.type == RelativeLayout::class.java }
                    val relativeLayout = field.get(param.thisObject) as RelativeLayout
                    val parent = relativeLayout.parent as ViewGroup
                    parent.layoutParams = ViewGroup.LayoutParams(0, 0)
                }
            }
        )
        return true
    }
}
