/*
 * QNotified - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 dmca@ioctl.cc
 * https://github.com/ferredoxin/QNotified
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/ferredoxin/QNotified/blob/master/LICENSE.md>.
 */
package me.singleneuron.hook.decorator

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.View
import io.github.qauxv.base.IEntityAgent
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.router.decorator.BaseDecorator
import io.github.qauxv.router.decorator.IStartActivityHookDecorator
import io.github.qauxv.router.dispacher.StartActivityHook
import io.github.qauxv.util.Log
import io.github.qauxv.util.xpcompat.XC_MethodHook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import me.singleneuron.activity.ChooseAgentActivity

@UiItemAgentEntry
@FunctionHookEntry
object ForceSystemAlbum : BaseDecorator(
    hookKey = "ForceSystemAlbum", defaultEnabled = false, dexDeobfIndexes = null
), IStartActivityHookDecorator {

    private const val CONFIG_KEY = "me.singleneuron.hook.decorator.ForceSystemAlbum.albumType"

    // 相册类型选项
    private val albumTypes = arrayOf("每次询问", "系统相册", "系统文档", "QQ 相册")

    val name = "可选使用系统相册"
    val description: CharSequence = "支持8.3.6及更高，点击选择默认相册类型"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.UI_CHAT_MSG
    override val dispatcher = StartActivityHook

    private val _valueState = MutableStateFlow(getCurrentAlbumTypeName())
    val valueState: StateFlow<String?> = _valueState

    private fun getCurrentAlbumType(): Int {
        return ConfigManager.getDefaultConfig().getIntOrDefault(CONFIG_KEY, 0)
    }

    private fun getCurrentAlbumTypeName(): String? {
        return if (isEnabled) albumTypes.getOrNull(getCurrentAlbumType()) else null
    }

    private fun setAlbumType(type: Int) {
        ConfigManager.getDefaultConfig().putInt(CONFIG_KEY, type)
        _valueState.value = getCurrentAlbumTypeName()
    }

    override val uiItemAgent: IUiItemAgent by lazy {
        object : IUiItemAgent {
            override val titleProvider: (IEntityAgent) -> String = { _ -> name }
            override val summaryProvider: ((IEntityAgent, Context) -> CharSequence?) = { _, _ -> description }
            override val valueState: StateFlow<String?>
                get() = this@ForceSystemAlbum.valueState
            override val validator: ((IUiItemAgent) -> Boolean) = { _ -> true }
            override val switchProvider = null
            override val onClickListener: ((IUiItemAgent, Activity, View) -> Unit) = { _, activity, _ ->
                val currentType = getCurrentAlbumType()
                AlertDialog.Builder(activity).setTitle("选择默认相册类型").setSingleChoiceItems(albumTypes, currentType) { dialog, which ->
                    setAlbumType(which)
                    isEnabled = true
                    dialog.dismiss()
                }.setNegativeButton(android.R.string.cancel, null).setNeutralButton("禁用") { _, _ ->
                    isEnabled = false
                    _valueState.value = null
                }.show()
            }
            override val extraSearchKeywordProvider: ((IUiItemAgent, Context) -> Array<String>?)? = null
        }
    }

    override fun onStartActivityIntent(intent: Intent, param: XC_MethodHook.MethodHookParam): Boolean {
        if (!isEnabled) return false

        val isWinkHomeActivity = intent.component?.className?.contains("WinkHomeActivity") == true
        val isNewPhotoListActivity = intent.component?.className?.contains("NewPhotoListActivity") == true
        if ((isWinkHomeActivity || (isNewPhotoListActivity && intent.getIntExtra(
                "uintype", -1
            ) != -1)) && (!intent.getBooleanExtra("PhotoConst.IS_CALL_IN_PLUGIN", false)) && (!intent.getBooleanExtra("is_decorated", false))
        ) {
            val uin = intent.getStringExtra("uin")
            if (uin.toString().length > 10) {
                // Filter out calls in the guild
                return true
            }
            val context = param.thisObject as Context
            Log.d("context: ${context.javaClass.name}")

            val albumType = getCurrentAlbumType()

            // 根据用户选择的默认类型处理
            when (albumType) {
                0 -> {
                    // 每次询问 - 显示选择对话框
                    showAlbumSelectionDialog(context, intent, param)
                    return true
                }

                1 -> {
                    // 系统相册
                    val systemAlbumIntent = Intent(context, ChooseAgentActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra("use_ACTION_PICK", true)
                        putExtras(intent)
                        type = "image/*"
                    }
                    context.startActivity(systemAlbumIntent)
                    param.result = null
                    return true
                }

                2 -> {
                    // 系统文档
                    val systemDocIntent = Intent(context, ChooseAgentActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtras(intent)
                        type = "image/*"
                    }
                    context.startActivity(systemDocIntent)
                    param.result = null
                    return true
                }

                3 -> {
                    // QQ 相册 - 不拦截，让原始 intent 继续
                    intent.putExtra("is_decorated", true)
                    return false
                }
            }
        }
        return false
    }

    private fun showAlbumSelectionDialog(context: Context, intent: Intent, param: XC_MethodHook.MethodHookParam) {
        val activityMap = mapOf("系统相册" to Intent(context, ChooseAgentActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("use_ACTION_PICK", true)
            putExtras(intent)
            type = "image/*"
        }, "系统文档" to Intent(context, ChooseAgentActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtras(intent)
            type = "image/*"
        }, "QQ 相册" to intent.apply {
            putExtra("is_decorated", true)
        })

        // 使用 AlertDialog 而不是 MaterialAlertDialogBuilder，因为 context 可能不是 Activity
        AlertDialog.Builder(context).setTitle("选择相册").setItems(activityMap.keys.toTypedArray()) { _, i ->
            context.startActivity(activityMap[activityMap.keys.toTypedArray()[i]])
        }.create().show()
        param.result = null
    }
}
