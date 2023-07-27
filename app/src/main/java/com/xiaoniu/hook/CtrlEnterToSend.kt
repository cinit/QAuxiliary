/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
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

package com.xiaoniu.hook

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcelable
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import cc.hicore.QApp.QAppUtils
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.router.decorator.IBaseChatPieInitDecorator
import mqq.app.AppRuntime


@FunctionHookEntry
@UiItemAgentEntry
object CtrlEnterToSend : CommonSwitchFunctionHook(), IBaseChatPieInitDecorator {
    override val name = "Ctrl+Enter发送消息"

    override val description = "与通用-回车键发送消息不冲突"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY

    override val isApplicationRestartRequired = true

    override fun initOnce(): Boolean {
        return true
    }

    @SuppressLint("DiscouragedApi")
    override fun onInitBaseChatPie(baseChatPie: Any, aioRootView: ViewGroup, session: Parcelable?, ctx: Context, rt: AppRuntime) {
        val inputTextId = ctx.resources.getIdentifier("input", "id", ctx.packageName)
        val input = aioRootView.findViewById<EditText>(inputTextId)
        val sendButtonId = ctx.resources.getIdentifier(if (QAppUtils.isQQnt()) "send_btn" else "fun_btn", "id", ctx.packageName)
        val send = aioRootView.findViewById<Button>(sendButtonId)
        input.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER && event.isCtrlPressed) {
                send.performClick()
                true
            } else false
        }

    }

}
