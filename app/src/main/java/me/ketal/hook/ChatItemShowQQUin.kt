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

import android.annotation.SuppressLint
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
import android.view.ViewStub
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.children
import cc.ioctl.hook.msg.FlashPicHook
import cc.ioctl.util.LayoutHelper
import cc.ioctl.util.Reflex
import cc.ioctl.util.ui.FaultyDialog
import com.github.kyuubiran.ezxhelper.utils.argTypes
import com.github.kyuubiran.ezxhelper.utils.args
import com.github.kyuubiran.ezxhelper.utils.invokeMethod
import com.github.kyuubiran.ezxhelper.utils.newInstance
import com.lxj.xpopup.util.XPopupUtils
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import de.robv.android.xposed.XC_MethodHook
import io.github.qauxv.R
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.bridge.AIOUtilsImpl
import io.github.qauxv.bridge.AppRuntimeHelper
import io.github.qauxv.bridge.ntapi.ChatTypeConstants
import io.github.qauxv.bridge.ntapi.MsgConstants
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.core.HookInstaller
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.requireMinQQVersion
import kotlinx.coroutines.flow.MutableStateFlow
import me.ketal.dispacher.BaseBubbleBuilderHook
import me.ketal.dispacher.OnBubbleBuilder
import me.singleneuron.data.MsgRecordData
import xyz.nextalone.util.clazz
import xyz.nextalone.util.findHostView
import xyz.nextalone.util.method
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@UiItemAgentEntry
object ChatItemShowQQUin : CommonConfigFunctionHook(), OnBubbleBuilder {

    override val name = "消息显示 ID 和时间"
    override val description = "可能导致聊天界面滑动掉帧"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY

    override fun initOnce(): Boolean {
        return isAvailable && BaseBubbleBuilderHook.initialize()
    }

    private const val CFG_KEY_CUSTOM_MSG_FORMAT = "ChatItemShowQQUin.CFG_KEY_CUSTOM_MSG_FORMAT"
    private const val CFG_KEY_CUSTOM_TIME_FORMAT = "ChatItemShowQQUin.CFG_KEY_CUSTOM_TIME_FORMAT"
    private const val CFG_KEY_ENABLE_DETAIL_INFO = "ChatItemShowQQUin.CFG_KEY_ENABLE_DETAIL_INFO"
    private const val DEFAULT_MSG_FORMAT = "\${shmsgseq}   \${formatTime}"
    private const val DEFAULT_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss"

    // For NT
    private const val ID_ADD_LAYOUT = 0x114515
    private const val ID_ADD_TEXTVIEW = 0x114516

    // X2J_APT <- ???Binding(com/tx/x2j/AioSenderBubbleTemplateBinding) <- AIOSenderBubbleTemplate
    private val NAME_TAIL_LAYOUT = when {
        requireMinQQVersion(QQVersion.QQ_9_0_8) -> "srn"
        requireMinQQVersion(QQVersion.QQ_8_9_93_BETA_13315) -> "so5"
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

    @SuppressLint("SetTextI18n")
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
            showDetailInfoDialog(it.context, Reflex.getShortClassName(chatMessage.msgRecord), chatMessage.msgRecord.toString())
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
        return msgFmt
            .replace("\${senderuin}", chatMessage.senderUin.toString())
            .replace("\${frienduin}", chatMessage.friendUin.toString())
            .replace("\${msgtype}", chatMessage.msgType.toString())
            .replace("\${readableMsgType}", chatMessage.readableMsgType)
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
    }

    override fun onGetView(rootView: ViewGroup, chatMessage: MsgRecordData, param: XC_MethodHook.MethodHookParam) {
        if (!isEnabled) return
        var text = formatTailMessage(chatMessage)
        if (!::pfnSetTailMessage.isInitialized) {
            pfnSetTailMessage =
                "Lcom/tencent/mobileqq/activity/aio/BaseChatItemLayout;->setTailMessage(ZLjava/lang/CharSequence;Landroid/view/View\$OnClickListener;)V".method
        }
        if (FlashPicHook.INSTANCE.isInitializationSuccessful && isFlashPic(chatMessage)) {
            text = "闪照 $text"
        }
        pfnSetTailMessage.invoke(rootView, true, text, if (mEnableDetailInfo) mOnTailMessageClickListener else null)
    }

    private fun formatTailMessageNt(chatMessage: MsgRecord): String {
        // TODO NT数据类型换血
        val msgFmt = mCurrentMsgFormat
        val timeFmt = mCurrentTimeFormat
        var formatTime = ""
        if (msgFmt.contains("\${formatTime}")) {
            if (mDataFormatter == null) {
                mDataFormatter = SimpleDateFormat(timeFmt, Locale.ROOT)
            }
            formatTime = mDataFormatter!!.format(Date(chatMessage.msgTime * 1000L))
        }
        return msgFmt
            .replace("\${senderuin}", chatMessage.senderUin.toString())
            .replace("\${frienduin}", chatMessage.peerUin.toString())
            .replace("\${msgtype}", chatMessage.msgType.toString())
            .replace("\${readableMsgType}", "")
            .replace("\${extraflag}", "")
            .replace("\${extStr}", "")
            .replace("\${formatTime}", formatTime)
            .replace("\${time}", chatMessage.msgTime.toString())
            .replace("\${msg}", chatMessage.elements.joinToString { it.toString() })
            .replace("\${istroop}", "")
            .replace("\${issend}", chatMessage.sendStatus.toString())
            .replace("\${isread}", "")
            .replace("\${msgUid}", "")
            .replace("\${shmsgseq}", chatMessage.msgSeq.toString())
            .replace("\${uniseq}", "")
            .replace("\${simpleName}", chatMessage.javaClass.simpleName)
    }

    private fun shouldShowTailMsgForMsgRecord(chatMessage: MsgRecord): Boolean {
        // do not show tail message for grey tips
        if (chatMessage.msgType == MsgConstants.MSG_TYPE_GRAY_TIPS) {
            return false
        }
        return true
    }

    @SuppressLint("ResourceType", "SetTextI18n")
    override fun onGetViewNt(rootView: ViewGroup, chatMessage: MsgRecord, param: XC_MethodHook.MethodHookParam) {
        // 因为tailMessage是自己添加的，所以闪照文字也放这里处理
        val isFlashPicTagNeedShow = FlashPicHook.INSTANCE.isInitializationSuccessful && isFlashPicNt(chatMessage)
        if (!isEnabled && !isFlashPicTagNeedShow) return

        if (requireMinQQVersion(QQVersion.QQ_9_0_15)) {
            if (!rootView.children.map { it.id }.contains(ID_ADD_LAYOUT)) {
                val layout = LinearLayout(rootView.context).apply {
                    layoutParams = ConstraintLayout.LayoutParams(
                        0 /* MATCH_CONSTRAINT */,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    )
                    id = ID_ADD_LAYOUT
                }

                val textView = TextView(rootView.context).apply {
                    id = ID_ADD_TEXTVIEW
                    textSize = 12f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setOnClickListener {
                        if (!mEnableDetailInfo) return@setOnClickListener
                        val msgRecord = it.tag as MsgRecord
                        showDetailInfoDialog(rootView.context, Reflex.getShortClassName(msgRecord), msgRecord.toString())
                    }
                }
                layout.addView(textView)
                rootView.addView(layout)
                val constraintSet = constraintSetClz.newInstance(args())!!
                constraintSet.invokeMethod("clone", args(rootView), argTypes(constraintLayoutClz))
                val id_msg = rootView.children.find { it is LinearLayout && it.id != View.NO_ID }!!.id
                constraintSet.invokeMethod(
                    "connect",
                    args(ID_ADD_LAYOUT, ConstraintLayout.LayoutParams.TOP, id_msg, ConstraintLayout.LayoutParams.BOTTOM, 0),
                    argTypes(Int::class.java, Int::class.java, Int::class.java, Int::class.java, Int::class.java)
                )
                if (chatMessage.senderUin != AppRuntimeHelper.getLongAccountUin()) {
                    constraintSet.invokeMethod(
                        "connect",
                        args(ID_ADD_LAYOUT, ConstraintSet.LEFT, id_msg, ConstraintSet.LEFT),
                        argTypes(Int::class.java, Int::class.java, Int::class.java, Int::class.java)
                    )
                } else {
                    constraintSet.invokeMethod(
                        "connect",
                        args(ID_ADD_LAYOUT, ConstraintSet.RIGHT, id_msg, ConstraintSet.RIGHT),
                        argTypes(Int::class.java, Int::class.java, Int::class.java, Int::class.java)
                    )
                }
                constraintSet.invokeMethod("applyTo", args(rootView), argTypes(constraintLayoutClz))
            }

            val textView = rootView.findViewById<TextView>(ID_ADD_TEXTVIEW)

            if (isFlashPicTagNeedShow || shouldShowTailMsgForMsgRecord(chatMessage)) {
                textView.visibility = View.VISIBLE
                textView.let {
                    it.tag = chatMessage
                    it.text = (if (isFlashPicTagNeedShow) "闪照 " else "") + (if (isEnabled) formatTailMessageNt(chatMessage) else "")
                }
            } else {
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
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
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
                setOnClickListener {
                    // 或者不用tag，像上面mOnTailMessageClickListener一样通过view获取message
                    // Dialog细节没有考虑，MsgRecord里面的冗余内容很多，可考虑格式化/选择性展示
                    if (!mEnableDetailInfo) return@setOnClickListener
                    val msgRecord = it.tag as MsgRecord
                    showDetailInfoDialog(rootView.context, Reflex.getShortClassName(msgRecord), msgRecord.toString())
                }
            }
            layout.addView(textView)
            tailLayout.addView(layout)
        }

        rootView.findViewById<TextView>(ID_ADD_TEXTVIEW).let {
            it.tag = chatMessage
            it.text = (if (isFlashPicTagNeedShow) "闪照 " else "") + (if (isEnabled) formatTailMessageNt(chatMessage) else "")
        }
    }

    private fun isFlashPic(chatMessage: MsgRecordData): Boolean {
        val msgtype = chatMessage.msgType
        return (msgtype == -2000 || msgtype == -2006) &&
            chatMessage.getExtInfoFromExtStr("commen_flash_pic").isNotEmpty()
    }

    private fun isFlashPicNt(chatMessage: MsgRecord): Boolean {
        return chatMessage.javaClass.getDeclaredField("subMsgType").run {
            isAccessible = true
            val subMsgType = getInt(chatMessage)
            subMsgType == 8194 || subMsgType == 12288
        }
    }

    private fun showDetailInfoDialog(context: Context, title: String, msg: String) {
        val ctx = CommonContextWrapper.createAppCompatContext(context)
        val text = AppCompatTextView(ctx).apply {
            text = msg
            textSize = 16f
            setTextIsSelectable(true)
            isVerticalScrollBarEnabled = true
            setTextColor(ctx.resources.getColor(R.color.firstTextColor, ctx.theme))
            val dp24 = LayoutHelper.dip2px(ctx, 24f)
            setPadding(dp24, 0, dp24, 0)
        }
        AlertDialog.Builder(ctx)
            .setTitle(title)
            .setView(text)
//            .setMessage(msg)
            .setCancelable(true)
            .setPositiveButton(android.R.string.ok, null)
            .show()
//            .apply {
//                findViewById<TextView>(android.R.id.message)?.setTextIsSelectable(true)
//            }
    }
}
