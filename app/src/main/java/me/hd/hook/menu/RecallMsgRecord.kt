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

package me.hd.hook.menu

import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import com.xiaoniu.dispatcher.OnMenuBuilder
import com.xiaoniu.util.ContextUtils
import io.github.qauxv.R
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.bridge.AppRuntimeHelper
import io.github.qauxv.bridge.kernelcompat.ContactCompat
import io.github.qauxv.bridge.ntapi.MsgServiceHelper
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.CustomMenu
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.dexkit.AbstractQQCustomMenuItem
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedHelpers

@FunctionHookEntry
@UiItemAgentEntry
object RecallMsgRecord : CommonSwitchFunctionHook(
    targets = arrayOf(AbstractQQCustomMenuItem)
), OnMenuBuilder {

    override val name = "撤回特殊消息"
    override val description = "消息菜单中新增功能, 当前支持表情泡泡, 戳一戳, 红包转账"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override fun initOnce(): Boolean {
        return true
    }

    private const val FACE_BUBBLE_CONTEXT = "com.tencent.mobileqq.aio.msglist.holder.component.facebubble.AIOFaceBubbleContentComponent"
    private const val POKE_CONTEXT = "com.tencent.mobileqq.aio.msglist.holder.component.poke.AIOPokeContentComponent"
    private const val Q_WALLET = "com.tencent.mobileqq.aio.qwallet.AIOQWalletComponent"
    override val targetComponentTypes = arrayOf(FACE_BUBBLE_CONTEXT, POKE_CONTEXT, Q_WALLET)

    override fun onGetMenuNt(msg: Any, componentType: String, param: XC_MethodHook.MethodHookParam) {
        if (!isEnabled) return
        val item = CustomMenu.createItemIconNt(msg, "撤回消息", R.drawable.ic_item_recall_72dp, R.id.item_recall_msgRecord) {
            val context = CommonContextWrapper.createAppCompatContext(ContextUtils.getCurrentActivity())
            val msgRecord = XposedHelpers.callMethod(msg, "getMsgRecord") as MsgRecord
            val appRuntime = AppRuntimeHelper.getAppRuntime()!!
            val msgService = MsgServiceHelper.getKernelMsgService(appRuntime)!!
            msgService.recallMsg(
                ContactCompat(msgRecord.chatType, msgRecord.peerUid, ""),
                ArrayList<Long>(listOf(msgRecord.msgId)),
            ) { status, reason ->
                if (status == 0) {
                    Toasts.success(context, "撤回成功")
                } else {
                    Toasts.error(context, "撤回失败: $reason")
                }
            }
        }
        param.result = listOf(item) + param.result as List<*>
    }
}