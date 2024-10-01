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

import android.annotation.SuppressLint
import android.widget.EditText
import cc.ioctl.util.hookAfterIfEnabled
import com.github.kyuubiran.ezxhelper.utils.findField
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
import io.github.qauxv.util.dexkit.AIO_InputRootInit_QQNT
import io.github.qauxv.util.dexkit.AbstractQQCustomMenuItem
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedHelpers

@SuppressLint("StaticFieldLeak")
@FunctionHookEntry
@UiItemAgentEntry
object EditTextContent : CommonSwitchFunctionHook(
    targets = arrayOf(
        AbstractQQCustomMenuItem,
        AIO_InputRootInit_QQNT,
    )
), OnMenuBuilder {

    override val name = "编辑重发文本消息"
    override val description = "消息菜单中新增功能"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    private var editText: EditText? = null

    override fun initOnce(): Boolean {
        hookAfterIfEnabled(DexKit.requireMethodFromCache(AIO_InputRootInit_QQNT)) { param ->
            param.thisObject.javaClass.findField {
                type == EditText::class.java
            }.let {
                editText = it.get(param.thisObject) as EditText
            }
        }
        return true
    }

    private const val TEXT_CONTEXT = "com.tencent.mobileqq.aio.msglist.holder.component.text.AIOTextContentComponent"
    private const val MIX_CONTEXT = "com.tencent.mobileqq.aio.msglist.holder.component.mix.AIOMixContentComponent"
    override val targetComponentTypes = arrayOf(TEXT_CONTEXT, MIX_CONTEXT)

    @SuppressLint("SetTextI18n")
    override fun onGetMenuNt(msg: Any, componentType: String, param: XC_MethodHook.MethodHookParam) {
        if (!isEnabled) return
        val msgRecord = XposedHelpers.callMethod(msg, "getMsgRecord") as MsgRecord
        if (msgRecord.sendType == 0) return
        val item = CustomMenu.createItemIconNt(msg, "编辑重发", R.drawable.ic_item_edit_72dp, R.id.item_edit_to_send) {
            when (componentType) {
                TEXT_CONTEXT -> {
                    val stringBuilder = StringBuilder()
                    msgRecord.elements.forEach { element ->
                        element.textElement?.let { textElement ->
                            stringBuilder.append(textElement.content)
                        }
                    }
                    editText?.setText(stringBuilder.toString())
                    recallMsg(msgRecord)
                }

                MIX_CONTEXT -> {
                    // TODO: 待开发
                    Toasts.show("快了快了, 已经新建文件夹了!")
                }
            }
        }
        param.result = listOf(item) + param.result as List<*>
    }

    private fun recallMsg(msgRecord: MsgRecord) {
        val context = CommonContextWrapper.createAppCompatContext(ContextUtils.getCurrentActivity())
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
}