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

package io.github.qauxv.chainloader.detail.ui

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import cc.ioctl.util.LayoutHelper.MATCH_PARENT
import cc.ioctl.util.ui.doWithFaultyDialogWithoutErrorHandling
import cc.ioctl.util.ui.dsl.RecyclerListViewController
import io.github.qauxv.R
import io.github.qauxv.base.IEntityAgent
import io.github.qauxv.base.ISwitchCellAgent
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.IUiItemAgentProvider
import io.github.qauxv.chainloader.detail.ExternalModuleManager
import io.github.qauxv.databinding.DialogExternalModuleInfoBinding
import io.github.qauxv.dsl.item.CategoryItem
import io.github.qauxv.dsl.item.DescriptionItem
import io.github.qauxv.dsl.item.DslTMsgListItemInflatable
import io.github.qauxv.dsl.item.UiAgentItem
import io.github.qauxv.fragment.BaseRootLayoutFragment
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.IoUtils
import io.github.qauxv.util.Log
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException


class ExternalModuleConfigFragment : BaseRootLayoutFragment() {

    interface IUiItemAgentWithProvider : IUiItemAgent, IUiItemAgentProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "配置外部模块"
    }

    private var mDslListViewController: RecyclerListViewController? = null

    private var mGeneration = 1

    override fun doOnCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = inflater.context
        mDslListViewController = RecyclerListViewController(context, lifecycleScope)
        mDslListViewController!!.initAdapter()
        mDslListViewController!!.initRecyclerListView()
        val recyclerView = mDslListViewController!!.recyclerListView!!
        recyclerView.apply {
            id = R.id.fragmentMainRecyclerView
        }
        val rootView: FrameLayout = FrameLayout(context).apply {
            addView(mDslListViewController!!.recyclerListView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }
        rootLayoutView = recyclerView
        return rootView
    }

    override fun onResume() {
        super.onResume()
        mDslListViewController?.items = collectDslItemHierarchy()
    }

    private fun refreshDslList() {
        // only if the fragment is >= resumed
        if (isResumed) {
            mDslListViewController?.items = collectDslItemHierarchy()
        }
    }

    override fun onPause() {
        mGeneration++
        super.onPause()
    }

    private fun collectDslItemHierarchy(): Array<DslTMsgListItemInflatable> {
        val currentGeneration = mGeneration
        val ctx = requireContext()
        val items = ArrayList<DslTMsgListItemInflatable>()
        try {
            val modules = ExternalModuleManager.loadExternalModuleInfoList()
            items += CategoryItem("外部模块列表") {
                if (modules.isEmpty()) {
                    description("当前没有任何外部模块")
                } else {
                    for (info in modules) {
                        val agent = makeIUiItemAgent(ctx, currentGeneration, info)
                        add(
                            UiAgentItem(
                                identifier = info.packageName,
                                name = getAppLabelName(info.packageName),
                                agentProvider = agent,
                            )
                        )
                    }
                    description("此处列出了所有已启用的外部模块插件，您可以在此处查看和管理它们。")
                }
            }
            items += CategoryItem("添加外部模块") {
                textItem("添加外部模块", "从已安装的应用添加外部模块", onClick = {
                    showConfigExternalModuleDialog(null)
                })
                description("您需要先将外部模块安装到手机上，然后将其包名和证书 SHA-256 列表添加到此处，才能启用外部模块。")
            }
        } catch (e: Exception) {
            items += DescriptionItem("加载外部模块信息失败: \n${Log.getStackTraceString(e)}", textIsSelectable = true)
        }
        return items.toTypedArray()
    }

    private fun makeIUiItemAgent(ctx: Context, generation: Int, info: ExternalModuleManager.ExternalModuleInfo): IUiItemAgentWithProvider {
        val appName = getAppLabelName(info.packageName)
        return object : IUiItemAgentWithProvider {
            override val valueState: StateFlow<String?>? = null
            override val validator: ((IUiItemAgent) -> Boolean) = { _ -> true }
            override val switchProvider: ISwitchCellAgent = object : ISwitchCellAgent {
                override val isCheckable: Boolean = true
                override var isChecked: Boolean
                    get() = info.enable
                    set(value) {
                        check(generation == mGeneration) { "Fragment generation changed, activity recreation?" }
                        doWithFaultyDialogWithoutErrorHandling(ctx) {
                            updateSingleExternalModuleEnableState(info, value)
                        }
                        refreshDslList()
                    }
            }
            override val onClickListener: ((IUiItemAgent, Activity, View) -> Unit) = { _, activity, _ ->
                check(generation == mGeneration) { "Fragment generation changed, activity recreation?" }
                showConfigExternalModuleDialog(info)
            }
            override val extraSearchKeywordProvider: ((IUiItemAgent, Context) -> Array<String>?)? = null
            override val titleProvider: (IEntityAgent) -> String = { appName }
            override val summaryProvider: ((IEntityAgent, Context) -> CharSequence?) = { _, _ -> info.packageName }
            override val uiItemAgent: IUiItemAgent get() = this
            override val uiItemLocation: Array<String> = arrayOf()
        }
    }

    @Throws(IllegalArgumentException::class)
    private fun normalizeCertificateSha256HexLowerCharsArray(
        inputText: String
    ): Array<String>? {
        // split with \n or \r\n or \r, or ';', or ','
        var lines = inputText.split(Regex("[\n\r;,；，]+")).map { it.trim() }
            .filter { it.isNotEmpty() }
        // we expect it in lower case hex format, however, user may input base64 ones.
        lines = lines.map {
            if (it.length == 44) {
                IoUtils.bytesToHex(Base64.decode(it, 0), false)
            } else {
                it.lowercase().replace(":", "").replace("-", "").replace(" ", "")
            }
        }
        // check if all lines are valid SHA-256 hex strings, /[a-f0-9]{64}/
        if (lines.any { it.length != 64 || !it.matches(Regex("[a-f0-9]{64}")) }) {
            return null
        }
        // remove duplicates
        lines = lines.distinct()
        // return as an array
        return lines.toTypedArray()
    }

    private fun showConfigExternalModuleDialog(current: ExternalModuleManager.ExternalModuleInfo?) {
        val ctx = CommonContextWrapper.createAppCompatContext(requireContext())
        val vb = DialogExternalModuleInfoBinding.inflate(LayoutInflater.from(ctx))
        val builder = AlertDialog.Builder(ctx)
            .setTitle(if (current == null) "添加外部模块" else "编辑外部模块")
            .setView(vb.root)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
        if (current != null) {
            builder.setNeutralButton("删除") { _, _ ->
                doWithFaultyDialogWithoutErrorHandling(ctx) {
                    updateSingleExternalModuleConfig(current, true)
                }
                refreshDslList()
            }
        }
        // set up the view info
        vb.itemExternalModulePackageName.setText(current?.packageName ?: "")
        // cannot change package name
        vb.itemExternalModulePackageName.isEnabled = (current == null)
        vb.itemExternalModuleCertSha256.setText(current?.certificateSha256HexLowerCharsArray?.joinToString("\n") ?: "")
        // typically user wants to enable the module by default for new modules
        vb.itemExternalModuleEnable.isChecked = current?.enable ?: true
        // set up the listeners
        val dialog = builder.create()
        val positiveButtonOnClickListener = View.OnClickListener {
            val packageName = vb.itemExternalModulePackageName.text.toString().trim()
            if (packageName.isEmpty()) {
                vb.itemExternalModulePackageName.error = "包名不能为空"
                vb.itemExternalModulePackageName.requestFocus()
                return@OnClickListener
            }
            // /[a-zA-Z0-9_.-]+/
            if (!packageName.matches(Regex("[a-zA-Z0-9_.]+"))) {
                vb.itemExternalModulePackageName.error = "包名格式错误"
                vb.itemExternalModulePackageName.requestFocus()
                return@OnClickListener
            }
            val certsText = vb.itemExternalModuleCertSha256.text.toString().trim()
            if (certsText.isEmpty()) {
                vb.itemExternalModuleCertSha256.error = "证书 SHA-256 列表不能为空"
                vb.itemExternalModuleCertSha256.requestFocus()
                return@OnClickListener
            }
            val normalizedCerts: Array<String>?
            try {
                normalizedCerts = normalizeCertificateSha256HexLowerCharsArray(certsText)
            } catch (e: RuntimeException) {
                vb.itemExternalModuleCertSha256.error = e.message ?: "证书 SHA-256 列表格式错误"
                vb.itemExternalModuleCertSha256.requestFocus()
                return@OnClickListener
            }
            if (normalizedCerts == null) {
                vb.itemExternalModuleCertSha256.error = "证书 SHA-256 列表格式错误"
                vb.itemExternalModuleCertSha256.requestFocus()
                return@OnClickListener
            }
            val info = ExternalModuleManager.ExternalModuleInfo(
                enable = vb.itemExternalModuleEnable.isChecked,
                packageName = packageName,
                certificateSha256HexLowerCharsArray = normalizedCerts
            )
            doWithFaultyDialogWithoutErrorHandling(ctx) {
                updateSingleExternalModuleConfig(info, false)
            }
            dialog.dismiss()
            refreshDslList()
        }
        // show the dialog
        dialog.setOnCancelListener {
            // reset the error state
            vb.itemExternalModulePackageName.error = null
            vb.itemExternalModuleCertSha256.error = null
        }
        dialog.show()
        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        dialog.setOnShowListener {
            // set the positive button to be enabled
            positiveButton.isEnabled = true
        }
        positiveButton.setOnClickListener(positiveButtonOnClickListener)
    }

    @Throws(IOException::class, IllegalArgumentException::class)
    private fun updateSingleExternalModuleConfig(info: ExternalModuleManager.ExternalModuleInfo, isRemove: Boolean) {
        val currentList = ExternalModuleManager.loadExternalModuleInfoList().toMutableList()
        if (isRemove) {
            currentList.removeIf { it.packageName == info.packageName }
        } else {
            val index = currentList.indexOfFirst { it.packageName == info.packageName }
            if (index >= 0) {
                currentList[index] = info
            } else {
                currentList.add(info)
            }
        }
        ExternalModuleManager.saveExternalModuleInfoList(currentList.toTypedArray())
        refreshDslList()
    }

    @Throws(IOException::class, IllegalArgumentException::class)
    private fun updateSingleExternalModuleEnableState(info: ExternalModuleManager.ExternalModuleInfo, enable: Boolean) {
        // clone the info to avoid modifying the original object
        val clonedInfo = info.copy(enable = enable)
        updateSingleExternalModuleConfig(clonedInfo, false)
    }

    private val mAppLabelNameCaches = HashMap<String, String>()

    @UiThread
    fun getAppLabelName(packageName: String): String {
        if (mAppLabelNameCaches.containsKey(packageName)) {
            return mAppLabelNameCaches[packageName]!!
        }
        try {
            val appLabelName = requireContext().packageManager.getApplicationLabel(
                requireContext().packageManager.getApplicationInfo(packageName, 0)
            ).toString()
            mAppLabelNameCaches[packageName] = appLabelName
            return appLabelName
        } catch (e: NameNotFoundException) {
            // fallback to package name
            return packageName
        }
    }

}
