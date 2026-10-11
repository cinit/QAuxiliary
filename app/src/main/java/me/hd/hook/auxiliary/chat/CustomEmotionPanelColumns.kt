/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2026 QAuxiliary developers
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

package me.hd.hook.auxiliary.chat

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.appcompat.app.AlertDialog
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import me.hd.util.hookAfterIfEnabled
import me.hd.util.hookBeforeIfEnabled
import me.hd.util.name
import me.hd.util.parameterCount
import me.hd.util.parameters
import me.hd.util.returnType
import me.hd.util.singleConstructor
import me.hd.util.singleField
import me.hd.util.singleMethod
import me.hd.util.toHostClass
import me.hd.util.type

@FunctionHookEntry
@UiItemAgentEntry
object CustomEmotionPanelColumns : CommonConfigFunctionHook() {

    override val name = "自定义表情面板列数"
    override val description = "自定义收藏与热图面板显示列数"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    private const val KEY_COLUMN_COUNT = "CustomEmotionPanelColumns.columnCount"
    private const val PANEL_TYPE_FAVORITE = 4 // 收藏表情面板
    private const val PANEL_TYPE_HOT_PIC = 12 // 热图面板
    private val TARGET_PANEL_TYPES = setOf(PANEL_TYPE_FAVORITE, PANEL_TYPE_HOT_PIC)

    private const val COLUMN_COUNT_DISABLED = 0
    private const val MIN_COLUMN_COUNT = 2
    private const val MAX_COLUMN_COUNT = 8
    private val selectableColumnCounts = intArrayOf(COLUMN_COUNT_DISABLED, 2, 3, 4, 5, 6, 7, 8)

    private fun getStateText(): String {
        val value = columnCount
        return if (value == COLUMN_COUNT_DISABLED) "禁用" else "${value}列"
    }

    override val valueState: MutableStateFlow<String?> by lazy {
        MutableStateFlow(getStateText())
    }

    private var columnCount: Int
        get() = ConfigManager.getDefaultConfig()
            .getIntOrDefault(KEY_COLUMN_COUNT, COLUMN_COUNT_DISABLED)
            .let { value ->
                if (value == COLUMN_COUNT_DISABLED) 0 else value.coerceIn(MIN_COLUMN_COUNT, MAX_COLUMN_COUNT)
            }
        set(value) {
            val safeValue = if (value == COLUMN_COUNT_DISABLED) 0 else value.coerceIn(MIN_COLUMN_COUNT, MAX_COLUMN_COUNT)
            ConfigManager.getDefaultConfig().apply {
                putInt(KEY_COLUMN_COUNT, safeValue)
                save()
            }
            valueState.update { getStateText() }
        }

    override var isEnabled: Boolean
        get() = columnCount in MIN_COLUMN_COUNT..MAX_COLUMN_COUNT
        set(value) {
            // ignore
        }

    override val onUiItemClickListener: (IUiItemAgent, Activity, View) -> Unit = { _, activity, _ ->
        val labels = selectableColumnCounts.map { value ->
            if (value == COLUMN_COUNT_DISABLED) "禁用" else "${value}列"
        }.toTypedArray()
        val checkedIndex = selectableColumnCounts.indexOf(columnCount).takeIf { it >= 0 } ?: 0
        AlertDialog.Builder(activity)
            .setTitle("选择表情面板列数")
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                columnCount = selectableColumnCounts[which]
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun initOnce(): Boolean {
        "com.tencent.mobileqq.emoticonview.EmotionPanelViewPagerAdapter".toHostClass()
            .singleMethod {
                returnType(Int::class.java) &&
                    name("getColumnNum") &&
                    parameterCount(2)
            }.hookBeforeIfEnabled(this) { param ->
                val panelInfo = param.args[1] ?: return@hookBeforeIfEnabled
                val panelType = panelInfo.singleField {
                    type(Int::class.java) &&
                        name("type")
                }.get(panelInfo) as Int
                if (panelType in TARGET_PANEL_TYPES) {
                    param.result = columnCount
                }
            }
        "com.tencent.mobileqq.emoticonview.FavoriteEmotionAdapter".toHostClass()
            .singleConstructor {
                parameters(null, null, Context::class.java, null, null, null, null)
            }.hookAfterIfEnabled(this) { param ->
                val adapter = param.thisObject
                // 收藏面板新UI会将单个格子宽度固定为64dp 关闭后才会自适应
                adapter.singleField {
                    type(Boolean::class.java) &&
                        name("isNewUISwitchOn")
                }.set(adapter, false)
            }
        return true
    }
}
