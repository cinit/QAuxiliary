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
package io.github.qauxv.router.dispacher

import cc.hicore.QApp.QAppUtils
import io.github.qauxv.base.annotation.EntityAgentEntry
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedBridge
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.hook.BaseHookDispatcher
import io.github.qauxv.router.decorator.IItemBuilderFactoryHookDecorator
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.dexkit.CItemBuilderFactory
import io.github.qauxv.util.dexkit.DexKit
import me.singleneuron.hook.decorator.CardMsgToText
import me.singleneuron.hook.decorator.MiniAppToStruckMsg
import me.singleneuron.hook.decorator.SimpleCheckIn
import me.singleneuron.hook.decorator.SimpleReceiptMessage
import java.lang.reflect.Method

@FunctionHookEntry
@EntityAgentEntry
object ItemBuilderFactoryHook : BaseHookDispatcher<IItemBuilderFactoryHookDecorator>(
        arrayOf(CItemBuilderFactory)
) {

    // register your decorator here
    // THEY ARE INVOKED IN UI THREAD WITH A VERY HIGH FREQUENCY,
    // OPTIMIZE YOUR CODE, CACHE YOUR REFLECTION FIELDS AND METHODS FOR BETTER PERFORMANCE
    // *** Peak frequency: ~412 invocations per second
    override val decorators: Array<IItemBuilderFactoryHookDecorator> = arrayOf(
            CardMsgToText,
            MiniAppToStruckMsg,
            SimpleCheckIn,
            SimpleReceiptMessage,
    )

    @Throws(Exception::class)
    override fun initOnce(): Boolean {
        if (QAppUtils.isQQnt()) {
            // QQ NT does not use ItemBuilderFactory
            // TODO: 2024-07-19 implement an alternative for QQ NT
            return true
        }
        var getMsgType: Method? = null
        for (m in DexKit.requireClassFromCache(CItemBuilderFactory).methods) {
            if (m.returnType == Int::class.javaPrimitiveType) {
                val argt = m.parameterTypes
                if (argt.isNotEmpty() && argt[argt.size - 1] == Initiator._ChatMessage()) {
                    getMsgType = m
                    break
                }
            }
        }
        XposedBridge.hookMethod(getMsgType, object : XC_MethodHook(39) {
            @Throws(Throwable::class)
            override fun afterHookedMethod(param: MethodHookParam) {
                val result = param.result as Int
                val chatMessage = param.args[param.args.size - 1]
                for (decorator in decorators) {
                    try {
                        if (decorator.isEnabled && decorator.onGetMsgTypeHook(result, chatMessage, param)) {
                            return
                        }
                    } catch (e: Throwable) {
                        decorator.traceError(e)
                    }
                }
            }
        })
        return true
    }
}
