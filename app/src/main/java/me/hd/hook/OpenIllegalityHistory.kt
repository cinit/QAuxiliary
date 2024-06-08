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
import android.content.Intent
import android.view.View
import io.github.qauxv.base.ISwitchCellAgent
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.IUiItemAgentProvider
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Toasts
import kotlinx.coroutines.flow.StateFlow

@UiItemAgentEntry
object OpenIllegalityHistory : IUiItemAgentProvider {
    override val uiItemAgent = object : IUiItemAgent {
        override val titleProvider: (IUiItemAgent) -> String = { "查看历史违规记录" }
        override val summaryProvider: ((IUiItemAgent, Context) -> String?) = { _, _ -> null }
        override val valueState: StateFlow<String?>? = null
        override val validator: ((IUiItemAgent) -> Boolean)? = null
        override val switchProvider: ISwitchCellAgent? = null
        override val onClickListener: ((IUiItemAgent, Activity, View) -> Unit) = { _, ctx, _ ->
            try {
                Toasts.success(ctx, "正在打开")
                val browser = Initiator.loadClass("com.tencent.mobileqq.activity.QQBrowserDelegationActivity")
                ctx.startActivity(
                    Intent(ctx, browser).apply {
                        putExtra("fling_action_key", 2)
                        putExtra("fling_code_key", ctx.hashCode())
                        putExtra("useDefBackText", true)
                        putExtra("param_force_internal_browser", true)
                        putExtra("url", "https://m.q.qq.com/a/s/07befc388911b30c2359bfa383f2d693")
                    }
                )
            } catch (e: Exception) {
                Toasts.error(ctx, "打开失败")
                e.printStackTrace()
            }
        }
        override val extraSearchKeywordProvider: ((IUiItemAgent, Context) -> Array<String>?)? = null
    }
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MISC_CATEGORY
}