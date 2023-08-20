/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
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

package cc.microblock.hook

import android.view.View
import cc.hicore.hook.ReplyMsgWithImg
import cc.ioctl.util.HookUtils
import cc.ioctl.util.Reflex
import com.github.kyuubiran.ezxhelper.utils.getObject
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import xyz.nextalone.util.get
import android.widget.RelativeLayout;
import com.github.kyuubiran.ezxhelper.utils.getObjectAs

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

}
