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

package io.github.duzhaokun123.hook

import android.content.Context
import android.os.Parcelable
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import io.github.qauxv.base.RuntimeErrorTracer
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.router.decorator.IBaseChatPieInitDecorator
import io.github.qauxv.router.dispacher.InputButtonHookDispatcher
import io.github.qauxv.util.Log
import mqq.app.AppRuntime

@FunctionHookEntry
@UiItemAgentEntry
object InputPlusButtonHook : CommonSwitchFunctionHook(), IBaseChatPieInitDecorator {
    override val name = "输入框长按切换加号菜单"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY
    override val runtimeErrorDependentComponents: List<RuntimeErrorTracer>?
        get() = null

    override fun initOnce(): Boolean {
        return InputButtonHookDispatcher.INSTANCE.initialize()
    }

    override fun onInitBaseChatPie(
        baseChatPie: Any,
        aioRootView: ViewGroup,
        session: Parcelable?,
        ctx: Context,
        rt: AppRuntime
    ) {
        Log.d("here")
        val input = aioRootView.findViewById<EditText>(ctx.resources.getIdentifier("input", "id", ctx.packageName))
        val originalCallback = input.customInsertionActionModeCallback
        input.customInsertionActionModeCallback = object : ActionMode.Callback {
            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                if (originalCallback.onActionItemClicked(mode, item)) {
                    return true
                }
                if (item.title == "加号菜单") {
                    val funBtn = aioRootView.findViewById<ImageButton>(ctx.resources.getIdentifier("fun_btn", "id", ctx.packageName))
                    funBtn.callOnClick()
                    return true
                }
                return false
            }

            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                originalCallback.onCreateActionMode(mode, menu)
                menu.add("加号菜单")
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                originalCallback.onDestroyActionMode(mode)
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                return originalCallback.onPrepareActionMode(mode, menu)
            }

        }
    }
}