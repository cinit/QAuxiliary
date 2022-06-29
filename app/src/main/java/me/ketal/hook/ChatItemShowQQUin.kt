/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package me.ketal.hook

import android.view.View
import android.view.ViewGroup
import cc.ioctl.util.Reflex
import cc.ioctl.util.ui.FaultyDialog
import de.robv.android.xposed.XC_MethodHook
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.bridge.AIOUtilsImpl
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.ui.CustomDialog
import me.ketal.dispacher.OnBubbleBuilder
import me.singleneuron.data.MsgRecordData
import xyz.nextalone.util.method
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@UiItemAgentEntry
object ChatItemShowQQUin : CommonSwitchFunctionHook(), OnBubbleBuilder {

    override val name = "消息显示发送者QQ号和时间"
    override val description = "可能导致聊天界面滑动掉帧"
    override fun initOnce() = isAvailable
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY

    private val mListener = View.OnClickListener {
        try {
            val msg = AIOUtilsImpl.getChatMessage(it)!!
            val chatMessage = MsgRecordData(msg)
            CustomDialog.createFailsafe(it.context)
                .setTitle(Reflex.getShortClassName(chatMessage.msgRecord))
                .setMessage(chatMessage.msgRecord.toString())
                .setPositiveButton("确认", null)
                .show()
        } catch (e: Exception) {
            FaultyDialog.show(it.context, e)
        }
    }

    private lateinit var pfnSetTailMessage: Method

    private val mDataFormatter: SimpleDateFormat by lazy {
        SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
    }

    override fun onGetView(rootView: ViewGroup, chatMessage: MsgRecordData, param: XC_MethodHook.MethodHookParam) {
        if (!isEnabled) return
        val text = "QQ:${chatMessage.senderUin}  Time:" + mDataFormatter.format(Date(chatMessage.time!! * 1000))
        if (!::pfnSetTailMessage.isInitialized) {
            pfnSetTailMessage =
                "Lcom/tencent/mobileqq/activity/aio/BaseChatItemLayout;->setTailMessage(ZLjava/lang/CharSequence;Landroid/view/View\$OnClickListener;)V".method
        }
        pfnSetTailMessage.invoke(rootView, true, text, mListener)
    }
}
