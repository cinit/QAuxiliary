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

package nep.timeline

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewStub
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.children
import com.github.kyuubiran.ezxhelper.utils.argTypes
import com.github.kyuubiran.ezxhelper.utils.args
import com.github.kyuubiran.ezxhelper.utils.invokeMethod
import com.github.kyuubiran.ezxhelper.utils.newInstance
import com.lxj.xpopup.util.XPopupUtils
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.bridge.AppRuntimeHelper
import io.github.qauxv.bridge.ntapi.MsgConstants
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.ui.CustomDialog
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.TIMVersion
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.requireMinTimVersion
import io.github.qauxv.util.xpcompat.XC_MethodHook
import kotlinx.coroutines.flow.MutableStateFlow
import me.ketal.dispacher.BaseBubbleBuilderHook
import me.ketal.dispacher.OnBubbleBuilder
import me.singleneuron.data.MsgRecordData
import xyz.nextalone.util.clazz
import xyz.nextalone.util.findHostView

@UiItemAgentEntry
object PromptForNoSeqMessage : CommonSwitchFunctionHook(), OnBubbleBuilder {
    override val name = "提示 NoSeq 消息"
    override val description = "对于一些因为弱网可能没发出去或者被腾讯拦截的消息进行提示"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY

    override fun initOnce(): Boolean {
        return isAvailable && BaseBubbleBuilderHook.initialize()
    }

    // For NT
    private const val ID_ADD_LAYOUT = 0x114515
    private const val ID_ADD_TEXTVIEW = 0x114516

    // X2J_APT <- ???Binding(com/tx/x2j/AioSenderBubbleTemplateBinding) <- AIOSenderBubbleTemplate
    private val NAME_TAIL_LAYOUT = when {
        requireMinQQVersion(QQVersion.QQ_8_9_90) -> "smi"
        requireMinQQVersion(QQVersion.QQ_8_9_88) -> "slx"
        requireMinQQVersion(QQVersion.QQ_8_9_85) -> "sih"
        requireMinQQVersion(QQVersion.QQ_8_9_83) -> "shv"
        requireMinQQVersion(QQVersion.QQ_8_9_80) -> "sg6"
        requireMinQQVersion(QQVersion.QQ_8_9_78) -> "s_8"
        requireMinQQVersion(QQVersion.QQ_8_9_75) -> "s_l"
        requireMinQQVersion(QQVersion.QQ_8_9_73) -> "s8p"
        requireMinQQVersion(QQVersion.QQ_8_9_70) -> "s55"
        requireMinQQVersion(QQVersion.QQ_8_9_68) -> "s3o"
        else -> "rzs"
    }

    private val constraintSetClz by lazy { "androidx.constraintlayout.widget.ConstraintSet".clazz!! }
    private val constraintLayoutClz by lazy { "androidx.constraintlayout.widget.ConstraintLayout".clazz!! }

    private fun shouldShowTailMsgForMsgRecord(chatMessage: MsgRecord): Boolean {
        // do not show tail message for grey tips
        return chatMessage.msgType != MsgConstants.MSG_TYPE_GRAY_TIPS && MessageUtils.isNoSeqMessage(chatMessage.sendStatus)
    }

    override fun onGetView(rootView: ViewGroup, chatMessage: MsgRecordData, param: XC_MethodHook.MethodHookParam) {

    }

    @SuppressLint("ResourceType", "SetTextI18n")
    override fun onGetViewNt(rootView: ViewGroup, chatMessage: MsgRecord, param: XC_MethodHook.MethodHookParam) {
        if (!isEnabled) return

        if (requireMinQQVersion(QQVersion.QQ_8_9_63_BETA_11345) || requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA)) {
            if (!rootView.children.map { it.id }.contains(ID_ADD_LAYOUT)) {
                val layout = LinearLayout(rootView.context).apply {
                    layoutParams = ConstraintLayout.LayoutParams(
                        0 /* MATCH_CONSTRAINT */,
                        WRAP_CONTENT
                    )
                    id = ID_ADD_LAYOUT
                }

                val textView = TextView(rootView.context).apply {
                    id = ID_ADD_TEXTVIEW
                    textSize = 12f
                    layoutParams = LinearLayout.LayoutParams(
                        WRAP_CONTENT,
                        WRAP_CONTENT
                    )
                }
                layout.addView(textView)
                rootView.addView(layout)

                val constraintSet = constraintSetClz.newInstance(args())!!
                constraintSet.invokeMethod("clone", args(rootView), argTypes(constraintLayoutClz))
                val i_msg = rootView.children.indexOfFirst { it is LinearLayout && it.id != View.NO_ID }
                val id_msg = rootView.getChildAt(i_msg).id
                val id_name = rootView.getChildAt(i_msg - 1).id
                constraintSet.invokeMethod(
                    "connect",
                    args(ID_ADD_LAYOUT, ConstraintLayout.LayoutParams.TOP, id_msg, ConstraintLayout.LayoutParams.BOTTOM, 0),
                    argTypes(Int::class.java, Int::class.java, Int::class.java, Int::class.java, Int::class.java)
                )
                if (chatMessage.senderUin != AppRuntimeHelper.getLongAccountUin()) {
                    constraintSet.invokeMethod(
                        "connect",
                        args(ID_ADD_LAYOUT, ConstraintSet.LEFT, id_name, ConstraintSet.LEFT),
                        argTypes(Int::class.java, Int::class.java, Int::class.java, Int::class.java)
                    )
                    if (chatMessage.chatType == 1) {
                        // 调整私聊显示边距
                        constraintSet.invokeMethod(
                            "setMargin",
                            args(ID_ADD_LAYOUT, ConstraintSet.START, XPopupUtils.dp2px(rootView.context, 10f)),
                            argTypes(Int::class.java, Int::class.java, Int::class.java)
                        )
                    } else if (chatMessage.chatType == 2 && chatMessage.msgType == MsgConstants.MSG_TYPE_FILE) {
                        // 调整群聊文件边距
                        constraintSet.invokeMethod(
                            "setMargin",
                            args(ID_ADD_LAYOUT, ConstraintSet.START, XPopupUtils.dp2px(rootView.context, 55f)),
                            argTypes(Int::class.java, Int::class.java, Int::class.java)
                        )
                    }
                } else {
                    constraintSet.invokeMethod(
                        "connect",
                        args(ID_ADD_LAYOUT, ConstraintSet.RIGHT, id_name, ConstraintSet.RIGHT),
                        argTypes(Int::class.java, Int::class.java, Int::class.java, Int::class.java)
                    )
                    if (chatMessage.chatType == 1) {
                        // 调整私聊显示边距
                        constraintSet.invokeMethod(
                            "setMargin",
                            args(ID_ADD_LAYOUT, ConstraintSet.END, XPopupUtils.dp2px(rootView.context, 10f)),
                            argTypes(Int::class.java, Int::class.java, Int::class.java)
                        )
                    }
                }
                constraintSet.invokeMethod("applyTo", args(rootView), argTypes(constraintLayoutClz))
            }

            val layout = rootView.findViewById<LinearLayout>(ID_ADD_LAYOUT)
            val textView = rootView.findViewById<TextView>(ID_ADD_TEXTVIEW)

            if (shouldShowTailMsgForMsgRecord(chatMessage)) {
                layout.visibility = View.VISIBLE
                textView.visibility = View.VISIBLE
                textView.let {
                    it.tag = chatMessage
                    it.text = "这条消息可能未成功发送！"
                }
            } else {
                layout.visibility = View.GONE
                textView.visibility = View.GONE
            }

            return
        }

//        Log.d("rootView: $rootView")
        val tailLayout = try {
            val v = rootView.findHostView<FrameLayout>(NAME_TAIL_LAYOUT)
            if (v == null) {
//                Log.e("ChatItemShowQQUin tailLayout is null")
                // dump root children
//                rootView.children.forEach {
//                    Log.e("[ERR]--> rootView child: $it")
//                }
                return
            } else {
//                rootView.children.forEach {
//                    Log.e("[+++]--> rootView child: $it")
//                }
            }
            v
        } catch (_: Exception) {
            val stub = rootView.findHostView<ViewStub>(NAME_TAIL_LAYOUT)!!
            stub.inflate() as FrameLayout
        }
        // TODO: 2023-11-25 8.9.93 work around 使用和 "群文件" 同一个 FrameLayout
        // 因为先前用的 view 在 8.9.93 只在自己发的消息存在，不是自己发的消息上连 view 都没有
        tailLayout.visibility = View.VISIBLE
        // Log.d("ChatItemShowQQUin tailLayout: $tailLayout, msg: $chatMessage")
        if (!tailLayout.children.map { it.id }.contains(ID_ADD_LAYOUT)) {
            val layout = LinearLayout(rootView.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    MATCH_PARENT,
                    WRAP_CONTENT
                ).apply {
                    marginStart = XPopupUtils.dp2px(rootView.context, 15f)
                    // 因为tailLayout是FrameLayout，所以继承了会和原消息tailMessage重叠的特性
                }
                // 灰色背景不想搞了，弄圆角麻烦
                id = ID_ADD_LAYOUT
            }
            val textView = TextView(rootView.context).apply {
                id = ID_ADD_TEXTVIEW
                textSize = 12f
            }
            layout.addView(textView)
            tailLayout.addView(layout)
        }

        rootView.findViewById<TextView>(ID_ADD_TEXTVIEW).let {
            it.tag = chatMessage
            it.text = "这条消息可能未成功发送！"
        }
    }
}
