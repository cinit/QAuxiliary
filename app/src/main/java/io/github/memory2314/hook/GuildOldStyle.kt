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

package io.github.memory2314.hook

import cc.ioctl.util.hookBeforeIfEnabled
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.requireRangeQQVersion
import xyz.nextalone.util.invoke
import xyz.nextalone.util.method
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object GuildOldStyle : CommonSwitchFunctionHook() {
    override val name = "频道旧版样式"
    override val description = "仅支持QQ9.0.15~9.0.73版本"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.GUILD_CATEGORY
    override val isAvailable = requireRangeQQVersion(QQVersion.QQ_9_0_15, QQVersion.QQ_9_0_73)

    override fun initOnce() = throwOrTrue {
        if (requireMinQQVersion(QQVersion.QQ_9_0_30)) { // 9.0.30~9.0.73(之后的版本获取布局方法不存在)
            hookBeforeIfEnabled("Lcom/tencent/mobileqq/guild/mainframe/GuildFragmentDelegateFrame;->getCurrentMainFragment()Lcom/tencent/mobileqq/guild/mainframe/AbsGuildMainFragment;".method) {
                it.result = it.thisObject.invoke("getMainFrameFragment")
            }
        } else { // (第一个新版频道)9.0.15~9.0.25
            hookBeforeIfEnabled("Lcom/tencent/mobileqq/guild/mainframe/GuildFragmentDelegateFrame;->s()Lcom/tencent/mobileqq/guild/mainframe/AbsGuildMainFragment;".method) {
                it.result = it.thisObject.invoke("u")
            }
        }
    }
}