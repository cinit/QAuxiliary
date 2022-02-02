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

package me.ketal.dispacher

import android.view.View
import android.view.ViewGroup
import de.robv.android.xposed.XC_MethodHook
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.hook.CommonDelayableHook
import me.ketal.hook.ChatItemShowQQUin
import me.ketal.hook.ShowMsgAt
import me.singleneuron.qn_kernel.data.MsgRecordData
import xyz.nextalone.hook.HideTroopLevel
import xyz.nextalone.util.clazz
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.isPublic
import xyz.nextalone.util.tryOrFalse

@FunctionHookEntry
object BaseBubbleBuilderHook : CommonDelayableHook("__NOT_USED__") {
    //Register your decorator here
    private val decorators = arrayOf<OnBubbleBuilder>(
        HideTroopLevel,
        ShowMsgAt,
        ChatItemShowQQUin,
    )

    override fun initOnce() = tryOrFalse {
        for (m in "com.tencent.mobileqq.activity.aio.BaseBubbleBuilder".clazz?.methods!!) {
            //
            if (m.name != "a") continue
            if (m.returnType != View::class.java) continue
            if (!m.isPublic) continue
            if (m.parameterTypes.size != 6) continue
            m.hookAfter(this) {
                if (it.result == null) return@hookAfter
                val rootView = it.result as ViewGroup
                val msg = MsgRecordData(it.args[2])
                for (decorator in decorators) {
                    try {
                        decorator.onGetView(rootView, msg, it)
                    } catch (e: Exception) {
                        Log.e(e)
                    }
                }
            }
        }
    }

    override fun isEnabled() = true

    override fun setEnabled(enabled: Boolean) = Unit
}

interface OnBubbleBuilder {
    fun onGetView(
        rootView: ViewGroup,
        chatMessage: MsgRecordData,
        param: XC_MethodHook.MethodHookParam
    )
}
