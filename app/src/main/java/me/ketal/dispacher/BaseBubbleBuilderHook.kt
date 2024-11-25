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

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import cc.hicore.QApp.QAppUtils
import cc.ioctl.hook.msg.MultiForwardAvatarHook
import cc.ioctl.util.HookUtils
import com.github.kyuubiran.ezxhelper.utils.findMethodOrNull
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.hook.BasePersistBackgroundHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.xpcompat.XC_MethodHook
import me.ketal.hook.ChatItemShowQQUin
import me.ketal.hook.ShowMsgAt
import me.singleneuron.data.MsgRecordData
import xyz.nextalone.hook.HideTroopLevel
import xyz.nextalone.util.hookAfter
import java.lang.reflect.Method
import java.lang.reflect.Modifier

@FunctionHookEntry
object BaseBubbleBuilderHook : BasePersistBackgroundHook() {
    // Register your decorator here
    // THESE HOOKS ARE CALLED IN UI THREAD WITH A VERY HIGH FREQUENCY
    // CACHE REFLECTION METHODS AND FIELDS FOR BETTER PERFORMANCE
    // Peak frequency: ~68 invocations per second
    private val decorators = arrayOf<OnBubbleBuilder>(
        HideTroopLevel,
        ShowMsgAt,
        ChatItemShowQQUin,
        MultiForwardAvatarHook
    )

    @Throws(Exception::class)
    override fun initOnce(): Boolean {
        if (QAppUtils.isQQnt()) {
            val kAIOBubbleMsgItemVB = Initiator.loadClass("com.tencent.mobileqq.aio.msglist.holder.AIOBubbleMsgItemVB")
            val getHostView = kAIOBubbleMsgItemVB.findMethodOrNull(true) {
                modifiers == Modifier.PUBLIC && returnType == View::class.java && parameterTypes.isEmpty()
            } ?: kAIOBubbleMsgItemVB.getMethod("getHostView")
            val kAIOMsgItem = Initiator.loadClass("com.tencent.mobileqq.aio.msg.AIOMsgItem")
            val bindMethod = kAIOBubbleMsgItemVB.declaredMethods.single {
                val argt = it.parameterTypes
                it.returnType == Void.TYPE && argt.size == 4 &&
                    argt[0] == Integer.TYPE &&
                    argt[1] == kAIOMsgItem.superclass &&
                    argt[2] == List::class.java &&
                    argt[3] == Bundle::class.java
            }
            val getMsgRecord = kAIOMsgItem.getMethod("getMsgRecord")
            HookUtils.hookAfterAlways(this, bindMethod, 50) {
                val msg = getMsgRecord.invoke(it.args[1]) as MsgRecord
                val rootView = getHostView.invoke(it.thisObject) as ViewGroup
                for (decorator in decorators) {
                    try {
                        decorator.onGetViewNt(rootView, msg, it)
                    } catch (e: Exception) {
                        traceError(e)
                    }
                }
            }
            return true
        }
        val kBaseBubbleBuilder = Initiator.loadClass("com.tencent.mobileqq.activity.aio.BaseBubbleBuilder")
        val getView: Method? = kBaseBubbleBuilder.declaredMethods.singleOrNull { m ->
            if (m.parameterTypes.size == 6 && m.returnType == View::class.java && m.modifiers == Modifier.PUBLIC) {
                val argt = m.parameterTypes
                argt[0] == Int::class.javaPrimitiveType && argt[1] == Int::class.javaPrimitiveType
            } else false
        }
        check(getView != null) { "Cannot find BaseBubbleBuilder.getView" }
        getView.hookAfter(this) {
            if (it.result == null) return@hookAfter
            val rootView = it.result as ViewGroup
            val msg = MsgRecordData(it.args[2])
            for (decorator in decorators) {
                try {
                    decorator.onGetView(rootView, msg, it)
                } catch (e: Exception) {
                    traceError(e)
                }
            }
        }
        return true
    }
}

interface OnBubbleBuilder {
    @Throws(Exception::class)
    fun onGetView(
        rootView: ViewGroup,
        chatMessage: MsgRecordData,
        param: XC_MethodHook.MethodHookParam
    )

    @Throws(Exception::class)
    fun onGetViewNt(
        rootView: ViewGroup,
        chatMessage: MsgRecord,
        param: XC_MethodHook.MethodHookParam
    )
}
