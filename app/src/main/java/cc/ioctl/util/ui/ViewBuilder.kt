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

@file:JvmName("ViewBuilder")

package cc.ioctl.util.ui

import android.content.Context
import android.widget.CompoundButton
import io.github.qauxv.base.IDynamicHook
import io.github.qauxv.core.HookInstaller
import io.github.qauxv.dsl.cell.TitleValueCell

fun newListItemSwitch(ctx: Context, title: String, desc: String?, on: Boolean, enabled: Boolean,
                      listener: CompoundButton.OnCheckedChangeListener?): TitleValueCell {
    val root = TitleValueCell(ctx).apply {
        this.title = title
        summary = desc
        isHasSwitch = true
        isChecked = on
        hasDivider = true
        isEnabled = enabled
        switchView.isClickable = true
        switchView.setOnCheckedChangeListener(listener)
    }
    return root
}

fun newListItemSwitch(ctx: Context, title: String, desc: String?, on: Boolean,
                      listener: CompoundButton.OnCheckedChangeListener?): TitleValueCell {
    return newListItemSwitch(ctx, title, desc, on, true, listener)
}

fun newListItemHookSwitchInit(ctx: Context, title: String, desc: String?, hook: IDynamicHook): TitleValueCell {
    val on: Boolean = hook.isEnabled
    return newListItemSwitch(ctx, title, desc, on) { _: CompoundButton, isChecked: Boolean ->
        if (!hook.isInitialized && isChecked) {
            hook.isEnabled = true
            HookInstaller.initializeHookForeground(ctx, hook)
        } else {
            hook.isEnabled = isChecked
        }
    }
}
