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

package me.hd.hook

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.constraintlayout.widget.ConstraintSet
import cc.ioctl.util.LayoutHelper
import com.github.kyuubiran.ezxhelper.utils.argTypes
import com.github.kyuubiran.ezxhelper.utils.args
import com.github.kyuubiran.ezxhelper.utils.findConstructor
import com.github.kyuubiran.ezxhelper.utils.findFieldObject
import com.github.kyuubiran.ezxhelper.utils.findFieldObjectAs
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.invokeMethod
import com.github.kyuubiran.ezxhelper.utils.newInstance
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.TIMVersion
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.requireMinTimVersion
import xyz.nextalone.util.clazz

@FunctionHookEntry
@UiItemAgentEntry
object TimMineAddPdFunc : CommonSwitchFunctionHook() {
    override val name = "TIM 我的添加频道入口"
    override val description = "仅适配 TIM_NT"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY
    override val isAvailable = requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA)

    private val constraintSetClz by lazy { "androidx.constraintlayout.widget.ConstraintSet".clazz!! }
    private val constraintLayoutClz by lazy { "androidx.constraintlayout.widget.ConstraintLayout".clazz!! }

    private fun Any.clone(constraintLayout: Any) {
        val constraintSet = this
        constraintSet.invokeMethod(
            "clone",
            args(constraintLayout),
            argTypes(constraintLayoutClz)
        )
    }

    private fun Any.connect(startID: Int, startSide: Int, endID: Int, endSide: Int, margin: Int = 0) {
        val constraintSet = this
        constraintSet.invokeMethod(
            "connect",
            args(startID, startSide, endID, endSide, margin),
            argTypes(Int::class.java, Int::class.java, Int::class.java, Int::class.java, Int::class.java)
        )
    }

    private fun Any.applyTo(constraintLayout: Any) {
        val constraintSet = this
        constraintSet.invokeMethod(
            "applyTo",
            args(constraintLayout),
            argTypes(constraintLayoutClz)
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun initOnce(): Boolean {
        Initiator.loadClass("com.tencent.mobileqq.activity.tim.mine.e").findConstructor {
            parameterTypes contentEquals arrayOf(Context::class.java, View::class.java)
        }.hookAfter {
            val context = it.args[0] as Context
            val viewBinding = it.thisObject.findFieldObject { name == "d" }
            val rootLayout = viewBinding.invokeMethod("getRoot") as ViewGroup
            val qrCodeIv = viewBinding.findFieldObjectAs<ImageView> { type == ImageView::class.java }
            val pdEntryTv = TextView(context).apply {
                id = View.generateViewId()
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                text = "频"
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                setTextColor(if (night) Color.WHITE else Color.BLACK)
                setOnClickListener {
                    try {
                        val browser = Initiator.loadClass("com.tencent.mobileqq.activity.QQBrowserDelegationActivity")
                        context.startActivity(
                            Intent(context, browser).apply {
                                putExtra("fling_action_key", 2)
                                putExtra("fling_code_key", context.hashCode())
                                putExtra("useDefBackText", true)
                                putExtra("param_force_internal_browser", true)
                                putExtra("url", "https://pd.qq.com/")
                            }
                        )
                    } catch (e: Exception) {
                        Toasts.error(context, "无法启动内置浏览器")
                        e.printStackTrace()
                    }
                }
            }.also { iv ->
                rootLayout.addView(iv)
            }
            constraintSetClz.newInstance(args())!!.apply {
                clone(rootLayout)
                connect(pdEntryTv.id, ConstraintSet.START, qrCodeIv.id, ConstraintSet.END, LayoutHelper.dip2px(context, 16f))
                connect(pdEntryTv.id, ConstraintSet.TOP, qrCodeIv.id, ConstraintSet.TOP)
                connect(pdEntryTv.id, ConstraintSet.BOTTOM, qrCodeIv.id, ConstraintSet.BOTTOM)
                applyTo(rootLayout)
            }
        }
        return true
    }
}
