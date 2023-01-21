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
package me.ketal.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.LinearLayout
import androidx.core.view.plusAssign
import cc.ioctl.util.ui.newListItemHookSwitchInit
import cc.ioctl.util.ui.newListItemSwitch
import com.tencent.mobileqq.widget.BounceScrollView
import io.github.qauxv.fragment.BaseRootLayoutFragment
import me.ketal.hook.LeftSwipeReplyHook

@SuppressLint("Registered")
class ModifyLeftSwipeReplyFragment : BaseRootLayoutFragment() {

    override fun doOnCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        title = "修改消息左滑动作"
        val activity = settingsHostActivity!!
        val ll = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            val hook = LeftSwipeReplyHook
            this += newListItemHookSwitchInit(activity, "总开关", "打开后才可使用以下功能", hook)
            this += newListItemSwitch(
                activity, "取消消息左滑动作", "取消取消，一定要取消", hook.isNoAction
            ) { _: CompoundButton?, on: Boolean -> hook.isNoAction = on }
            this += newListItemSwitch(
                activity, "左滑多选消息", "娱乐功能，用途未知", hook.isMultiChose
            ) { _: CompoundButton?, on: Boolean -> hook.isMultiChose = on }
        }
        val rootView = BounceScrollView(activity, null).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            addView(ll, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        rootLayoutView = rootView
        return rootView
    }
}
