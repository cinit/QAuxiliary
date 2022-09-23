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

package me.ketal.hook

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.view.View
import com.github.kyuubiran.ezxhelper.utils.invokeMethodAutoAs
import com.github.kyuubiran.ezxhelper.utils.invokeStaticMethodAuto
import com.github.kyuubiran.ezxhelper.utils.loadClass
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.ui.CustomDialog
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.hostInfo
import io.github.qauxv.util.requireMinQQVersion
import kotlinx.coroutines.flow.MutableStateFlow
import me.ketal.util.setEnable
import xyz.nextalone.util.method
import xyz.nextalone.util.replace
import xyz.nextalone.util.throwOrTrue

@[FunctionHookEntry UiItemAgentEntry]
object ChangeQQIcon : CommonConfigFunctionHook("Ketal_ManageComponent") {

    override val name = "修改QQ图标"
    override val valueState: MutableStateFlow<String?>? = null

    override val onUiItemClickListener: (IUiItemAgent, Activity, View) -> Unit = { _, activity, _ ->
        showDialog(activity)
    }
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_10)
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MISC_CATEGORY
    override fun initOnce() = throwOrTrue {
        val clazz = loadClass("com.tencent.mobileqq.vas.api.impl.VasAppIconStateManagerImpl")
        clazz.method("getSplashActivityComponent", ComponentName::class.java, Context::class.java)!!
            .replace(this) {
                val ctx = it.args[0] as Context
                val pm: PackageManager = ctx.packageManager
                pm.getLaunchIntentForPackage(hostInfo.packageName)!!.component
            }
    }

    private fun showDialog(ctx: Context) {
        if (!isAvailable) {
            Toasts.error(ctx, "当前QQ版本不支持修改QQ图标")
            return
        }
        val api = loadClass("com.tencent.mobileqq.qroute.QRoute")
            .invokeStaticMethodAuto("api",
                loadClass("com.tencent.mobileqq.vas.api.IVasAppIconStateManager"))!!

        val resources = hostInfo.application.resources
        val applicationInfo = hostInfo.application.applicationInfo
        val pm: PackageManager = ctx.packageManager
        val alias = api.invokeMethodAutoAs<Array<Any>>("getAllAlias")!!
            .map { it.toString().substringAfter("(").substringBefore(')').split(", ") }
        val list = alias.map {
            pm.getActivityInfo(ComponentName(hostInfo.packageName, it[1]), PackageManager.MATCH_DISABLED_COMPONENTS)
        }
        val items = Array(list.size) {
            val info = list[it]
            SpannableStringBuilder().apply {
                val icon = BitmapFactory.decodeResource(resources, info.icon)
                    ?: BitmapFactory.decodeResource(resources, applicationInfo.icon)
                append("icon", MyIm(ctx, icon), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                append("     " + info.loadLabel(pm))
            }
        }
        val current = pm.getLaunchIntentForPackage(hostInfo.packageName)!!.component!!

        AlertDialog.Builder(ctx, CustomDialog.themeIdForDialog())
            .setTitle("请选择一个图标")
            .setSingleChoiceItems(items, list.indexOfFirst { it.name == current.className }) { dialog, which ->
                val selected = list[which]
                current.setEnable(ctx, false)
                ComponentName(hostInfo.packageName, selected.name).setEnable(ctx, true)
                this.isEnabled = true
                Toasts.info(ctx, "修改完毕")
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private class MyIm(ctx: Context, bitmap: Bitmap) : ImageSpan(ctx, bitmap) {

        override fun getSize(
            paint: Paint, text: CharSequence?, start: Int, end: Int,
            fm: Paint.FontMetricsInt?
        ): Int {
            val rect = drawable.bounds
            if (fm != null) {
                val fmPaint = paint.fontMetricsInt
                val fontHeight: Int = fmPaint.bottom - fmPaint.top
                val drHeight: Int = rect.bottom - rect.top
                val top = drHeight / 2 - fontHeight / 4
                val bottom = drHeight / 2 + fontHeight / 4
                fm.ascent = -bottom
                fm.top = -bottom
                fm.bottom = top
                fm.descent = top
            }
            return rect.right
        }

        override fun draw(canvas: Canvas, text: CharSequence?, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
            canvas.save()
            val transY = (bottom - top - drawable.bounds.bottom) / 2 + top
            canvas.translate(x, transY.toFloat())
            drawable.draw(canvas)
            canvas.restore()
        }
    }
}
