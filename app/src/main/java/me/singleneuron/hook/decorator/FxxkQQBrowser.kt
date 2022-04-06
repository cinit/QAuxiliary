/*
 * QNotified - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 dmca@ioctl.cc
 * https://github.com/ferredoxin/QNotified
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/ferredoxin/QNotified/blob/master/LICENSE.md>.
 */

package me.singleneuron.hook.decorator

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_DARK
import androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_LIGHT
import cc.ioctl.hook.JumpController
import de.robv.android.xposed.XC_MethodHook
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.router.decorator.BaseSwitchFunctionDecorator
import io.github.qauxv.router.decorator.IStartActivityHookDecorator
import io.github.qauxv.router.dispacher.StartActivityHook
import io.github.qauxv.ui.ResUtils
import io.github.qauxv.util.hostInfo

@UiItemAgentEntry
object FxxkQQBrowser : BaseSwitchFunctionDecorator(), IStartActivityHookDecorator {

    override val name = "去你大爷的QQ浏览器"
    override val description = "致敬 “去你大爷的内置浏览器”"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.UI_MISC
    override val dispatcher = StartActivityHook

    @SuppressLint("ResourceType")
    override fun doDecorate(intent: Intent, param: XC_MethodHook.MethodHookParam): Boolean {
        val url = intent.getStringExtra("url")
        /*intent.dump()
        val check1 = !url.isNullOrBlank()
        val check2 = url?.contains(Regex("http|https",RegexOption.IGNORE_CASE))
        val check3 = intent.component?.shortClassName?.contains("QQBrowserActivity")
        Log.d("check1=$check1 check2=$check2 check3=$check3")*/
        return if (!url.isNullOrBlank()
                && url.contains(Regex("http|https", RegexOption.IGNORE_CASE))
                && !url.contains(Regex("qq.com|tenpay.com|meeting.tencent.com"))
                && intent.component?.shortClassName?.contains("QQBrowserActivity") == true
        ) {
            val customTabsIntent = CustomTabsIntent.Builder().apply {
                if (ResUtils.isInNightMode()) {
                    setColorScheme(COLOR_SCHEME_DARK)
                    // QQ dark theme in does not seems to have an accent color
                } else {
                    setColorScheme(COLOR_SCHEME_LIGHT)
                    // FF: QQ does not override the colorPrimary in the theme
                    // TODO: 2022-03-06 find the actual effective primary color for the QQ theme
                    // try {
                    //     val color = getColorPrimary()
                    //     setDefaultColorSchemeParams(
                    //         CustomTabColorSchemeParams.Builder()
                    //             .setToolbarColor(color)
                    //             .build()
                    //     )
                    // } catch (e: Exception) {
                    //     traceError(e)
                    // }
                }
                setShowTitle(true)
            }.build()
            customTabsIntent.intent.apply {
                putExtra("from_fqb", true)
                putExtra(JumpController.EXTRA_JMP_JEFS_PERMISSIVE, true)
            }
            customTabsIntent.launchUrl(hostInfo.application, Uri.parse(url))
            param.result = null
            true
        } else {
            false
        }
    }

    @ColorInt
    @Throws(Exception::class)
    fun getColorPrimary(): Int {
        val typedValue = TypedValue()
        hostInfo.application.theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
        return typedValue.data
    }

    fun processJefs(intent: Intent): Boolean {
        return isEnabled && intent.getBooleanExtra("from_fqb", false)
    }
}
