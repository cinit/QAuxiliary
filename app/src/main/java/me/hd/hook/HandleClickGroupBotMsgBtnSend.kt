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

import com.afollestad.materialdialogs.MaterialDialog
import com.github.kyuubiran.ezxhelper.utils.findFieldObjectAs
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.xiaoniu.util.ContextUtils
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.xpcompat.XposedBridge
import xyz.nextalone.util.get
import xyz.nextalone.util.method

@FunctionHookEntry
@UiItemAgentEntry
object HandleClickGroupBotMsgBtnSend : CommonSwitchFunctionHook() {

    override val name = "拦截点击群机器人消息按钮直接发送"
    override val description = "防止窥屏时不小心点到而被发现"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override fun initOnce(): Boolean {
        "Lcom/tencent/android/androidbypass/enhance/inlinekeyboard/view/InlineBtnView;->onClick(Landroid/view/View;)V".method.hookBefore { param ->
            param.result = null
            val activity = ContextUtils.getCurrentActivity()
            val context = CommonContextWrapper.createMaterialDesignContext(activity)

            val (btnModelName, prototypeName) = when {
                requireMinQQVersion(QQVersion.QQ_9_0_70) -> Pair("m", "d")//9.0.70~9.1.0
                requireMinQQVersion(QQVersion.QQ_9_0_60) -> Pair("j", "d")//9.0.60~9.0.68
                requireMinQQVersion(QQVersion.QQ_9_0_0) -> Pair("n", "d")//9.0.0~9.0.50
                else -> Pair("", "")
            }
            if (btnModelName != "" && prototypeName != "") {
                val btnModel = param.thisObject.get(btnModelName)
                val prototype = btnModel.get(prototypeName)
                val label = prototype.get("label") as String
                val type = prototype.get("type") as Int
                val enter = prototype.get("enter") as Boolean
                if (type == 2 && enter) {
                    MaterialDialog(context).show {
                        title(text = "是否发送内容")
                        message(text = label)
                        positiveButton(text = "是") {
                            XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                        }
                        negativeButton(text = "否")
                    }
                } else {
                    XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                }
            } else {
                MaterialDialog(context).show {
                    title(text = "是否发送或打开")
                    message(text = param.thisObject.findFieldObjectAs<String> { type == String::class.java })
                    positiveButton(text = "是") {
                        XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                    }
                    negativeButton(text = "否")
                }
            }

        }
        return true
    }
}