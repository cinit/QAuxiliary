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

package me.ketal.hook

import android.app.Activity
import android.content.Context
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.RuntimeErrorTracer
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.bridge.AppRuntimeHelper
import io.github.qauxv.core.HookInstaller
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.dsl.uiClickableItem
import io.github.qauxv.hook.BaseFunctionHook
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.NProfileCardUtil_getCard
import me.ketal.data.ConfigData
import me.ketal.ui.view.ConfigView
import me.ketal.util.ignoreResult
import xyz.nextalone.util.get
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.set
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object FakeQQLevel : BaseFunctionHook("Ketal_FakeQQLevel",
    targets = arrayOf(NProfileCardUtil_getCard)) {

    override val uiItemAgent: IUiItemAgent by lazy {
        uiClickableItem {
            title = "自定义QQ等级"
            summary = "仅本地生效"
            onClickListener = { _, view, _ ->
                showDialog(view)
            }
        }
    }

    override val uiItemLocation = FunctionEntryRouter.Locations.Entertainment.ENTERTAIN_CATEGORY
    override val runtimeErrorDependentComponents: List<RuntimeErrorTracer>? = null

    private val levelKey = ConfigData<String>("Ketal_FakeQQLevel_level")
    private var level: String
        get() = levelKey.getOrDefault("255")
        set(value) {
            levelKey.value = value
        }

    fun listener(activity: Activity) = View.OnClickListener {
        showDialog(activity)
    }

    private fun showDialog(ctx: Context) {
        throwOrTrue {
            val vg = ConfigView(ctx)
            val dialog = MaterialDialog(ctx).show {
                title(text = "自定义QQ等级")
                input(hint = "自定义QQ等级...", prefill = level, waitForPositiveButton = false) { dialog, text ->
                    val inputField = dialog.getInputField()
                    dialog.setActionButtonEnabled(WhichButton.POSITIVE, try {
                        val level = text.toString().toInt()
                        if (level < 1 || level > 1000) {
                            inputField.error = "无效数值，仅可以输入1-1000范围内的数字"
                            false
                        } else {
                            inputField.error = null
                            true
                        }
                    } catch (e: NumberFormatException) {
                        inputField.error = "请输入有效的数据"
                        false
                    })
                }.ignoreResult()
                positiveButton(text = "保存") {
                    val text = getInputField().text.toString()
                    val enableFake = vg.isChecked
                    isEnabled = enableFake
                    if (enableFake) {
                        level = text
                        if (!isInitialized) HookInstaller.initializeHookForeground(context, this@FakeQQLevel)
                    }
                    dismiss()
                }
                negativeButton(text = "取消")
            }
            vg.setText("启用自定义QQ等级")
            vg.view = dialog.getCustomView()
            vg.isChecked = isEnabled
            dialog.view.contentLayout.customView = null
            dialog.customView(view = vg)
        }
    }

    override fun initOnce() = throwOrTrue {
        DexKit.requireMethodFromCache(NProfileCardUtil_getCard).hookAfter(this) {
            if (it.result.get("uin") == AppRuntimeHelper.getAccount()) {
                it.result.set("iQQLevel", level.toInt())
            }
        }
    }
}
