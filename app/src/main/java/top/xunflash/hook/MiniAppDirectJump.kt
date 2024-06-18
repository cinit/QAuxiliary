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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
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

    private fun expandShortUrl(shortUrl: String): String? = CoroutineScope(Dispatchers.IO).future {
        val connection = (URL(shortUrl).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            connectTimeout = 10000
            readTimeout = 10000
            connect()
        }
        val result = connection.getHeaderField("Location")
        connection.disconnect()
        result
    }.get()

    private fun toBiliApp(ctx: Activity, text: String) {

        val bilibiliPackageName = "tv.danmaku.bili"
        val bilibiliHdPackageName = "tv.danmaku.bilibilihd"

        // 检查应用是否安装
        fun isPackageInstalled(packageName: String): Boolean {
            return try {
                ctx.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }

        val isBilibiliInstalled = isPackageInstalled(bilibiliPackageName)
        val isBilibiliHdInstalled = isPackageInstalled(bilibiliHdPackageName)

        // 提取哔哩链接
        val regexEscaped = """https:\\/\\/b23\.tv\\/(\w+)\?[^"]*""".toRegex()
        val regexNormal = """https://b23\.tv/(\w+)\?[^"]*""".toRegex()

        // 查找匹配
        val resultEscaped = regexEscaped.find(text)?.groupValues?.get(1)
        val resultNormal = regexNormal.find(text)?.groupValues?.get(1)

        // 根据匹配结果生成正常链接
        val matchResult = when {
            resultEscaped != null -> "https://b23.tv/$resultEscaped"
            resultNormal != null -> "https://b23.tv/$resultNormal"
            else -> null
        }
        matchResult?.let { b23url ->
            try {
                // 还原短链
                val expandedUrl = expandShortUrl(b23url)
                if (expandedUrl == null) {
                    Toasts.info(ctx, "无法还原短链接")
                    return
                }

                // 查找匹配
                val regex = """https://www\.bilibili\.com/video/(BV\w+)\?""".toRegex()
                val bvNumber = regex.find(expandedUrl)?.groupValues?.get(1)
                if (bvNumber == null) {
                    Toasts.info(ctx, "未找到BV号")
                    return
                }

                if (!isBilibiliInstalled && !isBilibiliHdInstalled) {
                    Toasts.info(ctx, "未安装应用")
                    return
                }

                // 构建Intent
                val uri = Uri.parse("bilibili://video/$bvNumber")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage(if (isBilibiliHdInstalled) bilibiliHdPackageName else bilibiliPackageName)
                }
                ctx.startActivity(intent)

            } catch (e: Exception) {
                Toasts.info(ctx, "还原请求出错")
            }
        } ?: Toasts.info(ctx, "未找到打开链接")


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