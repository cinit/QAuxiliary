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

package cc.ioctl.hook.troop

import cc.ioctl.util.hookBeforeIfEnabled
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.dexkit.BaseListenTogetherPanel_onUIModuleNeedRefresh
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.requireMinQQVersion

@FunctionHookEntry
@UiItemAgentEntry
object HideListenTogetherPanel : CommonSwitchFunctionHook(
    arrayOf(BaseListenTogetherPanel_onUIModuleNeedRefresh)
) {

    override val name = "隐藏一起听歌"

    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Simplify.CHAT_GROUP_TITLE

    override val isAvailable: Boolean get() = requireMinQQVersion(QQVersion.QQ_8_5_0)

    override fun initOnce(): Boolean {
        // com.tencent.mobileqq.listentogether.ui.BaseListenTogetherPanel
        // onUIModuleNeedRefresh(com.tencent.mobileqq.listentogether.ListenTogetherSession)V
        val onUIModuleNeedRefresh = DexKit.requireMethodFromCache(BaseListenTogetherPanel_onUIModuleNeedRefresh)
        hookBeforeIfEnabled(onUIModuleNeedRefresh) {
            it.result = null
        }
        return true
    }

}
