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
import android.os.Build
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
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
        val et_type = EditText(activity).apply {
            hint = "类型"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            }
        }
        val btn_show = Button(activity).apply {
            text = "显示"
            setOnClickListener {
                Toasts.showToast(activity, et_type.text.toString().toIntOrNull() ?: 0, "测试", Toast.LENGTH_LONG)
            }
        }
        val ll = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(et_type)
            addView(btn_show)
        }
        AlertDialog.Builder(activity)
            .setTitle(name)
            .setView(ll)
            .show()
        Unit
    }
}
