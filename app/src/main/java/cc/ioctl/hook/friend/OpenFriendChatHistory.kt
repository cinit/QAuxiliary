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

package cc.ioctl.hook.friend

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import cc.ioctl.util.LayoutHelper
import cc.ioctl.util.ui.FaultyDialog
import io.github.qauxv.base.ISwitchCellAgent
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.IUiItemAgentProvider
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Toasts
import kotlinx.coroutines.flow.MutableStateFlow

@UiItemAgentEntry
object OpenFriendChatHistory : IUiItemAgent, IUiItemAgentProvider {
    override val titleProvider: (IUiItemAgent) -> String = { "打开好友聊天记录" }
    override val summaryProvider: ((IUiItemAgent, Context) -> String?) = { _, _ -> "仅支持本地聊天记录" }
    override val valueState: MutableStateFlow<String?>? = null
    override val validator: ((IUiItemAgent) -> Boolean)? = null
    override val switchProvider: ISwitchCellAgent? = null
    override val extraSearchKeywordProvider: ((IUiItemAgent, Context) -> Array<String>?)? = null
    override val uiItemAgent: IUiItemAgent = this
    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Auxiliary.FRIEND_CATEGORY

    override val onClickListener: ((IUiItemAgent, Activity, View) -> Unit) = { _, activity, _ ->
        onClick(activity)
    }

    fun onClick(baseContext: Context) {
        val ctx = CommonContextWrapper.createAppCompatContext(baseContext)
        val editText = EditText(ctx)
        editText.textSize = 16f
        val linearLayout = LinearLayout(ctx)
        linearLayout.addView(editText, LayoutHelper.newLinearLayoutParams(MATCH_PARENT, WRAP_CONTENT))
        val alertDialog = AlertDialog.Builder(ctx)
            .setTitle("输入对方 QQ 号")
            .setView(linearLayout)
            .setCancelable(true)
            .setPositiveButton("确认", null)
            .setNegativeButton("取消", null)
            .create()
        alertDialog.show()
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
            .setOnClickListener {
                val text = editText.text.toString()
                if (TextUtils.isEmpty(text)) {
                    Toasts.error(ctx, "请输入 QQ 号")
                    return@setOnClickListener
                }
                var uin: Long = 0
                try {
                    uin = text.toLong()
                } catch (ignored: NumberFormatException) {
                }
                if (uin < 10000) {
                    Toasts.error(ctx, "请输入有效的 QQ 号")
                    return@setOnClickListener
                }
                alertDialog.dismiss()
                startFriendChatHistoryActivity(ctx, uin)
            }
    }

    @JvmStatic
    fun startFriendChatHistoryActivity(context: Context, uin: String) {
        try {
            startFriendChatHistoryActivity(context, uin.toLong())
        } catch (e: NumberFormatException) {
            return
        }
    }

    @JvmStatic
    fun startFriendChatHistoryActivity(context: Context, uin: Long) {
        if (uin < 10000L) return
        try {
            val kChatHistoryActivity = Initiator.loadClass("com.tencent.mobileqq.activity.history.ChatHistoryActivity")
            val intent = Intent(context, kChatHistoryActivity).apply {
                putExtra("uin", uin.toString())
                putExtra("SissionUin", uin.toString())
                putExtra("uintype", 0)
                putExtra("TargetTabPos", 0)
                putExtra("FromType", 3011)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            FaultyDialog.show(context, "打开好友聊天记录错误", e)
        }
    }
}
