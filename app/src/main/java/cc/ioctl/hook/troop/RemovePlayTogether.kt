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
package cc.ioctl.hook.troop

import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.isPlayQQ
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.tlb.ConfigTable
import xyz.nextalone.util.method
import xyz.nextalone.util.replace
import xyz.nextalone.util.throwOrTrue
import io.github.qauxv.util.PlayQQVersion
import io.github.qauxv.util.requireRangePlayQQVersion

//屏蔽群聊界面一起嗨
@FunctionHookEntry
@UiItemAgentEntry
object RemovePlayTogether : CommonSwitchFunctionHook() {

    override val name = "屏蔽群聊界面一起嗨"

    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.CHAT_GROUP_TITLE

    const val ClockInEntryHelper = "RemovePlayTogether.ClockInEntryHelper"
    const val TogetherControlHelper = "RemovePlayTogether.TogetherControlHelper"
    public override fun initOnce(): Boolean = throwOrTrue {
        if (isPlayQQ()) {
            if (requireRangePlayQQVersion(PlayQQVersion.PlayQQ_8_2_11, PlayQQVersion.PlayQQ_8_2_11))
                Initiator.loadClass("adhn").getDeclaredMethod("h").replace(this, result = false)
        } else {
            if (requireMinQQVersion(QQVersion.QQ_8_4_8)) {
                //QQ 8.4.8 除了一起嗨按钮，同一个位置还有一个群打卡按钮。默认显示群打卡，如果已经打卡就显示一起嗨，两个按钮点击之后都会打开同一个界面，但是要同时hook两个
                Initiator._ClockInEntryHelper()?.method(ConfigTable.getConfig(ClockInEntryHelper), 0, Boolean::class.java)?.replace(this, result = false)
            }
            Initiator._TogetherControlHelper()?.method(ConfigTable.getConfig(TogetherControlHelper), 0, Void.TYPE)?.replace(this, result = null)
        }
    }
}
