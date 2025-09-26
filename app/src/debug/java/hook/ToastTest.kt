/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2025 QAuxiliary developers
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

package hook

import android.app.Activity
import android.app.AlertDialog
import android.view.View
import android.widget.Toast
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.util.Toasts

@UiItemAgentEntry
@FunctionHookEntry
object ToastTest : CommonConfigFunctionHook() {
    override val name = "Toast 测试"
    override val description = "测试 Toast"
    override val valueState = null
    override val uiItemLocation = FunctionEntryRouter.Locations.DebugCategory.DEBUG_CATEGORY

    override fun initOnce(): Boolean {
        return false
    }

    override val onUiItemClickListener = { _: IUiItemAgent, activity: Activity, _: View ->
        AlertDialog.Builder(activity)
            .setItems(arrayOf("error", "success", "info", "plain", "system")) { _, which ->
                when (which) {
                    0 -> Toasts.error(activity, "错误 Toast")
                    1 -> Toasts.success(activity, "成功 Toast")
                    2 -> Toasts.info(activity, "信息 Toast")
                    3 -> Toasts.show(activity, "普通 Toast")
                    4 -> Toast.makeText(activity, "系统 Toast", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
        Unit
    }
}
