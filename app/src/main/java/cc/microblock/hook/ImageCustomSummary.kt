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
import android.content.DialogInterface
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import cc.hicore.QApp.QAppUtils
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import io.github.qauxv.R
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.util.dexkit.AIOSendMsg
import io.github.qauxv.util.dexkit.DexKit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import xyz.nextalone.util.get
import xyz.nextalone.util.set

@FunctionHookEntry
@UiItemAgentEntry
object ImageCustomSummary : CommonConfigFunctionHook("ImageCustomSummary", arrayOf(AIOSendMsg)) {
    override val name = "图片/表情包附加自定义概要"

    override val description = "就是改那个在消息概要里显示的"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY

    override fun initOnce(): Boolean {
        DexKit.requireMethodFromCache(AIOSendMsg).hookBefore {
            for (element in (it.args[0] as List<*>)) {
                if (element.get("d") != null) {
                    val picElement = element.get("d")
                    picElement?.set("e", summaryText)
                    picElement?.set("d", 0) // subType
                }
            }
        }
        return true
    }

    override val valueState: MutableStateFlow<String?> by lazy {
        MutableStateFlow(if (isEnabled) "已开启" else "禁用")
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

    @SuppressLint("SetTextI18n")
    override val onUiItemClickListener: (IUiItemAgent, Activity, View) -> Unit = { _, ctx, _ ->
        val builder = AlertDialog.Builder(ctx)
        val root = LinearLayout(ctx)
        root.orientation = LinearLayout.VERTICAL

        val enable = CheckBox(ctx)
        enable.text = "启用自定义概要"
        enable.isChecked = isEnabled


        val summaryTextLabel = AppCompatTextView(ctx).apply {
            text = "文本内容"
        }

        val summaryTextEdit: EditText = AppCompatEditText(ctx).apply {
            setText(summaryText)
            textSize = 16f
            setTextColor(ctx.resources.getColor(R.color.firstTextColor, ctx.theme))
            hint = "文本内容"
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

        root.apply {
            addView(enable)
            if (!isAvailable) addView(
                AppCompatTextView(ctx).apply {
                    text = "版本不支持：该功能需要 QQNT 版本"
                    setTextColor(0xFFFF0000.toInt())
                }
            )
            addView(summaryTextLabel)
            addView(summaryTextEdit)
        }

        builder.setView(root)
            .setTitle("自定义图片概要设置")
            .setPositiveButton("确定") { _: DialogInterface?, _: Int ->
                this.isEnabled = enable.isChecked
                this.summaryText = summaryTextEdit.text.toString()

                valueState.update { if (isEnabled) "已开启" else "禁用" }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override val isAvailable = QAppUtils.isQQnt()
}
