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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import cc.ioctl.util.LayoutHelper
import io.github.qauxv.BuildConfig
import io.github.qauxv.R
import io.github.qauxv.base.IDynamicHook
import io.github.qauxv.base.IUiItemAgentProvider
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.util.Log
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.hostInfo
import xyz.nextalone.util.SystemServiceUtils

class FuncStatusDetailsFragment : BaseRootLayoutFragment() {

    private var mFunction: IUiItemAgentProvider? = null
    private var mTextDetails: String? = null

    override fun getTitle() = "功能详情"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        val args = arguments
        val target: String? = args?.getString(TARGET_IDENTIFIER)
        if (target == null) {
            finishFragment()
            return
        }
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

    override fun doOnCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val ctx = inflater.context
        val func = mFunction ?: return null
        mTextDetails = dumpStatus(func)
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
        return if (item.itemId == R.id.menu_item_copy_all) {
            mTextDetails?.let {
                val ctx = requireContext()
                if (!mTextDetails.isNullOrEmpty()) {
                    SystemServiceUtils.copyToClipboard(ctx, it)
                    Toasts.show(ctx, "已复制到剪贴板")
                }
            }
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun dumpStatus(func: IUiItemAgentProvider): String {
        val sb = StringBuilder()
        sb.append(BuildConfig.VERSION_NAME).append("\n")
        sb.append(hostInfo.hostName).append(hostInfo.versionName).append('(').append(hostInfo.versionCode).append(')').append('\n')
        sb.apply {
            append(func.javaClass.name).append("\n")
            if (func is IDynamicHook) {
                val h: IDynamicHook = func
                append("isInitialized: ").append(h.isInitialized).append("\n")
                append("isInitializationSuccessful: ").append(h.isInitializationSuccessful).append("\n")
                append("isEnabled: ").append(h.isEnabled).append("\n")
                append("isAvailable: ").append(h.isAvailable).append("\n")
                append("isPreparationRequired: ").append(h.isPreparationRequired).append("\n")
                val errors: List<Throwable> = h.runtimeErrors
                append("errors: ").append(errors.size).append("\n")
                for (error in errors) {
                    append(Log.getStackTraceString(error)).append("\n")
                }
            }
        }
        return sb.toString()
    }

    companion object {
        const val TARGET_IDENTIFIER = "FuncStatusDetailsFragment.TARGET_IDENTIFIER"

        @JvmStatic
        fun newInstance(targetUiAgentId: String): SettingsMainFragment {
            val fragment = SettingsMainFragment()
            val bundle = getBundleForLocation(targetUiAgentId)
            fragment.arguments = bundle
            return fragment
        }

        @JvmStatic
        fun getBundleForLocation(targetUiAgentId: String): Bundle {
            if (targetUiAgentId.isEmpty()) {
                throw IllegalArgumentException("targetUiAgentId can not be empty")
            }
            val bundle = Bundle()
            bundle.putString(TARGET_IDENTIFIER, targetUiAgentId)
            return bundle
        }
    }
}
