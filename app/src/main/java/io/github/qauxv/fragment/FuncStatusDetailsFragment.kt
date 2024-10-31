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

@file:Suppress("DEPRECATION")

package io.github.qauxv.fragment

import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.os.Process
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import cc.ioctl.util.LayoutHelper
import cc.ioctl.util.Reflex
import cc.ioctl.util.ui.FaultyDialog
import io.github.qauxv.BuildConfig
import io.github.qauxv.R
import io.github.qauxv.activity.ShadowShareFileAgentActivity
import io.github.qauxv.base.IDynamicHook
import io.github.qauxv.base.IUiItemAgentProvider
import io.github.qauxv.base.RuntimeErrorTracer
import io.github.qauxv.core.HookInstaller
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.util.Log
import io.github.qauxv.util.MemoryFileUtils
import io.github.qauxv.util.Natives
import io.github.qauxv.util.SafUtils
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.hostInfo
import xyz.nextalone.util.SystemServiceUtils
import java.io.IOException

class FuncStatusDetailsFragment : BaseRootLayoutFragment() {

    private var mFunction: IUiItemAgentProvider? = null
    private var mTextDetails: String? = null
    private var observerDialog: AlertDialog? = null
    private val observerPaths = HashSet<String>()
    private var mInitException: Throwable? = null

    private val observer = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            if (mInitException != null) {
                // special case...
                return
            }
            if (observerPaths.contains(uri?.path ?: "")) return
            observerDialog?.cancel()
            observerDialog = AlertDialog.Builder(requireActivity())
                .setTitle("嘿！请不要截图日志")
                .setMessage("由于截图的日志无法方便排查问题，请点击下方的“复制日志”按钮或在此界面右上角的复制按钮，将日志复制后进行反馈，感谢你的理解。")
                .setPositiveButton("复制日志") { _, _ -> copyDebugLog() }
                .setNegativeButton("取消", null)
                .create()
            observerDialog?.show()
            uri?.path?.let { observerPaths.add(it) }
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private fun copyDebugLog() {
        mTextDetails?.let {
            val ctx = requireContext()
            if (!mTextDetails.isNullOrEmpty()) {
                val copy = {
                    SystemServiceUtils.copyToClipboard(ctx, it)
                    Toasts.show(ctx, "已复制到剪贴板")
                }
                if (it.length > 1024) {
                    AlertDialog.Builder(ctx)
                        .setTitle("日志较长")
                        .setMessage("日志较长，建议使用文件方式分享（点击右上角的以文件分享按钮，或者保存为文件）")
                        .setPositiveButton("仍然复制") { _, _ -> copy() }
                        .setNegativeButton("取消", null)
                        .setCancelable(true)
                        .create()
                        .show()
                } else {
                    copy()
                }
            }
        }
    }

    private fun shareDebugLogAsFile() {
        mTextDetails?.let {
            val ctx = requireContext()
            if (!mTextDetails.isNullOrEmpty()) {
                val name = if (mInitException == null) {
                    "${Reflex.getShortClassName(mFunction)}-${System.currentTimeMillis()}.txt"
                } else {
                    "InitError-${System.currentTimeMillis()}.txt"
                }
                val bytes = it.toByteArray()
                var fd: Int = -1
                try {
                    fd = MemoryFileUtils.createMemoryFile(name, bytes.size)
                    val r = Natives.write(fd, bytes, 0, bytes.size)
                    if (r != bytes.size) {
                        throw IOException("write error, expected: ${bytes.size}, actual: $r")
                    }
                    Natives.lseek(fd, 0, 0)
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "text/plain"
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    val pfd = ParcelFileDescriptor.fromFd(fd)
                    ShadowShareFileAgentActivity.startShareFileActivity(ctx, intent, name, "text/plain", pfd, true)
                    Natives.close(fd)
                } catch (e: Exception) {
                    FaultyDialog.show(ctx, e)
                    runCatching {
                        if (fd != -1) {
                            Natives.close(fd)
                        }
                    }
                }
            }
        }
    }

    private fun saveDebugLogAsFile() {
        mTextDetails?.let { textDetails ->
            val ctx = requireContext()
            if (!mTextDetails.isNullOrEmpty()) {
                val name = if (mInitException == null) {
                    "${Reflex.getShortClassName(mFunction)}-${System.currentTimeMillis()}.txt"
                } else {
                    "InitError-${System.currentTimeMillis()}.txt"
                }
                try {
                    SafUtils.requestSaveFile(ctx)
                        .setMimeType("text/plain")
                        .setDefaultFileName(name)
                        .onResult { uri ->
                            try {
                                ctx.contentResolver.openOutputStream(uri)?.use { os ->
                                    val bytes = textDetails.toByteArray()
                                    os.write(bytes)
                                    os.flush()
                                    Toasts.show(ctx, "保存成功")
                                } ?: FaultyDialog.show(ctx, IOException("contentResolver.openOutputStream failed"))
                            } catch (e: Exception) {
                                FaultyDialog.show(ctx, e)
                            }
                        }
                        .commit()
                } catch (e: Exception) {
                    FaultyDialog.show(ctx, e)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        title = "功能详情"
        val args = arguments
        val target: String? = args?.getString(TARGET_IDENTIFIER)
        if (target == null) {
            finishFragment()
            return
        }
        if (target == TARGET_INIT_EXCEPTION) {
            mInitException = HookInstaller.getFuncInitException()
            if (mInitException == null) {
                Toasts.show(requireContext(), "没有初始化异常")
                finishFragment()
                return
            }
            subtitle = "初始化异常"
        } else {
            for (entry in FunctionEntryRouter.queryAnnotatedUiItemAgentEntries()) {
                if (entry.itemAgentProviderUniqueIdentifier == target) {
                    mFunction = entry
                    break
                }
            }
            if (mFunction == null) {
                Toasts.show(requireContext(), "未找到对应的功能: $target")
                finishFragment()
                return
            }
            subtitle = mFunction!!.uiItemAgent.let { it.titleProvider(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer
        )
    }

    override fun onPause() {
        super.onPause()
        requireActivity().contentResolver.unregisterContentObserver(observer)
    }

    override fun doOnCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val ctx = inflater.context
        mTextDetails = if (mInitException != null) {
            dumpInitException()
        } else {
            val func = mFunction ?: return null
            dumpStatus(func)
        }
        TextView(ctx).apply {
            text = mTextDetails
            textSize = 14f
            setTextIsSelectable(true)
            setTextColor(ResourcesCompat.getColor(resources, R.color.firstTextColor, ctx.theme))
            val dp16 = LayoutHelper.dip2px(ctx, 16f)
            setPadding(dp16, 0, dp16, dp16 / 2)
        }.also {
            return ScrollView(ctx).apply {
                addView(it, ViewGroup.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
                rootLayoutView = this
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.func_status_details, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_copy_all -> {
                copyDebugLog()
                true
            }

            R.id.menu_item_share_as_file -> {
                shareDebugLogAsFile()
                true
            }

            R.id.menu_item_save_as_file -> {
                saveDebugLogAsFile()
                true
            }

            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun dumpStatus(func: IUiItemAgentProvider) = buildString {
        append(BuildConfig.VERSION_NAME).append("\n")
        append(hostInfo.hostName).append(hostInfo.versionName)
        append('(').append(hostInfo.versionCode).append(')').append('\n')
        append("PID: ").append(Process.myPid()).append(", UID: ").append(Process.myUid()).append('\n')
        append(func.javaClass.name).append("\n")
        kotlin.runCatching {
            if (func is IDynamicHook) {
                val h: IDynamicHook = func
                append("isInitialized: ").append(h.isInitialized).append("\n")
                append("isInitializationSuccessful: ").append(h.isInitializationSuccessful).append("\n")
                append("isEnabled: ").append(h.isEnabled).append("\n")
                append("isAvailable: ").append(h.isAvailable).append("\n")
                append("isPreparationRequired: ").append(h.isPreparationRequired).append("\n")
                val errors: List<Throwable> = if (h is RuntimeErrorTracer) {
                    FuncStatListFragment.collectFunctionErrors(h).toList()
                } else {
                    h.runtimeErrors
                }
                append("errors: ").append(errors.size).append("\n")
                for (error in errors) {
                    append(Log.getStackTraceString(error)).append("\n")
                }
            }
        }.onFailure {
            append('\n').append("dumpStatus failed: ").append(Log.getStackTraceString(it)).append("\n")
        }
    }

    private fun dumpInitException() = buildString {
        append(BuildConfig.VERSION_NAME).append("\n")
        append(hostInfo.hostName).append(' ').append(hostInfo.versionName).append(' ')
        append('(').append(hostInfo.versionCode).append(')').append('\n')
        append("PID: ").append(Process.myPid()).append(", UID: ").append(Process.myUid()).append('\n')
        append(hostInfo.packageName).append("\n")
        kotlin.runCatching {
            val ex = mInitException
            if (ex != null) {
                append("InitException: ").append(ex.javaClass.name).append("\n")
                ex.cause?.let {
                    append(Log.getStackTraceString(it)).append("\n")
                }
                append(Log.getStackTraceString(ex)).append("\n")
            }
        }.onFailure {
            append('\n').append("dumpStatus failed: ").append(Log.getStackTraceString(it)).append("\n")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        observerDialog = null
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    companion object {
        const val TARGET_IDENTIFIER = "FuncStatusDetailsFragment.TARGET_IDENTIFIER"
        const val TARGET_INIT_EXCEPTION = "FuncStatusDetailsFragment.TARGET_FATAL_EXCEPTION"

        @JvmStatic
        fun newInstance(targetUiAgentId: String): SettingsMainFragment {
            val fragment = SettingsMainFragment()
            val bundle = getBundleForLocation(targetUiAgentId)
            fragment.arguments = bundle
            return fragment
        }

        @JvmStatic
        fun getBundleForLocation(targetUiAgentId: String): Bundle {
            require(targetUiAgentId.isNotEmpty()) { "targetUiAgentId can not be empty" }
            val bundle = Bundle()
            bundle.putString(TARGET_IDENTIFIER, targetUiAgentId)
            return bundle
        }
    }
}
