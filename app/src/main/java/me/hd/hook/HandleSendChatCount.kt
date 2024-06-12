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

import android.app.Activity
import android.content.Context
import android.graphics.Color
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
import androidx.core.view.children
import cc.ioctl.util.LayoutHelper
import cc.ioctl.util.hookAfterIfEnabled
import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import io.github.qauxv.R
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.config.ConfigManager.getExFriendCfg
import io.github.qauxv.core.HookInstaller
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.NQQSettingMe_onResume
import io.github.qauxv.util.requireMinQQVersion
import kotlinx.coroutines.flow.MutableStateFlow
import me.hd.util.getExCfg
import me.hd.util.putExCfg
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.isStatic
import xyz.nextalone.util.putExFriend
import xyz.nextalone.util.today
import java.util.Date

@FunctionHookEntry
@UiItemAgentEntry
object HandleSendChatCount : CommonConfigFunctionHook(
    targets = arrayOf(NQQSettingMe_onResume)
) {

    override val name = "统计聊天发送消息数量"
    override val description = "替换旧样式侧滑栏个性签名, 支持 8.9.88 及以上"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override val valueState: MutableStateFlow<String?> by lazy {
        MutableStateFlow(if (isEnabled) "已开启" else "禁用")
    }
    override val onUiItemClickListener: (IUiItemAgent, Activity, View) -> Unit = { _, activity, _ ->
        showConfigDialog(activity)
    }

    private const val ERROR_MESSAGE = "未登录或无法获取当前账号信息"
    private const val CFG_KEY_SHOW_FORMAT = "ChatCountEntry.CFG_KEY_SHOW_FORMAT"
    private const val DEFAULT_SHOW_FORMAT = "Today Send: Text(\${text}) TextWord(\${textWord}) Pic(\${pic})"
    private const val CFG_KEY_COLOR = "ChatCountEntry.CFG_KEY_COLOR"
    private const val DEFAULT_COLOR_RED = "#FFFF0000"
    private const val CFG_KEY_DAY = "ChatCount.CFG_KEY_DAY"
    private const val DEFAULT_STR_EMPTY = ""
    private const val DEFAULT_INT_ZERO = 0
    private const val CFG_KEY_TEXT = "ChatCount.CFG_KEY_TEXT"
    private const val CFG_KEY_TEXT_WORD = "ChatCount.CFG_KEY_TEXT_WORD"
    private const val CFG_KEY_PIC = "ChatCount.CFG_KEY_PIC"
    private const val CFG_KEY_VIDEO = "ChatCount.CFG_KEY_VIDEO"
    private const val CFG_KEY_PTT = "ChatCount.CFG_KEY_PTT"
    private const val CFG_KEY_FACE = "ChatCount.CFG_KEY_FACE"
    private const val CFG_KEY_WALLET = "ChatCount.CFG_KEY_WALLET"
    private const val CFG_KEY_FILE = "ChatCount.CFG_KEY_FILE"

    private var mShowFormat: String
        get() = getExCfg(CFG_KEY_SHOW_FORMAT, DEFAULT_SHOW_FORMAT) as String
        set(value) {
            putExFriend(CFG_KEY_SHOW_FORMAT, value)
        }
    private var mColorValue: String
        get() = getExCfg(CFG_KEY_COLOR, DEFAULT_COLOR_RED) as String
        set(value) {
            putExCfg(CFG_KEY_COLOR, value)
        }
    private var mDay: String
        get() = getExCfg(CFG_KEY_DAY, DEFAULT_STR_EMPTY) as String
        set(value) {
            putExCfg(CFG_KEY_DAY, value)
        }
    private var mText: Int
        get() = getExCfg(CFG_KEY_TEXT, DEFAULT_INT_ZERO) as Int
        set(value) {
            putExCfg(CFG_KEY_TEXT, value)
        }
    private var mTextWord: Int
        get() = getExCfg(CFG_KEY_TEXT_WORD, DEFAULT_INT_ZERO) as Int
        set(value) {
            putExCfg(CFG_KEY_TEXT_WORD, value)
        }
    private var mPtt: Int
        get() = getExCfg(CFG_KEY_PTT, DEFAULT_INT_ZERO) as Int
        set(value) {
            putExCfg(CFG_KEY_PTT, value)
        }
    private var mPic: Int
        get() = getExCfg(CFG_KEY_PIC, DEFAULT_INT_ZERO) as Int
        set(value) {
            putExCfg(CFG_KEY_PIC, value)
        }
    private var mFace: Int
        get() = getExCfg(CFG_KEY_FACE, DEFAULT_INT_ZERO) as Int
        set(value) {
            putExCfg(CFG_KEY_FACE, value)
        }
    private var mVideo: Int
        get() = getExCfg(CFG_KEY_VIDEO, DEFAULT_INT_ZERO) as Int
        set(value) {
            putExCfg(CFG_KEY_VIDEO, value)
        }
    private var mWallet: Int
        get() = getExCfg(CFG_KEY_WALLET, DEFAULT_INT_ZERO) as Int
        set(value) {
            putExCfg(CFG_KEY_WALLET, value)
        }
    private var mFile: Int
        get() = getExCfg(CFG_KEY_FILE, DEFAULT_INT_ZERO) as Int
        set(value) {
            putExCfg(CFG_KEY_FILE, value)
        }

    private fun showConfigDialog(ctx: Context) {
        val showFormat = mShowFormat
        val colorValue = mColorValue
        val currEnabled = isEnabled
        val availablePlaceholders: Array<String> = arrayOf(
            "\${text}", "\${textWord}",
            "\${ptt}",
            "\${pic}", "\${face}",
            "\${video}",
            "\${wallet}",
            "\${file}",
        )
        val funcSwitch = SwitchCompat(ctx).apply {
            isChecked = currEnabled
            textSize = 16f
            text = "功能开关 (开启后才生效)"
        }
        val tvShowFmt: EditText = AppCompatEditText(ctx).apply {
            setText(showFormat)
            textSize = 16f
            setTextColor(ctx.resources.getColor(R.color.firstTextColor, ctx.theme))
            hint = "显示格式 (见可用占位符)"
        }
        val tvColor: EditText = AppCompatEditText(ctx).apply {
            setText(colorValue)
            textSize = 16f
            setTextColor(ctx.resources.getColor(R.color.firstTextColor, ctx.theme))
            hint = "颜色配置 (#FFFF0000)"
        }
        val tvClickToAppend = AppCompatTextView(ctx).apply {
            setTextColor(ctx.resources.getColor(R.color.firstTextColor, ctx.theme))
            textSize = 14f
            movementMethod = LinkMovementMethod.getInstance()
            text = SpannableStringBuilder("显示格式 可用占位符(点击添加)\n").apply {
                availablePlaceholders.forEach {
                    val startOffset = length
                    append("$it ")
                    val endOffset = length - 1
                    setSpan(
                        object : ClickableSpan() {
                            override fun onClick(widget: View) {
                                val text = it
                                val st = tvShowFmt.selectionStart
                                val en = tvShowFmt.selectionEnd
                                tvShowFmt.text.replace(st, en, text)
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
            TextView(ctx).apply {
                text = "显示格式"
                textSize = 12f
                setTextColor(ctx.resources.getColor(R.color.secondTextColor, ctx.theme))
            }.also {
                addView(it, lp)
            }
            addView(tvShowFmt, lp)
            TextView(ctx).apply {
                text = "颜色配置"
                textSize = 12f
                setTextColor(ctx.resources.getColor(R.color.secondTextColor, ctx.theme))
            }.also {
                addView(it, lp)
            }
            addView(tvColor, lp)
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
                mShowFormat = tvShowFmt.text.toString()
                mColorValue = tvColor.text.toString()
                if (!isInitialized && isEnabled) {
                    HookInstaller.initializeHookForeground(ctx, this@HandleSendChatCount)
                }
                Toasts.success(ctx, "已保存")
            }
            setNegativeButton("取消") { _, _ -> }
            show()
        }
    }

    private fun getChatWords(): String {
        return getExFriendCfg()?.let {
            val isToday = Date().today == mDay
            mShowFormat
                .replace("\${text}", (if (isToday) mText else DEFAULT_INT_ZERO).toString())
                .replace("\${textWord}", (if (isToday) mTextWord else DEFAULT_INT_ZERO).toString())
                .replace("\${ptt}", (if (isToday) mPtt else DEFAULT_INT_ZERO).toString())
                .replace("\${pic}", (if (isToday) mPic else DEFAULT_INT_ZERO).toString())
                .replace("\${face}", (if (isToday) mFace else DEFAULT_INT_ZERO).toString())
                .replace("\${video}", (if (isToday) mVideo else DEFAULT_INT_ZERO).toString())
                .replace("\${wallet}", (if (isToday) mWallet else DEFAULT_INT_ZERO).toString())
                .replace("\${file}", (if (isToday) mFile else DEFAULT_INT_ZERO).toString())
        } ?: ERROR_MESSAGE
    }

    private fun updateView(viewGroup: ViewGroup) {
        var textView: TextView? = viewGroup.findViewById(R.id.chat_words_count)
        if (textView == null) {
            viewGroup.apply {
                children.forEach { childView -> childView.alpha = 0.0f }
                addView(
                    TextView(viewGroup.context).apply {
                        id = R.id.chat_words_count
                        textSize = 14.0f
                        setTextColor(Color.parseColor(mColorValue))
                    }
                )
            }
            textView = viewGroup.findViewById(R.id.chat_words_count)
        }
        textView!!.text = getChatWords()
    }

    override fun initOnce(): Boolean {
        val settingOldStyleClass = if (requireMinQQVersion(QQVersion.QQ_9_0_0)) {
            Initiator.loadClass("com.tencent.mobileqq.QQSettingMeView")
        } else {
            Initiator.loadClass("com.tencent.mobileqq.activity.QQSettingMeView")
        }
        /*
        val settingNewStyleClass = if (requireMinQQVersion(QQVersion.QQ_9_0_0)) {
            Initiator.loadClass("com.tencent.mobileqq.bizParts.QQSettingMeProfileCardPart")
        } else {
            Initiator.loadClass("com.tencent.mobileqq.activity.qqsettingme.bizParts.QQSettingMeProfileCardPart")
        }
        */
        val getOldStyleViewMethod = settingOldStyleClass.declaredMethods.single { method ->
            val params = method.parameterTypes
            method.isStatic && method.returnType == View::class.java && params.size == 1 && params[0] == settingOldStyleClass
        }
        var viewGroup: ViewGroup? = null
        hookAfterIfEnabled(getOldStyleViewMethod) { param ->
            viewGroup = param.result as ViewGroup
        }
        DexKit.loadMethodFromCache(NQQSettingMe_onResume)?.hookAfter(this) {
            viewGroup?.post { updateView(viewGroup!!) }
        }
        XposedHelpers.findAndHookMethod(
            Initiator.loadClass("com.tencent.qqnt.kernel.api.impl.MsgService"),
            "sendMsg",
            Long::class.java,
            if (requireMinQQVersion(QQVersion.QQ_9_0_68)) {
                Initiator.loadClass("com.tencent.qqnt.kernelpublic.nativeinterface.Contact")
            } else {
                Initiator.loadClass("com.tencent.qqnt.kernel.nativeinterface.Contact")
            },
            ArrayList::class.java,
            HashMap::class.java,
            Initiator.loadClass("com.tencent.qqnt.kernel.nativeinterface.IOperateCallback"),
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val msgElements = param.args[2] as ArrayList<*>
                    val msgElement = msgElements[0] as MsgElement
                    val isToday = Date().today == mDay
                    if (isToday) {
                        if (msgElement.textElement != null) {
                            mText += 1
                            mTextWord += msgElement.textElement.content.length
                        }
                        if (msgElement.pttElement != null) mPtt += 1
                        if (msgElement.picElement != null) mPic += 1
                        if (msgElement.faceElement != null) mFace += 1
                        if (msgElement.videoElement != null) mVideo += 1
                        if (msgElement.walletElement != null) mWallet += 1
                        if (msgElement.fileElement != null) mFile += 1
                    } else {
                        mDay = Date().today
                        mText = 0
                        mTextWord = 0
                        mPtt = 0
                        mPic = 0
                        mFace = 0
                        mVideo = 0
                        mWallet = 0
                        mFile = 0
                    }
                }
            }
        )
        return true
    }
}