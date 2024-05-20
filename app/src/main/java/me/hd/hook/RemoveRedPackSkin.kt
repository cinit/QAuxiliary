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

import cc.ioctl.util.hookBeforeIfEnabled
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.Hd_RemoveRedPackSkin_Class
import io.github.qauxv.util.requireMinQQVersion

@FunctionHookEntry
@UiItemAgentEntry
object RemoveRedPackSkin : CommonSwitchFunctionHook(
    targets = arrayOf(Hd_RemoveRedPackSkin_Class)
) {

    override val name = "移除红包封皮"
    override val description = "移除群聊红包列表中的红包封皮"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.CHAT_GROUP_OTHER
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override fun initOnce(): Boolean {
        val panelClass = DexKit.requireClassFromCache(Hd_RemoveRedPackSkin_Class)
        val addListMethod = panelClass.declaredMethods.single { method ->
            val params = method.parameterTypes
            params.size == 2 && params[0] == Int::class.java && params[1] == List::class.java
        }
        hookBeforeIfEnabled(addListMethod) { param ->
            param.args[1] = emptyList<Any>()
        }
        return true
    }
}