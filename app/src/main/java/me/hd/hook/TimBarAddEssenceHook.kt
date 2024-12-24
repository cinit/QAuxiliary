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

import android.content.Intent
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.children
import cc.hicore.message.chat.SessionHooker.IAIOParamUpdate
import cc.hicore.message.chat.SessionUtils
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.lxj.xpopup.util.XPopupUtils
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.TIMVersion
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.requireMinTimVersion

@FunctionHookEntry
@UiItemAgentEntry
object TimBarAddEssenceHook : CommonSwitchFunctionHook(), IAIOParamUpdate {
    override val name = "TIM 群标题栏添加精华消息入口"
    override val description = "仅适配 TIM_NT"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY
    override val isAvailable = requireMinTimVersion(TIMVersion.TIM_4_0_95)

    private val Layout_Id = "TimBarAddEssenceHook".hashCode()
    private var AIOParam: Any? = null

    override val runtimeErrorDependentComponents = null
    override fun onAIOParamUpdate(param: Any?) {
        this.AIOParam = param
    }

    override fun initOnce(): Boolean {
        Initiator.loadClass("com.tencent.tim.aio.titlebar.TimRight1VB").findMethod {
            returnType == Initiator.loadClass("com.tencent.mobileqq.aio.widget.RedDotImageView")
        }.hookAfter { param ->
            val view = param.result as View
            val rootView = view.parent as ViewGroup
            if (!rootView.children.map { it.id }.contains(Layout_Id)) {
                val textView = TextView(view.context).apply {
                    id = Layout_Id
                    text = "精"
                    textSize = 16f
                    setTextColor(Color.BLACK)
                    layoutParams = RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                        addRule(RelativeLayout.CENTER_VERTICAL)
                        marginEnd = XPopupUtils.dp2px(view.context, 60f)
                    }
                }
                textView.setOnClickListener {
                    val contact = SessionUtils.AIOParam2Contact(AIOParam)
                    val troopUin = contact.peerUid
                    try {
                        val browser = Initiator.loadClass("com.tencent.mobileqq.activity.QQBrowserDelegationActivity")
                        it.context.startActivity(
                            Intent(it.context, browser).apply {
                                putExtra("fling_action_key", 2)
                                putExtra("fling_code_key", it.context.hashCode())
                                putExtra("useDefBackText", true)
                                putExtra("param_force_internal_browser", true)
                                putExtra("url", "https://qun.qq.com/essence/index?gc=$troopUin")
                            }
                        )
                    } catch (e: Exception) {
                        Toasts.error(it.context, "无法启动内置浏览器")
                        e.printStackTrace()
                    }
                }
                rootView.addView(textView)
            }
        }
        return true
    }
}