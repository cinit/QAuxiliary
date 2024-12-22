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

package io.github.duzhaokun123.hook

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import io.github.duzhaokun123.util.blurBackground
import io.github.qauxv.R
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.SyncUtils
import xyz.nextalone.util.hookAfter
import kotlin.properties.Delegates

@FunctionHookEntry
@UiItemAgentEntry
object GalBgBlurHook : CommonConfigFunctionHook(SyncUtils.PROC_PEAK + SyncUtils.PROC_MAIN) {
    private const val brCfg = "gal_bg_blur_radius"
    private const val bdCfg = "gal_bg_dim"
    override val name = "聊天界面查看图片背景模糊"
    override val description: CharSequence
        get() = "需要 Android 12+ 并启用 允许窗口级模糊处理 (ro.surface_flinger.supports_background_blur=1)"
    override val valueState = null
    override val isAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    var brValue by Delegates.observable(ConfigManager.getDefaultConfig().getIntOrDefault(brCfg, 10)) { _, _, newValue ->
        ConfigManager.getDefaultConfig().putInt(brCfg, newValue)
    }
    var bdValue by Delegates.observable(ConfigManager.getDefaultConfig().getFloat(bdCfg, 0.1F)) { _, _, newValue ->
        ConfigManager.getDefaultConfig().putFloat(bdCfg, newValue)
    }
    var window: Window? = null

    override val onUiItemClickListener: (IUiItemAgent, Activity, View) -> Unit
        get() = { _, activity, _ ->
            val ctx = CommonContextWrapper.createMaterialDesignContext(activity)
            val ll = LinearLayout(ctx)
            ll.apply {
                orientation = LinearLayout.VERTICAL
                addView(CheckBox(activity).apply {
                    isChecked = this@GalBgBlurHook.isEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        this@GalBgBlurHook.isEnabled = isChecked
                    }
                    text = "聊天界面查看图片背景模糊"
                }, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                addView(TextView(ctx).apply {
                    text = "当前界面看不到模糊说明系统不支持"
                })
                addView(TextView(ctx).apply {
                    setTextColor(ctx.getColor(R.color.firstTextColor))
                    text = "模糊半径"
                })
                addView(com.google.android.material.textfield.TextInputEditText(ctx).apply {
                    setText(brValue.toString())
                    hint = "默认 10"
                    doAfterTextChanged { t ->
                        t ?: return@doAfterTextChanged
                        brValue = t.toString().toIntOrNull() ?: 0
                    }
                }, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                addView(TextView(ctx).apply {
                    setTextColor(ctx.getColor(R.color.firstTextColor))
                    text = "暗淡系数"
                })
                addView(com.google.android.material.textfield.TextInputEditText(ctx).apply {
                    setText(bdValue.toString())
                    hint = "默认 0.1"
                    doAfterTextChanged { t ->
                        t ?: return@doAfterTextChanged
                        bdValue = t.toString().toFloatOrNull() ?: 0F
                    }
                }, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            AlertDialog.Builder(ctx)
                .setView(ll)
                .show()
                .also {
                    it.window?.blurBackground(brValue, bdValue)
                }
        }

    override fun initOnce(): Boolean {
        Activity::class.java.hookAfter(this, "onCreate", Bundle::class.java) {
            val activity = it.thisObject as Activity
            when (activity::class.java.name) {
                "com.tencent.mobileqq.richmediabrowser.AIOGalleryActivity",
                "com.tencent.mobileqq.activity.aio.photo.AIOGalleryActivity",
                "com.tencent.mobileqq.activity.photo.album.NewPhotoPreviewActivity",
                "com.tencent.richframework.gallery.QQGalleryActivity" -> {
                    activity.window.blurBackground(
                        ConfigManager.getDefaultConfig().getIntOrDefault(brCfg, 10),
                        ConfigManager.getDefaultConfig().getFloat(bdCfg, 0.1F)
                    )
                }
            }
        }
        return true
    }

    override val uiItemLocation: Array<String>
        get() = FunctionEntryRouter.Locations.Simplify.CHAT_OTHER
}
