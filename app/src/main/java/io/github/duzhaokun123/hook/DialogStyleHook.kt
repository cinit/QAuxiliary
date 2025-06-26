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

package io.github.duzhaokun123.hook

import android.app.Dialog
import android.content.Context
import android.view.ContextThemeWrapper
import androidx.core.content.res.ResourcesCompat
import com.github.kyuubiran.ezxhelper.utils.findConstructor
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import io.github.qauxv.R
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Log

@UiItemAgentEntry
@FunctionHookEntry
object QaDialogStyleHook : CommonSwitchFunctionHook() {
    override val name = "统一 QA dialog 样式"
    override val description = "统一成 MD3 风格, 仅影响 dialog 外观 不影响内容"
    override val uiItemLocation = FunctionEntryRouter.Locations.Entertainment.ENTERTAIN_CATEGORY
    override fun initOnce(): Boolean {
        Dialog::class.java
            .findConstructor { parameterTypes contentEquals arrayOf(Context::class.java, Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType) }
            .hookBefore {
                val context = it.args[0] as Context
                val themeId = it.args[1] as Int
                val createContextThemeWrapper = it.args[2] as Boolean
                Log.d("QaDialogStyleHook: $createContextThemeWrapper $themeId $context")
                val newContext = ContextThemeWrapper(context, com.google.android.material.R.style.Theme_Material3_DayNight)
                it.args[0] = newContext
                it.args[1] = ResourcesCompat.ID_NULL
                it.args[2] = true
            }
        Dialog::class.java
            .findMethod { name == "show" }
            .hookAfter {
                val dialog = it.thisObject as Dialog
                dialog.window!!.apply {
                    setBackgroundDrawableResource(R.drawable.dialog_background)
                }
            }
        return true
    }
}