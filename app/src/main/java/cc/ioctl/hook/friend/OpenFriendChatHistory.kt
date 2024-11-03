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
import cc.hicore.QApp.QAppUtils
import cc.ioctl.util.LayoutHelper
import cc.ioctl.util.ui.FaultyDialog
import io.github.qauxv.base.IEntityAgent
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
    override val titleProvider: (IEntityAgent) -> String = { "打开好友聊天记录" }
    override val summaryProvider: ((IEntityAgent, Context) -> String?) = { _, _ -> "仅支持本地聊天记录" }
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
            .setTitle("请输入对方 qq 或 uid ")
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
                    Toasts.error(ctx, "请输入 qq 或 uid")
                    return@setOnClickListener
                }
                try {
                    val uin = text.toLong()
                    if (uin < 10000) {
                        Toasts.error(ctx, "请输入有效的 qq")
                        return@setOnClickListener
                    }
                    startFriendChatHistoryActivity(ctx, uin)
                } catch (ignored: NumberFormatException) {
                    val uid = text
                    if (!(uid.startsWith("u_") && uid.length == 24)) {
                        Toasts.error(ctx, "请输入有效的 uid")
                        return@setOnClickListener
                    }
                    startFriendChatHistoryActivity(ctx, uid)
                }
                alertDialog.dismiss()
            }
    }

    @JvmStatic
    fun startFriendChatHistoryActivity(context: Context, uin: Long) {
        if (uin < 10000L) return
        try {
            startFriendChatHistory(context, uin.toString(), false)
        } catch (e: NumberFormatException) {
            return
        }
    }

    @JvmStatic
    fun startFriendChatHistoryActivity(context: Context, uid: String) {
        if (!(uid.startsWith("u_") && uid.length == 24)) return
        try {
            startFriendChatHistory(context, uid, true)
        } catch (e: NumberFormatException) {
            return
        }
    }

    private fun startFriendChatHistory(context: Context, userUinOrUid: String, isUid: Boolean) {
        try {
            if (QAppUtils.isQQnt()) {
                val peerId = if (isUid) userUinOrUid else QAppUtils.UserUinToPeerID(userUinOrUid)
                if (peerId == null) {
                    FaultyDialog.show(context, "打开好友聊天记录错误", "无法获取 peerId")
                    return
                }
                val kNTChatHistoryActivity = Initiator.loadClass("com.tencent.mobileqq.activity.history.NTChatHistoryActivity")
                val intent = Intent(context, kNTChatHistoryActivity).apply {
                    //putExtra("nt_chat_history_session_name", name)    //显示在标题栏的昵称
                    putExtra("nt_chat_history_peerId", peerId)
                    putExtra("nt_chat_history_chatType", 1)
                }
                context.startActivity(intent)
            } else {
                if (isUid) {
                    FaultyDialog.show(context, "打开好友聊天记录错误", "非 QQNT 版本无法使用 uid 打开")
                    return
                }
                val kChatHistoryActivity = Initiator.loadClass("com.tencent.mobileqq.activity.history.ChatHistoryActivity")
                val intent = Intent(context, kChatHistoryActivity).apply {
                    putExtra("uin", userUinOrUid)
                    putExtra("SissionUin", userUinOrUid)
                    putExtra("uintype", 0)
                    putExtra("TargetTabPos", 0)
                    putExtra("FromType", 3011)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            FaultyDialog.show(context, "打开好友聊天记录错误", e)
        }
    }
}
