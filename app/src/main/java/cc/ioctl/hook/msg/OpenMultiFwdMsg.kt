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
package cc.ioctl.hook.msg

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.text.TextUtils
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import cc.ioctl.util.LayoutHelper
import cc.ioctl.util.ui.FaultyDialog
import io.github.qauxv.base.IEntityAgent
import io.github.qauxv.base.ISwitchCellAgent
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.IUiItemAgentProvider
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.bridge.AppRuntimeHelper
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Toasts
import kotlinx.coroutines.flow.MutableStateFlow

@UiItemAgentEntry
object OpenMultiFwdMsg : IUiItemAgent, IUiItemAgentProvider {
    override val titleProvider: (IEntityAgent) -> String = { "打开合并转发消息" }
    override val summaryProvider: ((IEntityAgent, Context) -> String?)? = null
    override val valueState: MutableStateFlow<String?>? = null
    override val validator: ((IUiItemAgent) -> Boolean)? = null
    override val switchProvider: ISwitchCellAgent? = null
    override val extraSearchKeywordProvider: ((IUiItemAgent, Context) -> Array<String>?)? = null
    override val uiItemAgent: IUiItemAgent = this
    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY

    override val onClickListener: ((IUiItemAgent, Activity, View) -> Unit) = { _, activity, _ ->
        onClick(activity)
    }

    fun onClick(baseContext: Context) {
        val ctx = CommonContextWrapper.createAppCompatContext(baseContext)
        val editText = EditText(ctx)
        editText.textSize = 16f
        val linearLayout = LinearLayout(ctx)
        linearLayout.addView(editText, LayoutHelper.newLinearLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val alertDialog = AlertDialog.Builder(ctx)
            .setTitle("输入合并消息的 ResID (看起来像 base64 一样的东西)")
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
                    Toasts.error(ctx, "请输入合并消息的 ResID")
                    return@setOnClickListener
                }
                if (!checkMultiMsgUri(text)) {
                    Toasts.error(ctx, "请输入有效的 ResID")
                    return@setOnClickListener
                }
                alertDialog.dismiss()
                startMultiForwardActivity(ctx, text)
            }
    }

    @JvmStatic
    fun checkMultiMsgUri(multiUri: String): Boolean {
        // expect a 0x30 length encoded base64
        if (!multiUri.matches(Regex("^[\\dA-Za-z+/]+$"))) {
            return false
        }
        return try {
            val bytes = Base64.decode(multiUri, Base64.DEFAULT)
            bytes.size == 0x30
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    @JvmStatic
    fun startMultiForwardActivity(context: Context, multiUri: String) {
        if (!checkMultiMsgUri(multiUri)) {
            return
        }
        try {
            val kMultiForwardActivity = Initiator.loadClass("com.tencent.mobileqq.activity.MultiForwardActivity")
            val intent = Intent(context, kMultiForwardActivity).apply {
                putExtra("multi_url", multiUri)
                putExtra("uin", AppRuntimeHelper.getAccount())
                putExtra("uintype", 0)
                putExtra("troop_code", null as String?)
                putExtra("chat_subType", 3)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            FaultyDialog.show(context, "打开合并聊天记录错误", e)
        }
    }
}
