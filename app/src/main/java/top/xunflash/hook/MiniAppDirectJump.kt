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

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.widget.SwitchCompat
import cc.hicore.QApp.QAppUtils
import cc.ioctl.util.Reflex
import cc.ioctl.util.afterHookIfEnabled
import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import com.xiaoniu.dispatcher.OnMenuBuilder
import com.xiaoniu.util.ContextUtils
import io.github.qauxv.R
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.util.CustomMenu
import io.github.qauxv.util.CustomMenu.createItemIconNt
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.dexkit.AbstractQQCustomMenuItem
import io.github.qauxv.util.dexkit.CArkAppItemBubbleBuilder
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.xpcompat.XC_MethodHook.MethodHookParam
import io.github.qauxv.util.xpcompat.XposedBridge
import io.github.qauxv.util.xpcompat.XposedHelpers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import me.ketal.data.ConfigData
import xyz.nextalone.util.throwOrTrue
import java.io.Serializable
import java.lang.reflect.Array
import java.net.HttpURLConnection
import java.net.URL

@FunctionHookEntry
@UiItemAgentEntry
object MiniAppDirectJump : CommonConfigFunctionHook(targets = arrayOf(CArkAppItemBubbleBuilder, AbstractQQCustomMenuItem)),
    OnMenuBuilder {

    data class AppConfig(
        var packageNames: List<String>,
        var regex: String,
        var parseShortUrls: Boolean
    ) : Serializable

    override val name: String = "小程序/分享卡片跳转APP"
    override val valueState: MutableStateFlow<String?> by lazy {
        MutableStateFlow(if (isEnabled) "已开启" else "禁用")
    }
    override val description: String = "长按小程序/分享卡片增加直接跳转到APP的菜单项，支持配置多个应用"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY

    override val onUiItemClickListener: (IUiItemAgent, Activity, View) -> Unit = { _, activity, _ ->
        showConfigDialog(activity)
    }

    private val appConfigsData = ConfigData<MutableList<AppConfig>>("MiniAppDirectJump_appConfigs")
    private var appConfigs: MutableList<AppConfig>
        get() = appConfigsData.getOrDefault(
            mutableListOf(
                AppConfig(
                    listOf("tv.danmaku.bili", "com.bilibili.app.in", "tv.danmaku.bilibilihd", "com.bilibili.app.blue"),
                    """https?:\\?/\\?/((b23\.tv)|([\w.]*bilibili\.com))[^"]*""",
                    true
                ),
                AppConfig(
                    listOf("com.xingin.xhs"),
                    """(?<="jumpUrl":")https?:\\?/\\?/[^"]*""",
                    false
                ),
                AppConfig(
                    listOf("com.coolapk.market"),
                    """(?<="jumpUrl":")https?:\\?/\\?/[^"]*""",
                    false
                ),
                AppConfig(
                    listOf("com.zhihu.android"),
                    """(?<="jumpUrl":")https?:\\?/\\?/[^"]*""",
                    false
                )
            )
        )
        set(value) {
            appConfigsData.value = value
        }

    @SuppressLint("SetTextI18n")
    private fun showConfigDialog(ctx: Context) {
        val currEnabled = isEnabled
        val configsView = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (16 * ctx.resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        val funcSwitch = SwitchCompat(ctx).apply {
            isChecked = currEnabled
            textSize = 16f
            text = "功能开关 (开启后才生效)"
        }
        configsView.addView(funcSwitch)


        val appConfigsContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        configsView.addView(appConfigsContainer)

        fun createAppConfigView(config: AppConfig? = null): View {
            return LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 16, 0, 16)
                }
                layoutParams = params

                addView(View(ctx).apply {
                    setBackgroundColor(Color.LTGRAY)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                    )
                })

                val packageNameEdit = EditText(ctx).apply {
                    setText(config?.packageNames?.joinToString(",") ?: "")
                    hint = "应用包名（英文逗号分隔，优先检查的放在前面）"
                    textSize = 14f
                }
                addView(
                    packageNameEdit, LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                )

                val regexEdit = EditText(ctx).apply {
                    setText(config?.regex ?: "")
                    hint = "匹配正则表达式（可从卡片消息json字符串中获取链接）"
                    textSize = 14f
                }
                addView(regexEdit)

                val parseShortLinksSwitch = SwitchCompat(ctx).apply {
                    isChecked = config?.parseShortUrls ?: false
                    textSize = 14f
                    text = "解析短链接还原"
                }
                addView(parseShortLinksSwitch)


                val deleteButton = Button(ctx).apply {
                    text = "删除"
                    setOnClickListener {
                        appConfigsContainer.removeView(this@apply.parent as View)
                    }
                }
                addView(deleteButton)

                tag = object {
                    fun getConfig() = AppConfig(
                        packageNameEdit.text.toString()
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() },
                        regexEdit.text.toString(),
                        parseShortLinksSwitch.isChecked
                    )
                }
            }
        }

        appConfigs.forEach { config ->
            appConfigsContainer.addView(createAppConfigView(config))
        }

        val addButton = Button(ctx).apply {
            text = "添加应用配置"
            setOnClickListener {
                appConfigsContainer.addView(createAppConfigView())
            }
        }
        configsView.addView(addButton)

        val scrollView = ScrollView(ctx).apply {
            addView(configsView)
        }

        AlertDialog.Builder(ctx).apply {
            setTitle("跳转配置")
            setView(scrollView)
            setCancelable(false)
            setPositiveButton("确定") { _, _ ->
                val newEnabled = funcSwitch.isChecked
                if (newEnabled != currEnabled) {
                    isEnabled = newEnabled
                    valueState.value = if (newEnabled) "已开启" else "禁用"
                }

                val newAppConfigs = mutableListOf<AppConfig>()
                for (i in 0 until appConfigsContainer.childCount) {
                    val view = appConfigsContainer.getChildAt(i)
                    val config = (view.tag as? Any)?.let {
                        it.javaClass.getDeclaredMethod("getConfig").invoke(it) as? AppConfig
                    } ?: continue
                    if (config.packageNames.isNotEmpty() && config.regex.isNotBlank()) {
                        newAppConfigs.add(config)
                    }
                }
                appConfigs = newAppConfigs

                Toasts.success(ctx, "已保存配置")
            }
            setNegativeButton("取消", null)
            show()
        }
    }

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
            toApp(ctx, text)
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

    private fun toApp(ctx: Activity, text: String) = MainScope().launch {
        var foundValidApp = false
        for (appConfig in appConfigs) {
            try {
                val regex = appConfig.regex.toRegex()
                val matchResult = regex.find(text) ?: continue
                val url = matchResult.value.replace("\\/", "/")
                val isShortLink = matchResult.groupValues.getOrNull(2)?.isNotEmpty() ?: false

                val resolvedUrl = if (isShortLink && appConfig.parseShortUrls) {
                    Toasts.info(ctx, "解析短链接中")
                    withContext(Dispatchers.IO) { resolveShortUrl(url) }
                } else url

                for (packageName in appConfig.packageNames) {
                    if (!isPackageInstalled(ctx, packageName)) {
                        continue
                    }

                    try {
                        val uri = Uri.parse(resolvedUrl)
                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                            setPackage(packageName)
                        }
                        if (ctx.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()) {
                            ctx.startActivity(intent)
                            foundValidApp = true
                            break
                        }
                    } catch (e: ActivityNotFoundException) {
                        continue
                    }
                }
                if (foundValidApp) break
            } catch (e: Exception) {
                continue
            }
        }
        if (!foundValidApp) Toasts.info(ctx, "未找到可打开的应用")
    }

    private fun isPackageInstalled(ctx: Context, packageName: String) = try {
        ctx.packageManager.getApplicationInfo(packageName, 0).enabled
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    override val targetComponentTypes = arrayOf("com.tencent.mobileqq.aio.msglist.holder.component.ark.AIOArkContentComponent")

    override fun onGetMenuNt(msg: Any, componentType: String, param: MethodHookParam) {
        if (!isEnabled) return
        val ctx = ContextUtils.getCurrentActivity()
        val item = createItemIconNt(msg, "打开", R.drawable.ic_item_open_72dp, R.id.item_jump_to_app) {
            val element = (msg.javaClass.declaredMethods.first {
                it.returnType == MsgElement::class.java && it.parameterTypes.isEmpty()
            }.apply { isAccessible = true }.invoke(msg) as MsgElement).arkElement
            toApp(ctx, element.bytesData)
        }
        val list = param.result as MutableList<Any>
        list.add(1, item)
    }
}