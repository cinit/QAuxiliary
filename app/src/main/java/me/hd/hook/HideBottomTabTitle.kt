/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2024 QAuxiliary developers
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

package me.hd.hook

import android.view.ViewGroup
import android.widget.TextView
import cc.ioctl.util.hookAfterIfEnabled
import com.github.kyuubiran.ezxhelper.utils.findViewByCondition
import com.github.kyuubiran.ezxhelper.utils.setViewZeroSize
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.util.method

@FunctionHookEntry
@UiItemAgentEntry
object HideBottomTabTitle : CommonSwitchFunctionHook() {

    override val name = "隐藏底栏标题"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.UI_MISC
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override fun initOnce(): Boolean {
        val generateTabItemMethod = if (requireMinQQVersion(QQVersion.QQ_9_0_30)) {
            "Lcom/tencent/mobileqq/activity/home/impl/TabFrameControllerImpl;->generateTabItem(IIIIILjava/lang/String;IIIILjava/lang/String;Ljava/lang/String;)Landroid/view/View;"
        } else {
            "Lcom/tencent/mobileqq/activity/home/impl/TabFrameControllerImpl;->generateTabItem(IIIIIIIIILjava/lang/String;Ljava/lang/String;)Landroid/view/View;"
        }.method
        hookAfterIfEnabled(generateTabItemMethod) { param ->
            val view = param.result as ViewGroup
            view.findViewByCondition {
                it is TextView
            }?.apply {
                setViewZeroSize()
            }
        }
        return true
    }
}