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
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.children
import cc.ioctl.util.LayoutHelper
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
import xyz.nextalone.util.findHostView
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.hookBeforeAllConstructors
import xyz.nextalone.util.putExFriend
import xyz.nextalone.util.today
import java.util.Date

@FunctionHookEntry
@UiItemAgentEntry
object HandleChatCount : CommonConfigFunctionHook(
    targets = arrayOf(NQQSettingMe_onResume)
) {

    override val name = "统计聊天数量"
    override val description = "替换侧滑栏个性签名, 用于统计今日发送消息数量"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override val valueState: MutableStateFlow<String?> by lazy {
        MutableStateFlow(if (isEnabled) "已开启" else "禁用")
    }
    override val onUiItemClickListener: (IUiItemAgent, Activity, View) -> Unit = { _, activity, _ ->
        showConfigDialog(activity)
    }

    private val LAYOUT_NAME = when {
        requireMinQQVersion(QQVersion.QQ_9_0_56) -> "oby"
        requireMinQQVersion(QQVersion.QQ_9_0_50) -> "obo"
        requireMinQQVersion(QQVersion.QQ_9_0_35) -> "obd"
        requireMinQQVersion(QQVersion.QQ_9_0_30) -> "oa3"
        requireMinQQVersion(QQVersion.QQ_9_0_25) -> "o_x"
        requireMinQQVersion(QQVersion.QQ_9_0_17) -> "o_5"
        requireMinQQVersion(QQVersion.QQ_9_0_15) -> "o_5"
        requireMinQQVersion(QQVersion.QQ_9_0_8) -> "o9g"
        requireMinQQVersion(QQVersion.QQ_9_0_0) -> "o95"
        requireMinQQVersion(QQVersion.QQ_8_9_88) -> "o7l"
        else -> "Unknown"
    }

    private const val ERROR_MESSAGE = "未登录或无法获取当前账号信息"
    private const val CFG_KEY_SHOW_FORMAT = "ChatCountEntry.CFG_KEY_SHOW_FORMAT"
    private const val DEFAULT_SHOW_FORMAT = "Today Send: Text(\${text}) Pic(\${pic})"
    private const val CFG_KEY_COLOR_VALUE = "ChatCountEntry.CFG_KEY_COLOR_VALUE"
    private const val DEFAULT_COLOR_VALUE = "#FFFF0000"
    private const val CFG_KEY_DAY = "ChatCountEntry.CFG_KEY_DAY"
    private const val DEFAULT_DAY = ""
    private const val CFG_KEY_TEXT = "ChatCountEntry.CFG_KEY_TEXT"
    private const val DEFAULT_TEXT = 0
    private const val CFG_KEY_PIC = "ChatCountEntry.CFG_KEY_PIC"
    private const val DEFAULT_PIC = 0

    private var mShowFormat: String
        get() = getExFriendCfg()?.getStringOrDefault(CFG_KEY_SHOW_FORMAT, DEFAULT_SHOW_FORMAT) ?: DEFAULT_SHOW_FORMAT
        set(value) {
            putExFriend(CFG_KEY_SHOW_FORMAT, value)
        }
    private var mColorValue: String
        get() = getExFriendCfg()?.getStringOrDefault(CFG_KEY_COLOR_VALUE, DEFAULT_COLOR_VALUE) ?: DEFAULT_COLOR_VALUE
        set(value) {
            putExFriend(CFG_KEY_COLOR_VALUE, value)
        }
    private var mDay: String
        get() = getExFriendCfg()?.getStringOrDefault(CFG_KEY_DAY, DEFAULT_DAY) ?: DEFAULT_DAY
        set(value) {
            putExFriend(CFG_KEY_DAY, value)
        }
    private var mText: Int
        get() = getExFriendCfg()?.getIntOrDefault(CFG_KEY_TEXT, DEFAULT_TEXT) ?: DEFAULT_TEXT
        set(value) {
            putExFriend(CFG_KEY_TEXT, value)
        }
    private var mPic: Int
        get() = getExFriendCfg()?.getIntOrDefault(CFG_KEY_PIC, DEFAULT_PIC) ?: DEFAULT_PIC
        set(value) {
            putExFriend(CFG_KEY_PIC, value)
        }

    private fun showConfigDialog(ctx: Context) {
        val showFormat = mShowFormat
        val colorValue = mColorValue
        val currEnabled = isEnabled
        val availablePlaceholders: Array<String> = arrayOf(
            "\${text}", "\${pic}"
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
                if (!isInitialized && isEnabled) {
                    HookInstaller.initializeHookForeground(ctx, this@HandleChatCount)
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
            val text = if (isToday) mText else 0
            val pic = if (isToday) mPic else 0
            mShowFormat
                .replace("\${text}", text.toString())
                .replace("\${pic}", pic.toString())
        } ?: ERROR_MESSAGE
    }

    private fun updateView(viewGroup: ViewGroup) {
        val relativeLayout = viewGroup.findHostView<RelativeLayout>(LAYOUT_NAME)!!
        var textView: TextView? = relativeLayout.findViewById(R.id.chat_words_count)
        if (textView == null) {
            relativeLayout.apply {
                children.forEach { childView ->
                    childView.alpha = 0.0f
                }
                addView(TextView(viewGroup.context).apply {
                    id = R.id.chat_words_count
                    textSize = 12.0f
                    setTextColor(Color.parseColor(mColorValue))
                })
            }
            textView = relativeLayout.findViewById(R.id.chat_words_count)
        }
        textView!!.text = getChatWords()
    }

    override fun initOnce(): Boolean {
        lateinit var viewGroup: ViewGroup
        val settingClass = if (requireMinQQVersion(QQVersion.QQ_9_0_0)) {
            Initiator.loadClass("com.tencent.mobileqq.QQSettingMeView")
        } else {
            Initiator.loadClass("com.tencent.mobileqq.activity.QQSettingMeView")
        }
        settingClass.hookBeforeAllConstructors {
            viewGroup = it.args[1] as ViewGroup
            updateView(viewGroup)
        }
        DexKit.loadMethodFromCache(NQQSettingMe_onResume)?.hookAfter(this) {
            updateView(viewGroup)
        }
        XposedHelpers.findAndHookMethod(
            Initiator.loadClass("com.tencent.qqnt.kernel.api.impl.MsgService"),
            "sendMsg",
            Long::class.java,
            Initiator.loadClass("com.tencent.qqnt.kernel.nativeinterface.Contact"),
            ArrayList::class.java,
            HashMap::class.java,
            Initiator.loadClass("com.tencent.qqnt.kernel.nativeinterface.IOperateCallback"),
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val msgElements = param.args[2] as ArrayList<*>
                    val msgElement = msgElements[0] as MsgElement
                    if (msgElement.textElement != null) {
                        val isToday = Date().today == mDay
                        if (isToday) {
                            mText += 1
                        } else {
                            mDay = Date().today
                            mText = 0
                            mPic = 0
                        }
                    }
                    if (msgElement.picElement != null) {
                        val isToday = Date().today == mDay
                        if (isToday) {
                            mPic += 1
                        } else {
                            mDay = Date().today
                            mText = 0
                            mPic = 0
                        }
                    }
                }
            }
        )
        return true
    }
}