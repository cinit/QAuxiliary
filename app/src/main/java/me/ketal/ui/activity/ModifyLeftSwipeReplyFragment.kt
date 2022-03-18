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
package me.ketal.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.LinearLayout
import androidx.core.view.plusAssign
import cc.ioctl.util.HostStyledViewBuilder
import cc.ioctl.util.ui.newListItemHookSwitchInit
import cc.ioctl.util.ui.newListItemSwitch
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.getInputLayout
import com.afollestad.materialdialogs.input.input
import com.tencent.mobileqq.widget.BounceScrollView
import io.github.qauxv.fragment.BaseClipSettingFragment
import me.ketal.hook.LeftSwipeReplyHook
import me.ketal.util.ignoreResult

@SuppressLint("Registered")
class ModifyLeftSwipeReplyFragment : BaseClipSettingFragment() {

    override fun getTitle() = "修改消息左滑动作"

    override fun doOnCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = settingsHostActivity!!
        val ll = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            val hook = LeftSwipeReplyHook
            val ctx = activity
            this += newListItemHookSwitchInit(ctx, "总开关", "打开后才可使用以下功能", hook)
            this += newListItemSwitch(ctx, "取消消息左滑动作", "取消取消，一定要取消", hook.isNoAction
            ) { _: CompoundButton?, on: Boolean -> hook.isNoAction = on }
            this += HostStyledViewBuilder.newListItemButton(ctx, "修改左滑消息灵敏度", "妈妈再也不用担心我误触了", null) {
                val dialog = MaterialDialog(ctx).show {
                    title(text = "输入响应消息左滑的距离")
                    input(prefill = hook.replyDistance.toString(), waitForPositiveButton = false) { dialog, text ->
                        val inputField = dialog.getInputField()
                        val isValid = try {
                            text.toString().toInt()
                            true
                        } catch (e: NumberFormatException) {
                            false
                        }
                        inputField.error = if (isValid) null else "请输入有效的数据"
                        dialog.setActionButtonEnabled(WhichButton.POSITIVE, isValid)
                    }.ignoreResult()
                    positiveButton(text = "确定") {
                        val text = it.getInputField().text.toString()
                        hook.replyDistance = text.toInt()
                        it.dismiss()
                    }
                    negativeButton(text = "取消")
                }
                dialog.getInputLayout().addView(HostStyledViewBuilder.subtitle(ctx,
                        "若显示为-1，代表为初始化，请先在消息界面使用一次消息左滑回复，即可获得初始阈值。\n当你修改出错时，输入一个小于0的值，即可使用默认值"), 0)
            }
            this += newListItemSwitch(ctx, "左滑多选消息", "娱乐功能，用途未知", hook.isMultiChose
            ) { _: CompoundButton?, on: Boolean -> hook.isMultiChose = on }
        }
        val rootView = BounceScrollView(activity, null).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            addView(ll, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        return rootView
    }
}
