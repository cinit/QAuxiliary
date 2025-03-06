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
import android.view.View
import io.github.qauxv.activity.SettingsUiFragmentHostActivity
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.chainloader.detail.ExternalModuleManager
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.util.SyncUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@FunctionHookEntry
@UiItemAgentEntry
object ExternalModuleConfigHook : CommonConfigFunctionHook(SyncUtils.PROC_ANY) {

    override val name: String = "加载外部插件"
    override val description: CharSequence = "加载兼容 QAuxiliary 私有 API 的第三方模块插件"

    // not used here
    override fun initOnce() = true
    override var isEnabled: Boolean
        get() = true
        set(value) {}
    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY
    private val mStateFlow by lazy { MutableStateFlow(getStateFlowText()) }
    override val valueState: StateFlow<String?> by lazy { mStateFlow }

    private fun getStateFlowText(): String {
        val size = ExternalModuleManager.getActiveExternalModules().size
        return if (size == 0) {
            "无"
        } else {
            "已启用 $size 个"
        }
    }

    fun notifyStateChanged() {
        mStateFlow.value = getStateFlowText()
    }

    override val onUiItemClickListener: (IUiItemAgent, Activity, View) -> Unit = { _, activity, _ ->
        SettingsUiFragmentHostActivity.startFragmentWithContext(activity, ExternalModuleConfigFragment::class.java)
    }

}
