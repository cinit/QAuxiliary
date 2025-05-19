/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2025 QAuxiliary developers
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

package io.github.nakixii.hook

import cc.ioctl.util.hookBeforeIfEnabled
import com.github.kyuubiran.ezxhelper.utils.findField
import com.github.kyuubiran.ezxhelper.utils.hookAllConstructorAfter
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinVersion
import xyz.nextalone.util.isPrivate

@FunctionHookEntry
@UiItemAgentEntry
object ForceUnfoldMessages : CommonSwitchFunctionHook() {
    override val name = "显示被折叠的消息"
    override val description = "使得那些被隐藏的色图可见"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY

    override fun initOnce(): Boolean {
        Initiator.loadClass("com.tencent.qqnt.kernel.nativeinterface.FoldingInfo").hookAllConstructorAfter {
            it.thisObject.javaClass.declaredFields.first {
                it.name == "beatType"
            }.apply { isAccessible = true }.set(it.thisObject, null)
        }
        return true
    }
}
