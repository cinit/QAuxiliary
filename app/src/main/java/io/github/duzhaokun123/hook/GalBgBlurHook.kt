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
import android.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.widget.doAfterTextChanged
import com.github.kyuubiran.ezxhelper.utils.findMethod
import io.github.duzhaokun123.util.blurBackground
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.util.SyncUtils
import kotlinx.coroutines.flow.MutableStateFlow
import me.kyuubiran.util.getExFriendCfg
import xyz.nextalone.util.clazz
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.putExFriend

@FunctionHookEntry
@UiItemAgentEntry
object PicBgBlurHook: CommonConfigFunctionHook(SyncUtils.PROC_PEAK) {
    val brCfg = "pic_bg_blur_radius"
    override val name: String
        get() = "聊天界面查看图片背景模糊"
    override val valueState: MutableStateFlow<String?>?
        get() = null
    override val onUiItemClickListener: (IUiItemAgent, Activity, View) -> Unit
        get() = { _, activity, _ ->
            val ll = LinearLayout(activity)
            ll.apply {
                orientation = LinearLayout.VERTICAL
                addView(CheckBox(activity).apply {
                    isChecked = this@PicBgBlurHook.isEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        this@PicBgBlurHook.isEnabled = isChecked
                    }
                    text = "使能"
                }, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                addView(EditText(activity).apply {
                    setText(getExFriendCfg()!!.getIntOrDefault(brCfg, 10).toString())
                    hint = "模糊半径"
                    doAfterTextChanged { t ->
                        t ?: return@doAfterTextChanged
                        putExFriend(brCfg, t.toString().toIntOrNull() ?: 0)
                    }
                }, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            AlertDialog.Builder(activity)
                .setView(ll)
                .show()
        }

    override fun initOnce(): Boolean {
        "com.tencent.mobileqq.richmediabrowser.AIOGalleryActivity".clazz!!.findMethod {
            name == "onCreate"
        }.hookAfter(this) {
            val activity = it.thisObject as Activity
            activity.window.blurBackground(getExFriendCfg()!!.getIntOrDefault(brCfg, 10))
        }
        return true
    }

    override val uiItemLocation: Array<String>
        get() = FunctionEntryRouter.Locations.Simplify.CHAT_OTHER
}
