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
import android.widget.TextView
import cc.ioctl.util.hookAfterIfEnabled
import de.robv.android.xposed.XposedHelpers
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.base.MultiItemDelayableHook
import xyz.nextalone.util.isPrivate
import xyz.nextalone.util.isPublic

@FunctionHookEntry
@UiItemAgentEntry
object HideTipTextView : MultiItemDelayableHook("hd_HideTipTextView") {

    override val preferenceTitle = "隐藏提示文本消息"
    override val description = "当提示文本消息过多, 可能导致聊天界面滑动掉帧"
    override val dialogDesc = "隐藏"
    override val allItems = setOf(
        "我也要打卡",
        "加入了群聊",
    )
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.UI_CHAT_MSG
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override fun initOnce(): Boolean {
        val tipsClass = Initiator.loadClass("com.tencent.mobileqq.aio.msglist.holder.component.graptips.common.CommonGrayTipsComponent")
        val initMethod = tipsClass.declaredMethods.single { method ->
            val params = method.parameterTypes
            method.isPublic && method.returnType == Void.TYPE && params.size == 3 && params[0] == Int::class.java && params[2] == List::class.java
        }
        val getTextViewMethod = tipsClass.declaredMethods.single { method ->
            method.isPrivate && method.returnType == if (requireMinQQVersion(QQVersion.QQ_9_0_8)) {
                Initiator.loadClass("com.tencent.qqnt.aio.widget.AIOMsgTextView")
            } else {
                Initiator.loadClass("com.tencent.mobileqq.aio.msglist.holder.component.graptips.GrayTipsTextView")
            }
        }
        hookAfterIfEnabled(initMethod) {
            val textView = XposedHelpers.callMethod(it.thisObject, getTextViewMethod.name) as TextView
            activeItems.forEach { item ->
                if (textView.text.toString().contains(item)) {
                    val parent = textView.parent.parent as ViewGroup
                    parent.layoutParams = ViewGroup.LayoutParams(0, 0)
                    return@forEach
                }
            }
        }
        return true
    }
}