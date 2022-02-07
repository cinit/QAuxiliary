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
package me.kyuubiran.dialog

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import cc.ioctl.util.HostStyledViewBuilder
import cc.ioctl.util.LayoutHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.qauxv.R
import io.github.qauxv.ui.CustomDialog
import io.github.qauxv.util.LicenseStatus
import io.github.qauxv.util.Toasts
import me.kyuubiran.util.AutoRenewFireMgr
import me.kyuubiran.util.getDefaultCfg
import me.kyuubiran.util.getExFriendCfg
import me.kyuubiran.util.showToastByTencent
import java.util.regex.Pattern

object AutoRenewFireDialog {
    private var currentEnable: Boolean? = null
    private var currentEnableAuto = getDefaultCfg().getBooleanOrFalse(AutoRenewFireMgr.AUTO)
    private val allowMsg = arrayListOf("早安", "早", "晚安", "安", "续火", "🔥")

    var replyMsg: String = getExFriendCfg()!!.getStringOrDefault(AutoRenewFireMgr.MESSAGE, "续火")
    var replyTime: String = getExFriendCfg()!!.getStringOrDefault(AutoRenewFireMgr.TIMEPRESET, "")

    fun showMainDialog(context: Context) {
        val dialog = CustomDialog.createFailsafe(context)
        val ctx = dialog.context
        val enable = getExFriendCfg()!!.getBooleanOrFalse(AutoRenewFireMgr.ENABLE)
        currentEnable = enable
        val msgEditText = EditText(ctx)
        msgEditText.textSize = 16f
        msgEditText.hint = "消息内容"
        val _5 = LayoutHelper.dip2px(context, 5f)
        msgEditText.setPadding(_5, _5, _5, _5 * 2)
        msgEditText.setText(replyMsg)
        msgEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) =
                Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                replyMsg = s.toString()
            }

            override fun afterTextChanged(s: Editable?) =
                Unit
        })
        msgEditText.isVisible = !currentEnableAuto
        val timeEditText = EditText(ctx)
        timeEditText.textSize = 16f
        timeEditText.hint = "续火时间，默认 00:00:05，格式 HH:MM:SS"
        timeEditText.setPadding(_5, _5, _5, _5 * 2)
        timeEditText.setText(replyTime)
        timeEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (replyTime.isNotEmpty() && !stringTimeValidator(replyTime)) {
                    timeEditText.error = "时间格式错误"
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                replyTime = s.toString().replace("：", ":")
                if (replyTime.isNotEmpty() && !stringTimeValidator(replyTime)) {
                    timeEditText.error = "时间格式错误"
                }
            }

            override fun afterTextChanged(s: Editable?) =
                Unit
        })
        val checkBox = CheckBox(ctx)
        checkBox.text = "开启自动续火"
        checkBox.isChecked = enable
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            currentEnable = isChecked
            when (isChecked) {
                true -> Toasts.showToast(ctx, Toasts.TYPE_INFO, "已开启自动续火", Toasts.LENGTH_SHORT)
                false -> Toasts.showToast(ctx, Toasts.TYPE_INFO, "已关闭自动续火", Toasts.LENGTH_SHORT)
            }
        }
        val checkBoxAuto = CheckBox(ctx)
        checkBoxAuto.text = "每日一言"
        checkBoxAuto.isChecked = currentEnableAuto
        checkBoxAuto.setOnCheckedChangeListener { _, isChecked ->
            currentEnableAuto = isChecked
            msgEditText.isVisible = !currentEnableAuto
            getDefaultCfg().putBoolean(AutoRenewFireMgr.AUTO, currentEnableAuto)
        }
        val params = LayoutHelper.newLinearLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            _5 * 2
        )
        val linearLayout = LinearLayout(ctx)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.addView(
            HostStyledViewBuilder.subtitle(
                context,
                "说明:启用后将会在每天预设时间之后给对方发一条消息。\n此处开关为总开关，请单独在好友的设置页面打开自动续火开关。\n无论你是否给TA发过消息，本功能都会发送续火消息。\n如果你在续火消息发送前添加了好友，那么之后将会发送给这个好友。\n如果今天已经发送过续火消息了，则再添加好友并不会发送续火消息。"
            )
        )
        linearLayout.addView(
            HostStyledViewBuilder.subtitle(
                context,
                "允许的续火消息:${allowMsg.joinToString(",")}",
                Color.RED
            )
        )
        linearLayout.addView(checkBox, params)
        linearLayout.addView(checkBoxAuto, params)
        linearLayout.addView(msgEditText, params)
        linearLayout.addView(timeEditText, params)
        val alertDialog = dialog.setTitle("自动续火设置")
            .setView(linearLayout)
            .setCancelable(true)
            .setPositiveButton("确认") { _, _ ->
            }.setNeutralButton("使用默认值") { _, _ ->
                replyMsg = "[续火]"
                replyTime = ""
                save()
            }
            .setNegativeButton("取消", null)
            .create() as AlertDialog
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (replyMsg == "") {
                Toasts.showToast(context, Toasts.TYPE_ERROR, "请输入自动续火内容", Toast.LENGTH_SHORT)
            } else {
                if (stringTimeValidator(replyTime) && msgValidator(replyMsg)) {
                    save()
                    Toasts.showToast(context, Toasts.TYPE_INFO, "设置已保存", Toast.LENGTH_SHORT)
                    alertDialog.cancel()
                } else if (!stringTimeValidator(replyTime)) {
                    replyTime = ""
                    Toasts.showToast(context, Toasts.TYPE_ERROR, "时间格式错误", Toast.LENGTH_SHORT)
                } else {
                    replyMsg = "续火"
                    Toasts.showToast(
                        context,
                        Toasts.TYPE_ERROR,
                        "非允许的续火消息",
                        Toast.LENGTH_SHORT
                    )
                }
            }
        }
    }

    fun showSettingsDialog(ctx: Context) {
        MaterialAlertDialogBuilder(ctx, R.style.MaterialDialog)
            .setTitle("自动续火设置")
            .setItems(arrayOf("重置列表", "重置时间")) { _, i ->
                when (i) {
                    0 -> {
                        AutoRenewFireMgr.resetList()
                        ctx.showToastByTencent("已清空自动续火列表")
                    }
                    1 -> {
                        AutoRenewFireMgr.resetTime()
                        ctx.showToastByTencent("已重置下次续火时间")
                    }
                }
            }
            .create()
            .show()
    }

    fun showSetMsgDialog(context: Context, uin: String?) {
        if (uin == null || uin.isEmpty() || !AutoRenewFireMgr.hasEnabled(uin)) return
        val dialog = CustomDialog.createFailsafe(context)
        val ctx = dialog.context
        val editText = EditText(ctx)
        editText.textSize = 16f
        editText.hint = "续火消息内容"
        val _5 = LayoutHelper.dip2px(context, 5f)
        editText.setPadding(_5, _5, _5, _5 * 2)
        editText.setText(AutoRenewFireMgr.getMsg(uin))
        val linearLayout = LinearLayout(ctx)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.addView(
            editText,
            LayoutHelper.newLinearLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                _5 * 2
            )
        )
        val alertDialog = dialog.setTitle("设置单独续火消息")
            .setView(linearLayout)
            .setCancelable(true)
            .setPositiveButton("确认") { _, _ ->
                Toasts.showToast(context, Toasts.TYPE_INFO, "设置自动续火消息成功", Toast.LENGTH_SHORT)
                dialog.dismiss()
                AutoRenewFireMgr.setMsg(uin, editText.text.toString())
            }.setNeutralButton("使用默认值") { _, _ ->
                AutoRenewFireMgr.setMsg(uin, "")
                Toasts.showToast(context, Toasts.TYPE_ERROR, "已使用默认值", Toast.LENGTH_SHORT)
            }
            .setNegativeButton("取消", null)
            .create() as AlertDialog
        alertDialog.show()
    }

    fun save() {
        val cfg = getExFriendCfg()!!
        currentEnable?.let { cfg.putBoolean(AutoRenewFireMgr.ENABLE, it) }
        cfg.putString(AutoRenewFireMgr.MESSAGE, replyMsg)
        cfg.putString(
            AutoRenewFireMgr.TIMEPRESET,
            if (replyTime.isNotEmpty()) replyTime else "00:00:05"
        )
        cfg.save()
    }

    fun stringTimeValidator(time: String): Boolean {
        val format = "([01]?[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]"
        val pattern = Pattern.compile(format)
        val matcher = pattern.matcher(time)
        if (matcher.matches()) {
            return true
        }
        return false
    }

    private fun msgValidator(msg: String): Boolean {
        if (!LicenseStatus.isInsider()) {
            if (msg !in allowMsg) {
                return false
            }
        }
        return true
    }
}
