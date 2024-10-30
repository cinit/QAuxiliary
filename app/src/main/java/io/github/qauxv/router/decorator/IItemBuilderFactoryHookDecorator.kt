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
package io.github.qauxv.router.decorator

import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.base.IDynamicHook
import io.github.qauxv.base.ITraceableDynamicHook
import io.github.qauxv.base.RuntimeErrorTracer
import io.github.qauxv.router.dispacher.ItemBuilderFactoryHook

interface IItemBuilderFactoryHookDecorator : ITraceableDynamicHook {

    /**
     * Called when the ItemBuilderFactory.getMsgType is called.
     * @param result the original result
     * @param chatMessage the chat message
     * @param param the [XC_MethodHook.MethodHookParam] parameter to allow you to modify the result
     * @return true to prevent the other hooks from being called
     */
    @Throws(Throwable::class)
    fun onGetMsgTypeHook(
        result: Int,
        chatMessage: Any,
        param: XC_MethodHook.MethodHookParam
    ): Boolean

    override val runtimeErrorDependentComponents: List<RuntimeErrorTracer>?
        get() = listOf(ItemBuilderFactoryHook)

}
