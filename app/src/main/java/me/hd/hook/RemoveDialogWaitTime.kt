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

import android.content.Context
import android.view.View
import cc.ioctl.util.hookBeforeIfEnabled
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.requireMinQQVersion

@FunctionHookEntry
@UiItemAgentEntry
object RemoveDialogWaitTime : CommonSwitchFunctionHook(
    targetProc = SyncUtils.PROC_ANY
) {

    override val name = "移除弹窗等待时间"
    override val description = "移除弹窗中, 继续按钮的5s, 10s等待时间"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.UI_MISC
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override fun initOnce(): Boolean {
        val dialogUtilClass = Initiator.loadClass("com.tencent.mobileqq.utils.DialogUtil")
        val getEmoReplyMenuViewMethod = dialogUtilClass.getDeclaredMethod(
            "createCountdownDialog",
            Context::class.java,
            String::class.java,
            CharSequence::class.java,
            String::class.java,
            String::class.java,
            Boolean::class.java,
            Int::class.java,
            Int::class.java,
            View.OnClickListener::class.java,
            View.OnClickListener::class.java
        )
        hookBeforeIfEnabled(getEmoReplyMenuViewMethod) { param ->
            param.args[6] = 0
        }
        return true
    }
}