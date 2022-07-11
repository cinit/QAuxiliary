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

import android.app.Activity
import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.SwitchCompat
import cc.ioctl.util.LayoutHelper
import cc.ioctl.util.Reflex
import cc.ioctl.util.ui.FaultyDialog
import de.robv.android.xposed.XC_MethodHook
import io.github.qauxv.R
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.bridge.AIOUtilsImpl
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.core.HookInstaller
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.ui.CustomDialog
import io.github.qauxv.util.Toasts
import kotlinx.coroutines.flow.MutableStateFlow
import me.ketal.dispacher.OnBubbleBuilder
import me.singleneuron.data.MsgRecordData
import xyz.nextalone.util.method
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@UiItemAgentEntry
object ChatItemShowQQUin : CommonConfigFunctionHook(), OnBubbleBuilder {

    override val name = "消息显示发送者QQ号和时间"
    override val description = "可能导致聊天界面滑动掉帧"
    override fun initOnce() = isAvailable
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY

    private const val CFG_KEY_CUSTOM_MSG_FORMAT = "ChatItemShowQQUin.CFG_KEY_CUSTOM_MSG_FORMAT"
    private const val CFG_KEY_CUSTOM_TIME_FORMAT = "ChatItemShowQQUin.CFG_KEY_CUSTOM_TIME_FORMAT"
    private const val CFG_KEY_ENABLE_DETAIL_INFO = "ChatItemShowQQUin.CFG_KEY_ENABLE_DETAIL_INFO"
    private const val DEFAULT_MSG_FORMAT = "QQ: \${senderuin} Time: \${formatTime}"
    private const val DEFAULT_TIME_FORMAT = "MM-dd HH:mm:ss"

    override val valueState: MutableStateFlow<String?> by lazy {
        MutableStateFlow(if (isEnabled) "已开启" else "禁用")
    }

    override val onUiItemClickListener: (IUiItemAgent, Activity, View) -> Unit = { _, activity, _ ->
        showConfigDialog(activity)
    }

    private var mCurrentMsgFormat: String
        get() = ConfigManager.getDefaultConfig().getStringOrDefault(CFG_KEY_CUSTOM_MSG_FORMAT, DEFAULT_MSG_FORMAT)
        set(value) {
            ConfigManager.getDefaultConfig().putString(CFG_KEY_CUSTOM_MSG_FORMAT, value)
        }

    private var mCurrentTimeFormat: String
        get() = ConfigManager.getDefaultConfig().getStringOrDefault(CFG_KEY_CUSTOM_TIME_FORMAT, DEFAULT_TIME_FORMAT)
        set(value) {
            ConfigManager.getDefaultConfig().putString(CFG_KEY_CUSTOM_TIME_FORMAT, value)
        }

    private var mEnableDetailInfo: Boolean
        get() = ConfigManager.getDefaultConfig().getBooleanOrDefault(CFG_KEY_ENABLE_DETAIL_INFO, true)
        set(value) {
            ConfigManager.getDefaultConfig().putBoolean(CFG_KEY_ENABLE_DETAIL_INFO, value)
        }

    private fun showConfigDialog(ctx: Context) {
        val timeFormat = mCurrentTimeFormat
        val msgFormat = mCurrentMsgFormat
        val enableDetailInfo = mEnableDetailInfo
        val currEnabled = isEnabled
        val availablePlaceholders: Array<String> = arrayOf(
            "\${senderuin}", "\${frienduin}", "\${msgtype}", "\${readableMsgType}", "\${extraflag}", "\${extStr}",
            "\${formatTime}", "\${time}", "\${msg}", "\${istroop}", "\${issend}", "\${isread}", "\${msgUid}",
            "\${shmsgseq}", "\${uniseq}", "\${simpleName}"
        )
        val funcSwitch = SwitchCompat(ctx).apply {
            isChecked = currEnabled
            textSize = 16f
            text = "总开关(开启后才会生效)"
        }
        val detailInfoSwitch = SwitchCompat(ctx).apply {
            isChecked = enableDetailInfo
            textSize = 16f
            text = "点击显示消息详细信息"
        }
        val tvMsgFmt: EditText = AppCompatEditText(ctx).apply {
            setText(msgFormat)
            textSize = 16f
            setTextColor(ctx.resources.getColor(R.color.firstTextColor, ctx.theme))
            hint = "消息格式 (见可用占位符)"
        }
        val tvTimeFmt: EditText = AppCompatEditText(ctx).apply {
            setText(timeFormat)
            textSize = 16f
            setTextColor(ctx.resources.getColor(R.color.firstTextColor, ctx.theme))
            hint = "时间格式 yyyy-MM-dd HH:mm:ss"
        }
        val tvClickToAppend = AppCompatTextView(ctx).apply {
            setTextColor(ctx.resources.getColor(R.color.firstTextColor, ctx.theme))
            textSize = 14f
            movementMethod = LinkMovementMethod.getInstance()
            text = SpannableStringBuilder("消息格式 可用占位符(点击添加)\n").apply {
                availablePlaceholders.forEach {
                    val startOffset = length
                    append("$it ")
                    val endOffset = length - 1
                    setSpan(
                        object : ClickableSpan() {
                            override fun onClick(widget: View) {
                                val text = it
                                // add text to current cursor position
                                val startp = tvMsgFmt.selectionStart
                                val endp = tvMsgFmt.selectionEnd
                                tvMsgFmt.text.replace(startp, endp, text)
                            }
                        }, startOffset, endOffset, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                val dp8 = LayoutHelper.dip2px(ctx, 8f)
                setMargins(dp8, 0, dp8, 0)
            }
            addView(funcSwitch, lp)
            addView(detailInfoSwitch, lp)
            TextView(ctx).apply {
                text = "消息格式"
                textSize = 12f
                setTextColor(ctx.resources.getColor(R.color.secondTextColor, ctx.theme))
            }.also {
                addView(it, lp)
            }
            addView(tvMsgFmt, lp)
            TextView(ctx).apply {
                text = "时间格式 (如果上面没有用到 \${formatTime} 可以不用设置)"
                textSize = 12f
                setTextColor(ctx.resources.getColor(R.color.secondTextColor, ctx.theme))
            }.also {
                addView(it, lp)
            }
            addView(tvTimeFmt, lp)
            addView(tvClickToAppend, lp)
        }
        AlertDialog.Builder(ctx).apply {
            setTitle("设置自定义格式")
            setView(layout)
            setCancelable(false)
            setPositiveButton("确定") { _, _ ->
                val newEnabled = funcSwitch.isChecked
                if (newEnabled != currEnabled) {
                    isEnabled = newEnabled
                    valueState.value = if (newEnabled) "已开启" else "禁用"
                }
                mEnableDetailInfo = detailInfoSwitch.isChecked
                mCurrentMsgFormat = tvMsgFmt.text.toString()
                mCurrentTimeFormat = tvTimeFmt.text.toString()
                if (!isInitialized && isEnabled) {
                    HookInstaller.initializeHookForeground(ctx, this@ChatItemShowQQUin)
                }
                // invalidate config
                mDataFormatter = null
                Toasts.success(ctx, "已保存")
            }
            setNegativeButton("取消") { _, _ -> }
            show()
        }
    }

    private val mOnTailMessageClickListener = View.OnClickListener {
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

    private var mDataFormatter: SimpleDateFormat? = null

    private fun formatTailMessage(chatMessage: MsgRecordData): String {
        val msgFmt = mCurrentMsgFormat
        val timeFmt = mCurrentTimeFormat
        var formatTime = ""
        if (msgFmt.contains("\${formatTime}")) {
            if (mDataFormatter == null) {
                mDataFormatter = SimpleDateFormat(timeFmt, Locale.ROOT)
            }
            formatTime = mDataFormatter!!.format(Date(chatMessage.time * 1000L))
        }
        val result = msgFmt
            .replace("\${senderuin}", chatMessage.senderUin.toString())
            .replace("\${frienduin}", chatMessage.friendUin.toString())
            .replace("\${msgtype}", chatMessage.msgType.toString())
            .replace("\${readableMsgType}", chatMessage.readableMsgType.toString())
            .replace("\${extraflag}", chatMessage.extraFlag.toString())
            .replace("\${extStr}", chatMessage.extStr.toString())
            .replace("\${formatTime}", formatTime)
            .replace("\${time}", chatMessage.time.toString())
            .replace("\${msg}", chatMessage.msg.toString())
            .replace("\${istroop}", chatMessage.isTroop.toString())
            .replace("\${issend}", chatMessage.isSend.toString())
            .replace("\${isread}", chatMessage.isRead.toString())
            .replace("\${msgUid}", chatMessage.msgUid.toString())
            .replace("\${shmsgseq}", chatMessage.shMsgSeq.toString())
            .replace("\${uniseq}", chatMessage.uniseq.toString())
            .replace("\${simpleName}", chatMessage.msgRecord.javaClass.simpleName)
        return result
    }

    override fun onGetView(rootView: ViewGroup, chatMessage: MsgRecordData, param: XC_MethodHook.MethodHookParam) {
        if (!isEnabled) return
        val text = formatTailMessage(chatMessage)
        if (!::pfnSetTailMessage.isInitialized) {
            pfnSetTailMessage =
                "Lcom/tencent/mobileqq/activity/aio/BaseChatItemLayout;->setTailMessage(ZLjava/lang/CharSequence;Landroid/view/View\$OnClickListener;)V".method
        }
        pfnSetTailMessage.invoke(rootView, true, text, if (mEnableDetailInfo) mOnTailMessageClickListener else null)
    }
}
