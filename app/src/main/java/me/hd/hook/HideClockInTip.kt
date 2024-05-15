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
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.util.isPrivate

@FunctionHookEntry
@UiItemAgentEntry
object HideClockInTip : CommonSwitchFunctionHook() {

    override val name = "隐藏打卡消息"
    override val description = "对提示消息中的打卡消息进行简单隐藏"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.CHAT_GROUP_OTHER
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override fun initOnce(): Boolean {
        /**
         * version 8.9.88(4852)
         *
         * class [ com/tencent/mobileqq/aio/msglist/holder/component/graptips/common/CommonGrayTipsComponent ]
         *
         * method [ private final t1()Lcom/tencent/mobileqq/aio/msglist/holder/component/graptips/GrayTipsTextView; ]
         */
        val tipsClass = Initiator.loadClass("com.tencent.mobileqq.aio.msglist.holder.component.graptips.common.CommonGrayTipsComponent")
        val tipsTextViewClass = Initiator.loadClass("com.tencent.mobileqq.aio.msglist.holder.component.graptips.GrayTipsTextView")
        val getTextViewMethod = tipsClass.declaredMethods.single { method ->
            method.isPrivate && method.returnType == tipsTextViewClass
        }
        getTextViewMethod.hookAfter { param ->
            val textView = param.result as TextView
            val text = textView.text.toString()
            if (text.endsWith("我也要打卡")) {
                textView.visibility = View.GONE
            }
        }
        return true
    }
}