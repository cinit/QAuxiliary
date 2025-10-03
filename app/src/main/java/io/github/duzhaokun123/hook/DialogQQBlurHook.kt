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
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.get
import com.github.kyuubiran.ezxhelper.utils.argTypes
import com.github.kyuubiran.ezxhelper.utils.args
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.loadClass
import com.github.kyuubiran.ezxhelper.utils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.utils.newInstanceAs
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Log
import xyz.nextalone.util.method

@UiItemAgentEntry
@FunctionHookEntry
object DialogQQBlurHook : CommonSwitchFunctionHook() {
    override val name = "AlertDialog 使用 QQ toast 同款模糊背景"
    override val description = "和官方一样会因为亮色背景看不清字"
    override val uiItemLocation = FunctionEntryRouter.Locations.Entertainment.ENTERTAIN_CATEGORY
    override val isAvailable = loadClassOrNull("com.tencent.biz.qui.toast.blur.QUIToastBlurWrapper") != null

    override fun initOnce(): Boolean {
        val class_QUIToastBlurWrapper = loadClass("com.tencent.biz.qui.toast.blur.QUIToastBlurWrapper")
        "Landroid/app/Dialog;->show()V".method
            .hookBefore {
                val dialog = it.thisObject as Dialog
                val dialogClassName = dialog.javaClass.name
                if (dialogClassName != "android.app.AlertDialog"
                    && dialogClassName != "androidx.appcompat.app.AlertDialog") {
                    Log.d("DialogQQBlurHook: skip non explicit AlertDialog $dialogClassName")
                    return@hookBefore
                }
                val rootView = (dialog.window!!.decorView as ViewGroup)[0]
                if (class_QUIToastBlurWrapper.isInstance(rootView)) {
                    Log.d("DialogQQBlurHook: already wrapped")
                    return@hookBefore
                }
                (rootView.parent as ViewGroup).removeView(rootView)
                val wrapperView = class_QUIToastBlurWrapper.newInstanceAs<FrameLayout>(args(dialog.context), argTypes(Context::class.java))!!.apply {
                    addView(rootView)
                }
                (dialog.window!!.decorView as ViewGroup).addView(wrapperView)
            }

        return true
    }
}