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
package me.singleneuron.hook

import android.app.Activity
import android.content.Intent
import android.util.Base64
import androidx.core.net.toUri
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedBridge
import me.singleneuron.util.NoAppletUtil
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object NoApplet : CommonSwitchFunctionHook() {

    override val name = "小程序分享转链接（发送）"
    override val description = "感谢Alcatraz323开发的远离小程序，由神经元移植到Xposed"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY

    private fun decodeBase64QueryParam(value: String?): String? {
        if (value.isNullOrEmpty()) return null
        return runCatching {
            String(Base64.decode(value.replace(' ', '+'), Base64.DEFAULT), Charsets.UTF_8)
        }.getOrNull()
    }

    override fun initOnce() = throwOrTrue {
        XposedBridge.hookAllMethods(Activity::class.java, "getIntent", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                // Log.d("NoApplet started: "+param.thisObject::class.java.simpleName)
                if (param.thisObject::class.java.simpleName != "JumpActivity") return

                val originIntent = param.result as Intent
                val originUri = originIntent.data ?: return
                val schemeUri = originUri.toString()
                // Log.d("NoApplet schemeUri: $schemeUri")

                if (schemeUri.contains("mqqapi://share/to_fri")) {
                    val titleBase64 = originUri.getQueryParameter("title")
                    val title = decodeBase64QueryParam(titleBase64)
                    // Log.d("NoApplet title: $titleBase64 -> $title")
                    val descBase64 = originUri.getQueryParameter("description")
                    val desc = decodeBase64QueryParam(descBase64)
                    // Log.d("NoApplet desc: $descBase64 -> $desc")
                    val urlBase64 = originUri.getQueryParameter("url")
                    val url = decodeBase64QueryParam(urlBase64)
                    // Log.d("NoApplet url: $urlBase64 -> $url")

                    val shareText = buildString {
                        if (!title.isNullOrBlank()) appendLine(title)
                        if (!desc.isNullOrBlank()) appendLine(desc)
                        if (!url.isNullOrBlank()) appendLine(url)
                    }
                    param.result = Intent(originIntent).apply {
                        data = null
                        component = null
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                } else if (schemeUri.contains("mini_program")) {
                    val processScheme = NoAppletUtil.removeMiniProgramNode(schemeUri)
                    val newScheme = NoAppletUtil.replace(processScheme, "req_type", "MQ==")
                    param.result = originIntent.apply {
                        data = newScheme.toUri()
                        component = null
                    }
                }
            }
        })
    }
}
