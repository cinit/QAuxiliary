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

import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import cc.ioctl.util.Reflex
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.DexMethodDescriptor
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.method
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object HideFriendCardSendGift : CommonSwitchFunctionHook() {

    override val name = "屏蔽好友资料页送礼物按钮"

    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.UI_MISC

    override val isAvailable: Boolean
        get() = requireMinQQVersion(QQVersion.QQ_8_0_0)

    override fun initOnce() = throwOrTrue {
        if (requireMinQQVersion(QQVersion.QQ_8_6_0)) {
            "Lcom/tencent/mobileqq/profilecard/base/container/ProfileBottomContainer;->initViews()V"
                .method.hookAfter(this) {
                    val rootView =
                        Reflex.getFirstNSFByType(it.thisObject, LinearLayout::class.java)
                    hideView(rootView)
                }
        } else {
            DexMethodDescriptor(Initiator._FriendProfileCardActivity(), "a", "(Landroid/widget/LinearLayout;)V")
                .getMethodInstance(Initiator.getHostClassLoader()).hookAfter(this) {
                    val rootView = it.args[0] as LinearLayout
                    hideView(rootView)
                }
        }
    }

    private fun hideView(rootView: LinearLayout) {
        val view = rootView[2]
        val child = (view as LinearLayout)[0]
        if (child is TextView) {
            child.doAfterTextChanged {
                if (!isEnabled) return@doAfterTextChanged
                if (it.toString() == "送礼物")
                    (child.parent as LinearLayout).isVisible = false
            }
        }
    }
}
