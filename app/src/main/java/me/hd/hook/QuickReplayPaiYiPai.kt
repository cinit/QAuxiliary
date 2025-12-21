/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2025 QAuxiliary developers
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
import cc.hicore.message.chat.SessionHooker.IAIOParamUpdate
import cc.hicore.message.chat.SessionUtils
import cc.ioctl.util.hookBeforeIfEnabled
import com.afollestad.materialdialogs.MaterialDialog
import com.github.kyuubiran.ezxhelper.utils.argTypes
import com.github.kyuubiran.ezxhelper.utils.args
import com.github.kyuubiran.ezxhelper.utils.findFieldObject
import com.github.kyuubiran.ezxhelper.utils.invokeMethod
import com.github.kyuubiran.ezxhelper.utils.invokeMethodAs
import com.xiaoniu.util.ContextUtils
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.bridge.AppRuntimeHelper
import io.github.qauxv.bridge.ntapi.RelationNTUinAndUidApi
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.Hd_QuickReplayPaiYiPai_Method
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.xpcompat.XposedBridge

@FunctionHookEntry
@UiItemAgentEntry
object QuickReplayPaiYiPai : CommonSwitchFunctionHook(
    targets = arrayOf(Hd_QuickReplayPaiYiPai_Method)
), IAIOParamUpdate {

    override val name = "快捷回拍"
    override val description = "单击对方发起的拍一拍提示中的昵称, 可触发快捷回拍弹窗"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_9_1_35)

    override val runtimeErrorDependentComponents = null

    private var AIOParam: Any? = null
    override fun onAIOParamUpdate(param: Any?) {
        AIOParam = param
    }

    /**
     * 发送拍一拍
     *
     * 私聊: (对方,对方,1,0)
     * 群聊: (对方,群号,2,0)
     */
    private fun sendPaiYiPai(toUin: String, friendUin: String, uinType: Int, where: Int = 0) {
        val kString = String::class.java
        val kInt = Int::class.java
        val app = AppRuntimeHelper.getQQAppInterface()
        val handlerName = Initiator.loadClass("com.tencent.mobileqq.paiyipai.PaiYiPaiHandler").name
        val handlerInstance = app.invokeMethod("getBusinessHandler", args(handlerName), argTypes(kString))!!
        handlerInstance.invokeMethod("S2", args(toUin, friendUin, uinType, where), argTypes(kString, kString, kInt, kInt))
    }

    override fun initOnce(): Boolean {
        val method = DexKit.requireMethodFromCache(Hd_QuickReplayPaiYiPai_Method)
        hookBeforeIfEnabled(method) { param ->
            val view = param.args[0] as View
            if (view is TextView && view.text.contains("你的")) {
                val clickableSpan = param.thisObject
                val item = clickableSpan.findFieldObject { name == "i" }
                val actionInfo = item.findFieldObject { name == "d" }
                val nick = actionInfo.invokeMethodAs<String>("a")
                val uid = actionInfo.invokeMethodAs<String>("b")
                val toUin = RelationNTUinAndUidApi.getUinFromUid(uid)

                val activity = ContextUtils.getCurrentActivity()
                val context = CommonContextWrapper.createMaterialDesignContext(activity)
                MaterialDialog(context).show {
                    title(text = "询问")
                    message(text = "是否对 $nick 发送回拍?")
                    positiveButton(text = "发送") {
                        val contact = SessionUtils.AIOParam2Contact(AIOParam)
                        val friendUin = contact.peerUid.let { uin ->
                            if (uin.startsWith("u_")) RelationNTUinAndUidApi.getUinFromUid(uin) else uin
                        }
                        if (contact.chatType == 1 || contact.chatType == 2) {
                            sendPaiYiPai(toUin, friendUin, contact.chatType)
                            Toasts.success(context, "已发送回拍")
                        }
                    }
                    negativeButton(text = "原操作") {
                        XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                    }
                }
                param.result = null
            }
        }
        return true
    }
}