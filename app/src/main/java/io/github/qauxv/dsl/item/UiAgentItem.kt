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

package io.github.qauxv.dsl.item

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import io.github.qauxv.base.IDynamicHook
import io.github.qauxv.base.ISwitchCellAgent
import io.github.qauxv.base.IUiItemAgentProvider
import io.github.qauxv.base.RuntimeErrorTracer
import io.github.qauxv.core.HookInstaller
import io.github.qauxv.dsl.cell.TitleValueCell
import io.github.qauxv.dsl.func.IDslItemNode
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.hostInfo

class UiAgentItem(
        override val identifier: String,
        override val name: String,
        val agentProvider: IUiItemAgentProvider,
) : IDslItemNode, TMsgListItem {

    override val isSearchable: Boolean = true
    override val isClickable: Boolean get() = isEnabled
    override val isEnabled: Boolean
        get() {
            val agent = agentProvider.uiItemAgent
            return agent.validator?.invoke(agent) ?: true
        }

    override val isVoidBackground: Boolean = false

    class HeaderViewHolder(cell: TitleValueCell) : RecyclerView.ViewHolder(cell)

    override fun createViewHolder(context: Context, parent: ViewGroup): RecyclerView.ViewHolder {
        return HeaderViewHolder(TitleValueCell(context))
    }

    private val mCheckChangedListener = CompoundButton.OnCheckedChangeListener { btn, isChecked ->
        val agent = agentProvider.uiItemAgent
        val funcName = agent.titleProvider.invoke(agent)
        val switchCellAgent = agent.switchProvider
        val unsupported = agentProvider is IDynamicHook && !agentProvider.isAvailable
        switchCellAgent?.isChecked = isChecked
        val action = {
            // if the function is enabled but not initialized, initialize it
            if (agentProvider is IDynamicHook) {
                val hook: IDynamicHook = agentProvider
                val context = btn.context
                if (hook.isEnabled && !hook.isInitialized) {
                    // we need to initialize the hook
                    HookInstaller.initializeHookForeground(context, hook)
                }
                if (hook.isApplicationRestartRequired) {
                    Toasts.info(context, "重启 ${hostInfo.hostName} 生效")
                }
            }
        }
        if (unsupported && isChecked) {
            val ctx = CommonContextWrapper.createAppCompatContext(btn.context)
            // confirm
            AlertDialog.Builder(ctx)
                .setTitle("不支持的功能")
                .setMessage("此功能（$funcName）暂不支持在 ${hostInfo.hostName} ${hostInfo.versionName}(${hostInfo.versionCode32}) 上使用，仍然要开启吗？")
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    action()
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    btn.isChecked = false
                }
                .setOnCancelListener {
                    btn.isChecked = false
                }
                .setCancelable(true)
                .show()
        } else {
            action()
        }
    }

    private val mOnClickListener = View.OnClickListener {
        onItemClick(it, -1, -1, -1)
    }

    override fun bindView(viewHolder: RecyclerView.ViewHolder, position: Int, context: Context) {
        // remove the listener first to avoid mess up
        val cell = viewHolder.itemView as TitleValueCell
        cell.setOnClickListener(null)
        cell.switchView.setOnCheckedChangeListener(null)
        val agent = agentProvider.uiItemAgent
        cell.title = agent.titleProvider.invoke(agent)
        val description: CharSequence? = agent.summaryProvider?.invoke(agent, context)
        val valueState = agent.valueState
        // value state observers are registered in the fragment, we only need to update the value
        val valueStateValue: String? = valueState?.value
        val switchAgent: ISwitchCellAgent? = agent.switchProvider
        val hasError = if (agentProvider is IDynamicHook) {
            val hook: IDynamicHook = agentProvider
            (hook.isInitialized && !hook.isInitializationSuccessful) || run {
                if (hook is RuntimeErrorTracer) {
                    hook.hasRuntimeErrors
                } else {
                    hook.runtimeErrors.isNotEmpty()
                }
            }
        } else false
        cell.hasError = hasError
        if (switchAgent != null) {
            // has switch!!, must not both have a switch and a value
            var toBeShownAtSummary: CharSequence? = valueState?.value
            if (valueStateValue.isNullOrEmpty()) {
                toBeShownAtSummary = description
            }
            cell.summary = if (toBeShownAtSummary.isNullOrEmpty()) null else toBeShownAtSummary
            cell.isHasSwitch = true
            cell.isChecked = switchAgent.isChecked
            cell.switchView.isEnabled = switchAgent.isCheckable
            cell.switchView.isClickable = switchAgent.isCheckable
            cell.switchView.setOnCheckedChangeListener(mCheckChangedListener)
        } else {
            // simple case, as it is
            cell.isHasSwitch = false
            cell.summary = description
            cell.value = valueStateValue
        }
        cell.setOnClickListener(mOnClickListener)
    }

    override fun onItemClick(v: View, position: Int, x: Int, y: Int) {
        val agent = agentProvider.uiItemAgent
        val cell = v as TitleValueCell
        // TODO: 2022-02-09 if ClassCastException, it means the context is not Activity use base.context
        val activity: Activity = v.context as Activity
        val onClick = agent.onClickListener
        if (onClick != null) {
            onClick.invoke(agent, activity, v)
        } else {
            // check if it has switch
            if (cell.isHasSwitch) {
                cell.switchView.toggle()
            }
        }
    }

    override val isLongClickable: Boolean = false

    override fun onLongClick(v: View, position: Int, x: Int, y: Int): Boolean {
        // nop
        return false
    }
}
