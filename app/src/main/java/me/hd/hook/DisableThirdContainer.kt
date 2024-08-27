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

import android.view.View
import cc.ioctl.util.hookAfterIfEnabled
import com.github.kyuubiran.ezxhelper.utils.setViewZeroSize
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion

@FunctionHookEntry
@UiItemAgentEntry
object DisableThirdContainer : CommonSwitchFunctionHook() {

    override val name = "屏蔽悬浮广告(测试版)"
    override val description = "屏蔽消息页右上角 24年08月 开学季悬浮广告"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override fun initOnce(): Boolean {
        val thirdContainerClass = Initiator.loadClass(
            if (requireMinQQVersion(QQVersion.QQ_9_0_60))
                "com.tencent.qqnt.chats.core.ui.third.ThirdContainer"
            else
                "com.tencent.qqnt.chats.core.ui.d.e"
        )
        val setOnClickMethod = thirdContainerClass.getDeclaredMethod(
            "x",
            View::class.java,
            Initiator.loadClass("com.tencent.qqnt.chats.core.ui.third.const.ThirdViewEnum"),
            Map::class.java
        )
        hookAfterIfEnabled(setOnClickMethod) { param ->
            val view = param.args[0] as View
            view.setViewZeroSize()
        }
        return true
    }
}