/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
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

package cc.ioctl.hook.experimental

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Environment
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import cc.ioctl.util.HostInfo
import cc.ioctl.util.hookAfterIfEnabled
import com.github.kyuubiran.ezxhelper.utils.Log
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.duzhaokun123.util.FilePicker
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.config.ConfigItems
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.dexkit.CAppConstants
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.xpcompat.XposedHelpers
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.lang.reflect.Field
import java.util.Locale

@FunctionHookEntry
@UiItemAgentEntry
object FileRecvRedirect : CommonConfigFunctionHook(SyncUtils.PROC_ANY and (SyncUtils.PROC_MSF or SyncUtils.PROC_UNITY or SyncUtils.PROC_MINI).inv(), arrayOf(CAppConstants)) {
    private var inited = false
    private lateinit var defaultCachePath: String
    private var targetField: Field? = null
    override val name: String
        get() = "下载文件重定向"
    override val valueState: StateFlow<String?>?
        get() = null
    override val onUiItemClickListener: (IUiItemAgent, Activity, View) -> Unit
        @SuppressLint("SetTextI18n")
        get() = { _, activity, _ ->
            val root = LinearLayout(activity)
            root.orientation = LinearLayout.VERTICAL

            val cb_enable = CheckBox(activity)
            cb_enable.text = "开启下载重定向"
            cb_enable.isChecked = isEnabled
            root.addView(cb_enable)

            val tv_note = TextView(activity)
            tv_note.text = "如果提示 目录无效 请检查是否已经给 ${HostInfo.getAppName()} 授予了读写权限"
            root.addView(tv_note)

            val ll_path = LinearLayout(activity)
            ll_path.orientation = LinearLayout.HORIZONTAL
            ll_path.visibility = if (cb_enable.isChecked) View.VISIBLE else View.GONE
            root.addView(ll_path)

            val et_path = EditText(activity)
            et_path.setText(getRedirectPath())
            ll_path.addView(et_path, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1F))

            val btn_select = Button(activity)
            btn_select.text = "..."
            ll_path.addView(btn_select, WRAP_CONTENT, WRAP_CONTENT)

            cb_enable.setOnCheckedChangeListener { _, isChecked ->
                ll_path.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
            btn_select.setOnClickListener {
                FilePicker.pickDir(activity, "选择下载文件重定向目录", Environment.getExternalStorageDirectory().absolutePath) { path ->
                    et_path.setText(path)
                }
            }

            MaterialAlertDialogBuilder(activity)
                .setTitle("下载文件重定向")
                .setPositiveButton("保存") { _, _ ->
                    val path = et_path.text.toString()
                    if (cb_enable.isChecked && checkPathAvailable(path).not()) {
                        Toasts.show(activity, "目录无效")
                        return@setPositiveButton
                    }
                    isEnabled = cb_enable.isChecked
                    if (isEnabled) {
                        setRedirectPathAndEnable(path)
                    }
                    Toasts.show(activity, "已保存,请重启 ${HostInfo.getAppName()}")
                }.setNegativeButton(android.R.string.cancel, null)
                .setView(root)
                .show()
        }

    override fun initOnce(): Boolean {
        defaultCachePath = getDefaultPath()
        val redirectPath = getRedirectPath()
        return if (redirectPath != null) {
            inited = doSetPath(redirectPath)
            inited
        } else {
            false
        }
    }

    override val uiItemLocation: Array<String>
        get() = FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY

    override var isEnabled: Boolean
        get() = ConfigManager.getDefaultConfig().getBooleanOrFalse(ConfigItems.qn_file_recv_redirect_enable)
        set(enabled) {
            val cfg = ConfigManager.getDefaultConfig()
            cfg.putBoolean(ConfigItems.qn_file_recv_redirect_enable, enabled)
            cfg.save()
            if (inited) {
                if (enabled) {
                    getRedirectPath()?.let { inited = doSetPath(it) }
                } else {
                    doSetPath(getDefaultPath())
                }
            }
        }

    private fun setRedirectPathAndEnable(path: String) {
        try {
            val cfg = ConfigManager.getDefaultConfig()
            cfg.putString(ConfigItems.qn_file_recv_redirect_path, path)
            cfg.putBoolean(ConfigItems.qn_file_recv_redirect_enable, true)
            cfg.save()
            inited = doSetPath(path)
        } catch (e: Exception) {
            Log.e(e)
        }
    }

    override val isInitializationSuccessful: Boolean
        get() = isInitialized

    override val isInitialized: Boolean
        get() = inited

    private fun getRedirectPath() =
        ConfigManager.getDefaultConfig().getString(ConfigItems.qn_file_recv_redirect_path)

    private fun doSetPath(path: String): Boolean {
        try {
            if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_2_8)) {
                if (inited.not()) {
                    hookAfterIfEnabled(XposedHelpers.findMethodBestMatch(Initiator.load("com.tencent.mobileqq.vfs.VFSAssistantUtils"), "getSDKPrivatePath", String::class.java)) { param ->
                        val result = param.result as String
                        val file = File(result)
                        if (file.exists() && file.isFile) return@hookAfterIfEnabled // 如果文件存在则不处理,防止已下载的文件出现异常
                        if (result.startsWith(defaultCachePath))
                            param.result = getRedirectPath() + result.substring(defaultCachePath.length)
                    }
                    try {
                        hookAfterIfEnabled(XposedHelpers.findMethodBestMatch(Initiator.load("com.tencent.guild.api.msg.impl.GuildMsgApiImpl"), "getNTKernelExtDataPath", *arrayOf<String>())) { param ->
                            param.result = HostInfo.getApplication().externalCacheDir!!.parentFile!!.absolutePath + "/Tencent/QQfile_recv/"
                        }
                    } catch (ignored: Exception) {
                    }
                }
            } else {
                val fields = DexKit.requireClassFromCache(CAppConstants).fields
                if (targetField == null) {
                    for (field in fields) {
                        field.isAccessible = true
                        val value = field.get(null)
                        val path = value!!.toString()
                        if (path.lowercase(Locale.ROOT).endsWith("file_recv/")) {
                            targetField = field
                            break
                        }
                    }
                }
                targetField?.isAccessible = true
                targetField?.set(null, path)
            }
            return true
        } catch (e: Exception) {
            Log.e(e)
            return false
        }
    }

    private fun getDefaultPath() =
        when {
            HostInfo.isTim() ->
                Environment.getExternalStorageDirectory().absolutePath + "/Tencent/TIMfile_recv/"

            HostInfo.requireMinQQVersion(QQVersion.QQ_8_2_8) ->
                HostInfo.getApplication().getExternalFilesDir(null)!!.parent!! + "/Tencent/QQfile_recv"

            else ->
                Environment.getExternalStorageDirectory().absolutePath + "/Tencent/QQfile_recv"
        }

    private fun checkPathAvailable(path: String): Boolean {
        val file = File(path)
        return file.exists() && file.isDirectory && file.canWrite()
    }
}