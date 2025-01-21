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

package com.xiaoniu.hook

import android.widget.TextView
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Auxiliary
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.TIMVersion
import io.github.qauxv.util.requireMinTimVersion
import xyz.nextalone.util.clazz
import xyz.nextalone.util.get
import xyz.nextalone.util.hookAfterAllConstructors
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object TimProfileSelectable : CommonSwitchFunctionHook() {

    override val name = "TIM账号资料文字可选中"
    override val description = "解决资料卡无法复制QQ号的问题"
    override val uiItemLocation = Auxiliary.PROFILE_CATEGORY
    override val isAvailable = requireMinTimVersion(TIMVersion.TIM_3_1_1)

    override fun initOnce() = throwOrTrue {
        if (requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA)) {
            val clazz = "com.tencent.mobileqq.profilecard.ProfileCellView".clazz!!
            clazz.hookAfterAllConstructors { param ->
                val textView = param.thisObject.get("mTvProfileContent") as TextView
                textView.setTextIsSelectable(true)
            }
        } else {
            val clazz = "com.tencent.tim.activity.profile.ProfileCellView".clazz!!
            val field = clazz.declaredFields.last { it.type == TextView::class.java }.apply { isAccessible = true }
            clazz.hookAfterAllConstructors { param ->
                val textView = field.get(param.thisObject) as TextView
                textView.setTextIsSelectable(true)
            }
        }
    }
}