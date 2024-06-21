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

package top.xunflash.hook

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.View
import cc.hicore.QApp.QAppUtils
import cc.ioctl.util.Reflex
import cc.ioctl.util.afterHookIfEnabled
import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import com.xiaoniu.dispatcher.OnMenuBuilder
import com.xiaoniu.util.ContextUtils
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.qauxv.R
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.CustomMenu
import io.github.qauxv.util.CustomMenu.createItemIconNt
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.dexkit.AbstractQQCustomMenuItem
import io.github.qauxv.util.dexkit.CArkAppItemBubbleBuilder
import io.github.qauxv.util.dexkit.DexKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import xyz.nextalone.util.throwOrTrue
import java.lang.reflect.Array
import java.net.HttpURLConnection
import java.net.URL


@FunctionHookEntry
@UiItemAgentEntry
object MiniAppDirectJump : CommonSwitchFunctionHook("MiniAppDirectJump::BaseChatPie", arrayOf(CArkAppItemBubbleBuilder, AbstractQQCustomMenuItem)),
    OnMenuBuilder {
    override val name: String = "小程序/分享卡片跳转APP"

    override val description: String = "长按小程序/分享卡片增加直接打开APP的菜单项，省去打开卡顿的小程序/网页的步骤（暂时仅支持哔哩哔哩）"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY

    // 借用 cc/ioctl/hook/msg/CopyCardMsg.kt
    override fun initOnce() = throwOrTrue {
        if (QAppUtils.isQQnt()) {
            return@throwOrTrue
        }
        val cl_ArkAppItemBuilder = DexKit.loadClassFromCache(CArkAppItemBubbleBuilder)
        XposedHelpers.findAndHookMethod(
            cl_ArkAppItemBuilder, "a", Int::class.javaPrimitiveType, Context::class.java,
            Initiator.load("com/tencent/mobileqq/data/ChatMessage"), menuItemClickCallback
        )
        for (m in cl_ArkAppItemBuilder!!.declaredMethods) {
            if (!m.returnType.isArray) {
                continue
            }
            val ps = m.parameterTypes
            if (ps.size == 1 && ps[0] == View::class.java) {
                XposedBridge.hookMethod(m, getMenuItemCallBack)
                break
            }
        }
    }

    private val getMenuItemCallBack = afterHookIfEnabled(60) { param ->
        val arr = param.result
        val clQQCustomMenuItem = arr.javaClass.componentType
        val item_copy = CustomMenu.createItem(clQQCustomMenuItem, R.id.item_jump_to_app, "打开")
        val ret = Array.newInstance(clQQCustomMenuItem, Array.getLength(arr) + 1)
        Array.set(ret, 0, Array.get(arr, 0))
        System.arraycopy(arr, 1, ret, 2, Array.getLength(arr) - 1)
        Array.set(ret, 1, item_copy)
        param.result = ret
    }

    private val menuItemClickCallback = afterHookIfEnabled(60) { param ->
        val id = param.args[0] as Int
        val ctx = param.args[1] as Activity
        val chatMessage = param.args[2]

        if (Initiator.loadClass("com.tencent.mobileqq.data.MessageForArkApp")
                .isAssignableFrom(chatMessage.javaClass)
        ) {
            val text = Reflex.invokeVirtual(
                Reflex.getInstanceObjectOrNull(chatMessage, "ark_app_message"), "toAppXml"
            ) as String
            toBiliApp(ctx, text)
        }
    }

    private suspend fun resolveShortUrl(shortUrl: String): String = runCatching {
        withTimeout(3000) {
            val connection = (URL(shortUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connect()
            }
            if (connection.responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                connection.getHeaderField("Location")
            } else shortUrl
        }
    }.getOrNull() ?: shortUrl

    private fun toBiliApp(ctx: Activity, text: String) = MainScope().launch {
        fun isPackageInstalled(packageName: String) = try {
            ctx.packageManager.getApplicationInfo(packageName, 0).enabled
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

        val bilibiliPackages = arrayOf("tv.danmaku.bili", "com.bilibili.app.in", "tv.danmaku.bilibilihd", "com.bilibili.app.blue")
        val firstValidPackage = bilibiliPackages.firstOrNull { isPackageInstalled(it) }
            ?: run { Toasts.info(ctx, "未找到可打开应用");return@launch }

        val bilibiliLinkRegex = """https?:\\?/\\?/((b23\.tv)|([\w.]*bilibili\.com))[^"]*""".toRegex()
        val (url, isShortLink) = bilibiliLinkRegex.find(text)?.let {
            it.value.replace("\\/", "/") to it.groupValues[2].isNotEmpty()
        } ?: run { Toasts.info(ctx, "链接解析失败");return@launch }
        val resolvedUrl = if (isShortLink) withContext(Dispatchers.IO) { resolveShortUrl(url) } else url

        try {
            val uri = Uri.parse(resolvedUrl)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(firstValidPackage)
            }
            ctx.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toasts.info(ctx, "应用打开失败")
        }
    }

    override val targetComponentTypes = arrayOf("com.tencent.mobileqq.aio.msglist.holder.component.ark.AIOArkContentComponent")

    override fun onGetMenuNt(msg: Any, componentType: String, param: MethodHookParam) {
        if (!isEnabled) return
        val ctx = ContextUtils.getCurrentActivity()
        val item = createItemIconNt(msg, "打开", R.drawable.ic_item_open_72dp, R.id.item_jump_to_app) {
            val element = (msg.javaClass.declaredMethods.first {
                it.returnType == MsgElement::class.java && it.parameterTypes.isEmpty()
            }.apply { isAccessible = true }.invoke(msg) as MsgElement).arkElement
            toBiliApp(ctx, element.bytesData)
        }
        val list = param.result as MutableList<Any>
        list.add(1, item)
    }
}
