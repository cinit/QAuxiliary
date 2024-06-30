/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the qwq233 Universal License
 * as published on https://github.com/qwq233/license; either
 * version 2 of the License, or any later version and our EULA as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the qwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package cc.microblock.hook

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.SwitchCompat
import cc.hicore.QApp.QAppUtils
import cc.ioctl.util.LayoutHelper
import cc.ioctl.util.hookBeforeIfEnabled
import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import io.github.qauxv.R
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.bridge.kernelcompat.ContactCompat
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.dexkit.AIOSendMsg
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import xyz.nextalone.util.method
import xyz.nextalone.util.set
import java.io.Serializable

@FunctionHookEntry
@UiItemAgentEntry
object ImageCustomSummary : CommonConfigFunctionHook("ImageCustomSummary", arrayOf(AIOSendMsg)) {

    override val name = "自定义外显内容"
    override val description = "自定义消息列表中[图片][动画表情]等内容"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY
    override val isAvailable = QAppUtils.isQQnt()

    override val valueState: MutableStateFlow<String?> by lazy {
        MutableStateFlow(if (isEnabled) "已开启" else "禁用")
    }
    override val onUiItemClickListener: (IUiItemAgent, Activity, View) -> Unit = { _, activity, _ ->
        showConfigDialog(activity)
    }

    private var summaryText: String
        get() {
            val cfg = ConfigManager.getDefaultConfig()
            return cfg.getString("customSummary.summaryText") ?: "喵喵喵"
        }
        set(value) {
            val cfg = ConfigManager.getDefaultConfig()
            cfg.putString("customSummary.summaryText", value)
        }
    private var typePic0: Boolean
        get() {
            val cfg = ConfigManager.getDefaultConfig()
            return cfg.getBooleanOrDefault("customSummary.typePic0", true)
        }
        set(value) {
            val cfg = ConfigManager.getDefaultConfig()
            cfg.putBoolean("customSummary.typePic0", value)
        }
    private var typePic1247: Boolean
        get() {
            val cfg = ConfigManager.getDefaultConfig()
            return cfg.getBooleanOrDefault("customSummary.typePic1247", true)
        }
        set(value) {
            val cfg = ConfigManager.getDefaultConfig()
            cfg.putBoolean("customSummary.typePic1247", value)
        }
    private var typeMarketFace: Boolean
        get() {
            val cfg = ConfigManager.getDefaultConfig()
            return cfg.getBooleanOrDefault("customSummary.typeMarketFace", true)
        }
        set(value) {
            val cfg = ConfigManager.getDefaultConfig()
            cfg.putBoolean("customSummary.typeMarketFace", value)
        }

    @SuppressLint("SetTextI18n")
    private fun showConfigDialog(ctx: Context) {
        val switchEnable = SwitchCompat(ctx).apply {
            isChecked = isEnabled
            textSize = 16f
            text = "功能总开关"
        }

        val checkBoxTypePic0 = CheckBox(ctx).apply {
            isChecked = typePic0
            textSize = 16f
            text = "纯图片0/图文混排0"
        }
        val checkBoxTypePic1247 = CheckBox(ctx).apply {
            isChecked = typePic1247
            textSize = 16f
            text = "动画表情1/表情搜索2/表情消息4/表情推荐7"
        }
        val checkBoxTypeMarketFace = CheckBox(ctx).apply {
            isChecked = typeMarketFace
            textSize = 16f
            text = "表情商城"
        }

        val summaryTextLabel = AppCompatTextView(ctx).apply {
            text = "外显内容"
            textSize = 12f
            setTextColor(ctx.resources.getColor(R.color.secondTextColor, ctx.theme))
        }
        val summaryTextEdit: EditText = AppCompatEditText(ctx).apply {
            setText(summaryText)
            textSize = 16f
            setTextColor(ctx.resources.getColor(R.color.firstTextColor, ctx.theme))
            hint = "外显内容"
        }

        // TODO: complete this
        val rangeTextLabel = AppCompatTextView(ctx).apply {
            text = "生效联系人列表（,分割）"
        }
        val rangeTextEdit: EditText = EditText(ctx).apply {
            setText("")
            textSize = 16f
            setTextColor(ctx.resources.getColor(R.color.firstTextColor, ctx.theme))
            hint = "114514, 1919810"
        }

        val rootLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                val dp8 = LayoutHelper.dip2px(ctx, 8f)
                setMargins(dp8, 0, dp8, 0)
            }
            addView(switchEnable, lp)
            addView(checkBoxTypePic0, lp)
            addView(checkBoxTypePic1247, lp)
            addView(checkBoxTypeMarketFace, lp)
            if (!isAvailable) addView(
                AppCompatTextView(ctx).apply {
                    text = "版本不支持：该功能需要 QQNT 版本"
                    setTextColor(0xFFFF0000.toInt())
                }
            )
            addView(summaryTextLabel, lp)
            addView(summaryTextEdit, lp)
        }

        AlertDialog.Builder(ctx).apply {
            setTitle("自定义外显内容")
            setView(rootLayout)
            setPositiveButton("确定") { _, _ ->
                isEnabled = switchEnable.isChecked
                summaryText = summaryTextEdit.text.toString()
                valueState.update { if (isEnabled) "已开启" else "禁用" }
            }
            setNegativeButton("取消") { _, _ -> }
            show()
        }
    }

    override fun initOnce(): Boolean {
        val sendMsgMethod = Initiator.loadClass("com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService\$CppProxy").method("sendMsg")!!
        hookBeforeIfEnabled(sendMsgMethod) { param ->
            val contact = ContactCompat.fromKernelObject(param.args[1] as Serializable)
            val chatType = contact.chatType
            val elements = param.args[2] as ArrayList<*>
            for (element in elements) {
                val msgElement = (element as MsgElement)
                msgElement.picElement?.let { picElement ->
                    val picSubType = picElement.picSubType
                    if (typePic0 && picSubType == 0) {
                        picElement.summary = summaryText
                    }
                    if (typePic1247 && picSubType != 0) {
                        picElement.summary = summaryText
                        if (chatType != 4) { // 不是频道才修改类型, 不然会发送不出去
                            picElement.picSubType = 7
                        }
                    }
                }
                msgElement.marketFaceElement?.let { marketFaceElement ->
                    if (typeMarketFace) {
                        marketFaceElement.set("faceName", summaryText)
                    }
                }
            }
        }
        return true
    }
}