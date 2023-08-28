/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the qwq233 Universal License
 * as published on https://github.com/qwq233/license; either
 * version 2 of the License, or any later version and our EULA as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the qwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package cc.microblock.hook

import android.view.View
import android.widget.RelativeLayout
import cc.hicore.QApp.QAppUtils
import cc.ioctl.util.HookUtils
import cc.ioctl.util.Reflex
import com.github.kyuubiran.ezxhelper.utils.getObjectAs
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator

@FunctionHookEntry
@UiItemAgentEntry
object ForceEnableMultiForward : CommonSwitchFunctionHook() {
    override val name = "转发时强制开启多选用户/群组"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY

    override fun initOnce(): Boolean {
        HookUtils.hookAfterIfEnabled(
            this, Reflex.findMethod(
                Initiator.loadClass("com.tencent.mobileqq.activity.ForwardRecentActivity"), Void.TYPE, "initEntryHeaderView")
        ) {
            for(slot in arrayOf("friendLayout","contactLayout","troopDiscussionLayout","multiChatLayout"))
                it.thisObject.getObjectAs<RelativeLayout>(slot).visibility = View.VISIBLE;
        }
        return true;
    }

    override val isAvailable = QAppUtils.isQQnt();
}
