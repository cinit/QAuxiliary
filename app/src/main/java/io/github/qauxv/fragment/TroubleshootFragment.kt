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

package io.github.qauxv.fragment

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.os.MessageQueue
import android.text.SpannableStringBuilder
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import cc.ioctl.fragment.ExfriendListFragment
import cc.ioctl.util.ExfriendManager
import cc.ioctl.util.HostInfo
import cc.ioctl.util.LayoutHelper
import cc.ioctl.util.Reflex
import cc.ioctl.util.data.EventRecord
import cc.ioctl.util.data.FriendRecord
import cc.ioctl.util.ui.ThemeAttrUtils
import cc.ioctl.util.ui.dsl.RecyclerListViewController
import io.github.qauxv.R
import io.github.qauxv.activity.SettingsUiFragmentHostActivity
import io.github.qauxv.activity.SettingsUiFragmentHostActivity.Companion.createStartActivityForFragmentIntent
import io.github.qauxv.base.ISwitchCellAgent
import io.github.qauxv.bridge.AppRuntimeHelper.getLongAccountUin
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.config.SafeModeManager
import io.github.qauxv.dsl.item.CategoryItem
import io.github.qauxv.dsl.item.DslTMsgListItemInflatable
import io.github.qauxv.dsl.item.TextSwitchItem
import io.github.qauxv.lifecycle.ActProxyMgr
import io.github.qauxv.poststartup.StartupInfo
import io.github.qauxv.tlb.ConfigTable.cacheMap
import io.github.qauxv.ui.CustomDialog
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.LoaderExtensionHelper
import io.github.qauxv.util.Natives
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.DexKitTarget
import io.github.qauxv.util.dexkit.ordinal
import io.github.qauxv.util.dexkit.values
import io.github.qauxv.util.hostInfo
import io.github.qauxv.util.soloader.NativeLoader
import me.ketal.base.PluginDelayableHook
import me.singleneuron.hook.decorator.FxxkQQBrowser
import kotlin.system.exitProcess


class TroubleshootFragment : BaseRootLayoutFragment() {

    private var mDslListViewController: RecyclerListViewController? = null

    override fun doOnCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        title = "故障排查"
        val context = inflater.context
        mDslListViewController = RecyclerListViewController(context, lifecycleScope)
        mDslListViewController!!.items = hierarchy
        mDslListViewController!!.initAdapter()
        mDslListViewController!!.initRecyclerListView()
        val recyclerView = mDslListViewController!!.recyclerListView!!
        recyclerView.apply {
            id = R.id.fragmentMainRecyclerView
        }
        val rootView: FrameLayout = FrameLayout(context).apply {
            addView(mDslListViewController!!.recyclerListView, FrameLayout.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))
        }
        rootLayoutView = recyclerView
        return rootView
    }

    private val hierarchy: Array<DslTMsgListItemInflatable> by lazy {
        arrayOf(
            CategoryItem("安全模式") {
                add(TextSwitchItem(title = "启用安全模式", summary = "停用所有功能，重启应用后生效", switchAgent = mSafeModeSwitch))
                add(
                    TextSwitchItem(
                        title = "禁用 PluginDelayableHook",
                        summary = "(仅供调试) 暂时关闭全部 PluginDelayableHook 功能，重启应用后生效",
                        switchAgent = mDisablePluginHookSwitch
                    )
                )
            },
            CategoryItem("功能") {
                textItem("功能异常列表", null, onClick = clickToShowFuncList)
            },
            CategoryItem("缓存") {
                textItem("清除缓存", "清除模块缓存并重新计算适配数据", onClick = clickToClearCache)
            },
            CategoryItem("清除与重置（不可逆）") {
                textItem("重置模块设置", "不影响历史好友信息", onClick = clickToResetDefaultConfig)
                textItem("清除[已恢复]的历史记录", "删除当前账号下所有状态为[已恢复]的历史好友记录", onClick = clickToClearRecoveredFriends)
                textItem("清除所有的历史记录", "删除当前账号下所有的历史好友记录", onClick = clickToClearAllFriends)
                textItem("清除所有的ShortCuts", "清除MessagingStyle通知等功能产生的ShortCuts", onClick = clickToClearShortCuts)
            },
            CategoryItem("测试") {
                textItem("打开X5调试页面", "内置浏览器调试页面", onClick = clickToOpenX5DebugPage)
                textItem("打开内置浏览器", "使用内置浏览器打开指定页面", onClick = clickToOpenBrowser)
                textItem("打开 DebugActivity", null, onClick = clickToStartHostDebugActivity)
                textItem("打开指定 Activity", null, onClick = clickToStartActivity)
                textItem("测试通知", "点击测试通知", onClick = clickToTestNotification)
            },
            CategoryItem("异常与崩溃测试") {
                textItem("退出 Looper", "Looper.getMainLooper().quit() 没事别按", onClick = clickToTestCrashAction {
                    val looper = Looper.getMainLooper()
                    val queue = Reflex.getInstanceObject(looper, "mQueue", MessageQueue::class.java)
                    Reflex.setInstanceObject(queue, "mQuitAllowed", true)
                    looper.quit()
                })
                textItem("abort()", "没事别按", onClick = clickToTestCrashAction {
                    val libc = Natives.dlopen("libc.so", Natives.RTLD_NOLOAD)
                    if (libc == 0L) {
                        error("dlopen libc.so failed")
                    }
                    val abort = Natives.dlsym(libc, "abort")
                    if (abort == 0L) {
                        val msg = Natives.dlerror()
                        if (msg != null) {
                            error(msg)
                        } else {
                            error("dlsym 'abort' failed")
                        }
                    }
                    Natives.call(abort)
                })
                textItem("((void(*)())0)();", "空指针测试, 没事别按", onClick = clickToTestCrashAction {
                    Natives.call(0)
                })
                textItem("*((int*)0)=0;", "空指针测试, 没事别按", onClick = clickToTestCrashAction {
                    Natives.memset(0, 0, 1);
                })
            },
            CategoryItem("调试信息") {
                description(generateStatusText(), isTextSelectable = true)
                description(generateDebugInfo(), isTextSelectable = true)
            }
        )
    }

    private val mSafeModeSwitch: ISwitchCellAgent = object : ISwitchCellAgent {
        override val isCheckable = true
        override var isChecked: Boolean
            get() {
                return SafeModeManager.getManager().isEnabledForNextTime
            }
            set(value) {
                val oldValue = SafeModeManager.getManager().isEnabledForNextTime
                if (value != oldValue) {
                    val isSuccess = SafeModeManager.getManager().setEnabledForNextTime(value)
                    context?.let {
                        if (isSuccess) {
                            if (isResumed) Toasts.info(it, "重启应用后生效")
                        } else Toasts.error(it, "${if (value) "启用" else "禁用"}安全模式失败")
                    }
                }
            }
    }

    private val mDisablePluginHookSwitch: ISwitchCellAgent = object : ISwitchCellAgent {
        override val isCheckable = true
        override var isChecked: Boolean
            get() = PluginDelayableHook.disablePluginDelayableHook
            set(value) {
                PluginDelayableHook.disablePluginDelayableHook = value
            }
    }

    private val clickToShowFuncList: (View) -> Unit = {
        SettingsUiFragmentHostActivity.startFragmentWithContext(it.context, FuncStatListFragment::class.java, null)
    }

    private val clickToClearCache = confirmBeforeAction(
        "确认清除缓存, 并重新计算适配数据?\n点击确认后请等待3秒后手动重启" + hostInfo.hostName + "."
    ) {
        ConfigManager.getCache().apply {
            clear()
            save()
        }
        Thread.sleep(100)
        exitProcess(0)
    }

    private val clickToResetDefaultConfig = confirmTwiceBeforeAction(
        "此操作将删除该模块的所有配置信息,包括屏蔽通知的群列表,但不包括历史好友列表.点击确认后请等待3秒后手动重启" + hostInfo.hostName + ".\n此操作不可恢复",
        "删除所有配置信息"
    ) {
        ConfigManager.getCache().apply {
            clear()
            save()
        }
        ConfigManager.getDefaultConfig().apply {
            clear()
            save()
        }
        Thread.sleep(100)
        exitProcess(0)
    }

    private val clickToClearRecoveredFriends = confirmTwiceBeforeAction(
        """
            此操作将删除当前账号(${getLongAccountUin()})下的 已恢复 的历史好友记录(记录可单独删除).
            如果因 BUG 大量好友被标记为已删除, 请先刷新好友列表, 然后再点击此按钮.
            此操作不可恢复
            """.trimIndent(),
        "删除已恢复的好友记录"
    ) {
        val exm = ExfriendManager.getCurrent()
        val it: MutableIterator<*> = exm.events.entries.iterator()
        while (it.hasNext()) {
            val ev = (it.next() as Map.Entry<*, *>).value as EventRecord
            if (exm.persons[ev.operand]!!.friendStatus
                == FriendRecord.STATUS_FRIEND_MUTUAL
            ) {
                it.remove()
            }
        }
        exm.saveConfigure()
        Toasts.success(requireContext(), "操作成功")
    }

    private val clickToClearAllFriends = confirmTwiceBeforeAction(
        "此操作将删除当前账号(" + getLongAccountUin()
            + ")下的 全部 的历史好友记录, 通常您不需要进行此操作. \n" +
            "如果您的历史好友列表中因bug出现大量好友,请在联系人列表下拉刷新后点击 删除标记为已恢复的好友. \n" +
            "此操作不可恢复",
        "删除所有好友记录"
    ) {
        val uin = getLongAccountUin()
        if (uin < 10000) {
            throw IllegalStateException("uin $uin is invalid")
        }
        ConfigManager.forAccount(uin).apply {
            clear()
            save()
        }
        Thread.sleep(50)
        exitProcess(0)
    }

    private val clickToClearShortCuts = confirmBeforeAction("确定清除所有ShortCuts吗？") {
        ShortcutManagerCompat.removeAllDynamicShortcuts(
            HostInfo.getApplication().applicationContext
        )
        Toasts.success(requireContext(), "操作成功")
    }

    private fun confirmBeforeAction(confirmMessage: String, action: () -> Unit) = View.OnClickListener {
        val dialog = CustomDialog.createFailsafe(requireContext())
        dialog.setPositiveButton(android.R.string.ok) { _, _ ->
            try {
                action()
            } catch (e: Exception) {
                CustomDialog.createFailsafe(requireContext())
                    .setTitle(Reflex.getShortClassName(e))
                    .setCancelable(true)
                    .setMessage(e.toString())
                    .ok().show()
            }
        }
        dialog.setNegativeButton(android.R.string.cancel, null)
        dialog.setCancelable(true)
        dialog.setMessage(confirmMessage)
        dialog.setTitle("确认操作")
        dialog.show()
    }

    private fun confirmTwiceBeforeAction(
        confirmMessage: String,
        secondConfirmCheckBoxText: String,
        action: () -> Unit
    ) = View.OnClickListener {
        val ctx = requireContext()
        val builder = AlertDialog.Builder(ctx)
        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            try {
                action()
            } catch (e: Exception) {
                CustomDialog.createFailsafe(ctx)
                    .setTitle(Reflex.getShortClassName(e))
                    .setCancelable(true)
                    .setMessage(e.toString())
                    .ok().show()
            }
        }
        builder.setNegativeButton(android.R.string.cancel, null)
        builder.setCancelable(true)
        builder.setTitle("确认操作")
        // create a linear layout to hold the message and checkbox
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val padding = ctx.resources.getDimension(androidx.appcompat.R.dimen.abc_dialog_padding_material).toInt()
            setPadding(padding, padding / 3, padding, 0)
        }
        val message = AppCompatTextView(ctx).apply {
            text = confirmMessage
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(ResourcesCompat.getColor(resources, R.color.firstTextColor, ctx.theme))
        }
        val checkBox = AppCompatCheckBox(ctx).apply {
            text = secondConfirmCheckBoxText
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(ResourcesCompat.getColor(resources, R.color.firstTextColor, ctx.theme))
            isClickable = true
            isChecked = false
        }
        layout.addView(message)
        layout.addView(checkBox)
        builder.setView(layout)
        val dialog = builder.show()
        // get positive button and set listener
        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            positiveButton.isEnabled = isChecked
        }
        positiveButton.isEnabled = false
    }

    private fun actionOrShowError(action: () -> Unit) = View.OnClickListener {
        runOrShowError(action)
    }

    private fun runOrShowError(action: () -> Unit) {
        try {
            action()
        } catch (e: Throwable) {
            CustomDialog.createFailsafe(requireContext())
                .setTitle(Reflex.getShortClassName(e))
                .setCancelable(true)
                .setMessage(e.toString())
                .ok().show()
        }
    }

    private val clickToOpenX5DebugPage = actionOrShowError {
        val browser = Initiator.loadClass("com.tencent.mobileqq.activity.QQBrowserDelegationActivity")
        val intent = Intent(requireContext(), browser)
        intent.putExtra("fling_action_key", 2)
        intent.putExtra("fling_code_key", this@TroubleshootFragment.hashCode())
        intent.putExtra("useDefBackText", true)
        intent.putExtra("param_force_internal_browser", true)
        intent.putExtra("url", "http://debugx5.qq.com/")
        startActivity(intent)
    }

    private val clickToOpenBrowser = actionOrShowError {
        val ctx = requireContext()
        val browser = Initiator.loadClass("com.tencent.mobileqq.activity.QQBrowserDelegationActivity")
        val r = { url: String ->
            val intent = Intent(requireContext(), browser)
            intent.putExtra("fling_action_key", 2)
            intent.putExtra("fling_code_key", this@TroubleshootFragment.hashCode())
            intent.putExtra("useDefBackText", true)
            intent.putExtra("param_force_internal_browser", true)
            intent.putExtra("url", url)
            intent.putExtra(FxxkQQBrowser.EXTRA_BYPASS_FQB_HOOK, true)
            startActivity(intent)
        }
        val input = EditText(ctx).apply {
            id = R.id.input_value
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(ResourcesCompat.getColor(resources, R.color.firstTextColor, ctx.theme))
        }
        AlertDialog.Builder(ctx).apply {
            setTitle("请输入 URL")
            setCancelable(true)
            setNeutralButton(android.R.string.paste, null) // set listener later
            setPositiveButton(android.R.string.ok, null)  // set listener later
            setNegativeButton(android.R.string.cancel, null)
            setView(input)
        }.show().apply {
            getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                val clipSvc = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = clipSvc.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    input.setText(clip.getItemAt(0).text)
                }
            }
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val url = input.text.toString()
                if (url.isEmpty()) {
                    Toasts.error(ctx, "URL 不能为空")
                    return@setOnClickListener
                }
                if (!url.matches(Regex("[A-Za-z0-9]+?://.*"))) {
                    Toasts.error(ctx, "URL 不合法")
                    return@setOnClickListener
                }
                dismiss()
                r(url)
            }
        }
    }

    @SuppressLint("NotificationPermission")
    private val clickToTestNotification = actionOrShowError {
        val app = hostInfo.application
        val inner = createStartActivityForFragmentIntent(app, ExfriendListFragment::class.java, null)
        val wrapper = Intent()
        wrapper.setClassName(HostInfo.getApplication().packageName, ActProxyMgr.STUB_DEFAULT_ACTIVITY)
        wrapper.putExtra(ActProxyMgr.ACTIVITY_PROXY_INTENT, inner)
        val pi = PendingIntent.getActivity(HostInfo.getApplication(), 0, wrapper, PendingIntent.FLAG_IMMUTABLE)
        val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val n = ExfriendManager.getCurrent().createNotiComp(nm, "Ticker", "Title", "Content", longArrayOf(100, 200, 200, 100), pi)
        nm.notify(ExfriendManager.ID_EX_NOTIFY, n)
    }

    private var mCrashActionWarned = false

    private fun clickToTestCrashAction(action: () -> Unit): View.OnClickListener {
        return actionOrShowError {
            val ctx = requireContext()
            if (!mCrashActionWarned) {
                AlertDialog.Builder(ctx).apply {
                    setTitle("警告")
                    setMessage("此操作将会导致应用崩溃, 仅用于测试崩溃处理功能。\nPS: 经常崩溃容易造成聊天记录数据库损坏")
                    setCancelable(true)
                    setPositiveButton(android.R.string.ok, null)
                    setNegativeButton(android.R.string.cancel, null)
                }.show().apply {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        mCrashActionWarned = true
                        dismiss()
                        runOrShowError(action)
                    }
                }
            } else {
                action()
            }
        }
    }

    private val clickToStartHostDebugActivity = actionOrShowError {
        val browser = Initiator.loadClass("com.tencent.mobileqq.debug.DebugActivity")
        val intent = Intent(requireContext(), browser)
        startActivity(intent)
    }

    private val clickToStartActivity = actionOrShowError {
        val ctx = requireContext()
        val r = { url: String ->
            val browser = Initiator.loadClass(url)
            val intent = Intent(requireContext(), browser)
            startActivity(intent)
        }
        val input = EditText(ctx).apply {
            id = R.id.input_value
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(ResourcesCompat.getColor(resources, R.color.firstTextColor, ctx.theme))
        }
        AlertDialog.Builder(ctx).apply {
            setTitle("请输入 activity")
            setCancelable(true)
            setNeutralButton(android.R.string.paste, null) // set listener later
            setPositiveButton(android.R.string.ok, null)  // set listener later
            setNegativeButton(android.R.string.cancel, null)
            setView(input)
        }.show().apply {
            getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                val clipSvc = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = clipSvc.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    input.setText(clip.getItemAt(0).text)
                }
            }
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val url = input.text.toString()
                if (url.isEmpty()) {
                    Toasts.error(ctx, "Activity 不能为空")
                    return@setOnClickListener
                }
                dismiss()
                r(url)
            }
        }
    }

    private fun generateStatusText(): String {
        val loader = StartupInfo.getLoaderService()
        val hook = StartupInfo.requireHookBridge()
        var statusInfo = "PID: " + android.os.Process.myPid() +
            ", UID: " + android.os.Process.myUid() +
            ", ISA: " + NativeLoader.getIsaName(NativeLoader.getPrimaryNativeLibraryIsa()) +
            (if (NativeLoader.isSecondaryNativeLibraryNeeded(requireContext())) "+" +
                NativeLoader.getIsaName(NativeLoader.getSecondaryNativeLibraryIsa()) else "") + "\n" +
            "Xposed API version: " + hook.apiLevel + "\n" +
            "module: " + StartupInfo.getModulePath() + "\n" +
            "ctx.dataDir: " + hostInfo.application.dataDir + "\n"
        statusInfo += "entry: " + loader.entryPointName + "\n"
        var xp = loader.queryExtension("GetXposedBridgeClass") as Class<*>?
        if (xp == null) {
            xp = loader.queryExtension("GetXposedInterfaceClass") as Class<*>?
        }
        statusInfo += "XposedBridge: " + (if (xp != null) xp.name else "null") + "\n"
        statusInfo += hook.frameworkName + " " + hook.frameworkVersion + " (" + hook.frameworkVersionCode + ")\n"
        statusInfo += "Hook counter: " + LoaderExtensionHelper.getHookCounter();
        return statusInfo
    }

    private fun generateDebugInfo(): CharSequence {
        val ctx = requireContext()
        val colorError: Int = ThemeAttrUtils.resolveColorOrDefaultColorInt(ctx, R.attr.unusableColor, Color.RED)
        val colorNotice: Int = ThemeAttrUtils.resolveColorOrDefaultColorInt(ctx, androidx.appcompat.R.attr.colorAccent, Color.BLUE)
        val sb = SpannableStringBuilder()
        val targets = DexKitTarget.values
            .groupBy { it.findMethod }
        targets[false]?.forEach {
            kotlin.runCatching {
                val orig = it.declaringClass.replace("/", ".")
                val shortName: String = Reflex.getShortClassName(orig)
                var currName = "(void*)0"
                val md = DexKit.getMethodDescFromCacheImpl(it)
                if (md != null) {
                    currName = md.toString()
                } else {
                    val c = DexKit.loadClassFromCache(it)
                    if (c != null) {
                        currName = c.name
                    }
                }
                val text = "  [${it.ordinal}]$shortName\n$orig\n= $currName"
                when (currName) {
                    "(void*)0" -> sb.append(text, ForegroundColorSpan(colorNotice), SPAN_EXCLUSIVE_EXCLUSIVE)
                    DexKit.NO_SUCH_METHOD.toString() -> {
                        sb.append(text, ForegroundColorSpan(colorError), SPAN_EXCLUSIVE_EXCLUSIVE)
                    }

                    else -> sb.append(text)
                }
            }.onFailure { t ->
                sb.append("  [${it.ordinal}]$t", ForegroundColorSpan(colorError), SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            sb.append("\n")
        }

        targets[true]?.forEach {
            kotlin.runCatching {
                val orig = it.declaringClass.replace("/", ".")
                val shortName: String = Reflex.getShortClassName(orig)
                var currName = "(void*)0"
                val md = DexKit.getMethodDescFromCacheImpl(it)
                if (md != null) {
                    currName = md.toString()
                } else {
                    val c = DexKit.loadClassFromCache(it)
                    if (c != null) {
                        currName = c.name
                    }
                }
                val text = "  [${it.ordinal}]$shortName\n$orig\n= $currName"
                when (currName) {
                    "(void*)0" -> sb.append(text, ForegroundColorSpan(colorNotice), SPAN_EXCLUSIVE_EXCLUSIVE)
                    DexKit.NO_SUCH_METHOD.toString() -> {
                        sb.append(text, ForegroundColorSpan(colorError), SPAN_EXCLUSIVE_EXCLUSIVE)
                    }

                    else -> sb.append(text)
                }
            }.onFailure { t ->
                sb.append("  [${it.ordinal}]$t", ForegroundColorSpan(colorError), SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            sb.append("\n")
        }

        val set: Set<Map.Entry<String, Any?>> = cacheMap.entries
        var i = 40001
        for ((shortName, value) in set) {
            try {
                val currName = value.toString()
                sb.append("  [$i]$shortName\n$currName")
            } catch (e: java.lang.Exception) {
                sb.append("  [$i]$e", ForegroundColorSpan(colorError), SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            i++
            sb.append("\n")
        }
        return sb
    }
}
