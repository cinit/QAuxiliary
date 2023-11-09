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

package com.xiaoniu.hook

import cc.hicore.QApp.QAppUtils
import cc.ioctl.util.hookBeforeIfEnabled
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion


@FunctionHookEntry
@UiItemAgentEntry
object DisableAddTroopToRecentUser : CommonSwitchFunctionHook() {

    override val name = "禁止群助手中的群有新帖时显示在主页"

    override val description = "给[有新帖]强提醒再砍一刀"

    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Simplify.MAIN_UI_MSG_LIST

    override val isAvailable: Boolean get() = requireMinQQVersion(QQVersion.QQ_8_9_23) && !QAppUtils.isQQnt()

    override fun initOnce(): Boolean {
        // Lcom/tencent/troopguild/api/impl/TroopGuildRecentServiceImpl;->addTroopToRecentUser(Ljava/lang/String;)V
        val kTroopGuildRecentServiceImpl = Initiator.loadClass("com.tencent.troopguild.api.impl.TroopGuildRecentServiceImpl")
        val addTroopToRecentUser = kTroopGuildRecentServiceImpl.getDeclaredMethod("addTroopToRecentUser", String::class.java)
        hookBeforeIfEnabled(addTroopToRecentUser) {
            it.result = null
        }
        return true
    }

}
