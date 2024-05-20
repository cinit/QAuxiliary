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
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.util.isPublic

@FunctionHookEntry
@UiItemAgentEntry
object RemoveSelectedMedia : CommonSwitchFunctionHook() {

    override val name = "移除选择媒体限制"
    override val description = "移除最多只能选择20张图片/视频限制"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override fun initOnce(): Boolean {
        val selectedMediaVMClass = Initiator.loadClass("com.tencent.qqnt.qbasealbum.select.viewmodel.SelectedMediaViewModel")
        val ifNumMethod = selectedMediaVMClass.declaredMethods.single { method ->
            method.isPublic && method.parameterTypes.isEmpty() && method.returnType == Boolean::class.java
        }
        hookBeforeIfEnabled(ifNumMethod) { param ->
            param.result = true
        }
        return true
    }
}