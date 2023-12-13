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
package xyz.nextalone.base

import android.app.Activity
import android.content.DialogInterface
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import cc.ioctl.util.HostStyledViewBuilder
import cc.ioctl.util.LayoutHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.qauxv.R
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.Log
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.dexkit.DexKitTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import me.ketal.data.ConfigData

abstract class MultiItemDelayableHook(keyName: String,targets: Array<DexKitTarget>? = null) :
    CommonConfigFunctionHook(keyName,targets) {
    abstract val preferenceTitle: String
    open val preferenceSummary = ""

    override val name: String get() = preferenceTitle

    override val description: String?
        get() = preferenceSummary.ifEmpty { null }
    open val dialogDesc = "精简"
    override val valueState: MutableStateFlow<String?> by lazy {
        MutableStateFlow("已选择" + activeItems.size + "项")
    }

    override val onUiItemClickListener: (IUiItemAgent, Activity, View) -> Unit = { _, activity, _ ->
        // create a dialog
        val dialogContext = CommonContextWrapper.createMaterialDesignContext(activity)
        val builder = MaterialAlertDialogBuilder(dialogContext)
        alertDialogDecorator(builder)
        builder.show()
    }

    private val itemsConfigKeys = ConfigData<Set<String>>(keyName)
    private val allItemsConfigKeys = ConfigData<Set<String>>("$keyName\\_All")
    abstract val allItems: Set<String>
    open val defaultItems: Set<String> = setOf()
    open val enableCustom = true
    internal open var items
        get() = try {
            allItemsConfigKeys.getOrDefault(allItems)?.toMutableList() ?: mutableListOf()
        } catch (e: ClassCastException) {
            allItemsConfigKeys.remove()
            allItems.toMutableList()
        }
        set(value) {
            allItemsConfigKeys.value = value.toSet()
        }
    var activeItems
        get() = try {
            itemsConfigKeys.getOrDefault(defaultItems)?.toMutableList()?.filter {
                items.isEmpty() || it in items
            } ?: mutableListOf()
        } catch (e: ClassCastException) {
            itemsConfigKeys.remove()
            defaultItems.toMutableList()
        }
        set(value) {
            itemsConfigKeys.value = value.toSet()
            valueState.update { "已选择" + value.size + "项" }
        }

    open fun listener() = View.OnClickListener {
        try {
            MaterialAlertDialogBuilder(CommonContextWrapper.createMaterialDesignContext(it.context)).apply(alertDialogDecorator).show()
        } catch (e: Exception) {
            Log.e(e)
        }
    }

    override fun initOnce() = true

    override var isEnabled: Boolean
        get() = activeItems.isNotEmpty() && isAvailable
        set(value) {
            // unsupported operation
        }

    internal open fun getBoolAry(): BooleanArray {
        val ret = BooleanArray(items.size)
        for ((i, item) in items.withIndex()) {
            ret[i] = item in activeItems
        }
        return ret
    }

    private val alertDialogDecorator: MaterialAlertDialogBuilder.() -> Unit = {
        val cache = activeItems.toMutableList()
        setTitle("选择要${dialogDesc}的项目")
        setMultiChoiceItems(items.toTypedArray(), getBoolAry()) { _: DialogInterface, i: Int, _: Boolean ->
            val item = items[i]
            if (!cache.contains(item)) cache.add(item)
            else cache.remove(item)
        }
        setNegativeButton("取消", null)
        setPositiveButton("确定") { _: DialogInterface, _: Int ->
            Toasts.info(context, "已保存${dialogDesc}项目")
            activeItems = cache
        }
        if (enableCustom)
            setNeutralButton("自定义") { _: DialogInterface, _: Int ->
                val editText = EditText(context)
                editText.textSize = 16f
                val _5 = LayoutHelper.dip2px(context, 5f)
                editText.setPadding(_5 * 2, _5, _5 * 2, _5 * 2)
                editText.setText(items.joinToString("|"))
                val linearLayout = LinearLayout(context)
                linearLayout.orientation = LinearLayout.VERTICAL
                linearLayout.addView(
                    HostStyledViewBuilder.subtitle(context, "使用|分割，请确保格式正确！", Color.RED),
                    LayoutHelper.newLinearLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        _5 * 2, _5, _5 * 2, _5
                    )
                )
                linearLayout.addView(
                    editText,
                    LayoutHelper.newLinearLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        _5 * 2, _5, _5 * 2, _5
                    )
                )
                MaterialAlertDialogBuilder(context, R.style.MaterialDialog)
                    .setTitle("自定义${dialogDesc}项目")
                    .setView(linearLayout)
                    .setCancelable(true)
                    .setPositiveButton("确认") { _: DialogInterface, _: Int ->
                        val text = editText.text.toString()
                        if (text.isEmpty()) {
                            Toasts.error(context, "不可为空")
                            return@setPositiveButton
                        }
                        text.split("|").forEach { item ->
                            if (item.isEmpty()) {
                                Toasts.error(context, "请确保格式正确！")
                                return@setPositiveButton
                            }
                        }
                        Toasts.info(context, "已保存自定义项目")
                        allItemsConfigKeys.value = editText.text.split("|").toSet()
                    }
                    .setNegativeButton("取消", null)
                    .setNeutralButton("使用默认值") { _: DialogInterface, _: Int ->
                        allItemsConfigKeys.remove()
                        Toasts.info(context, "已使用默认值")
                    }
                    .show()
            }
    }
}
