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

package cc.ioctl.hook.notification

import cc.ioctl.util.Reflex
import cc.ioctl.util.msg.MessageManager
import de.robv.android.xposed.XC_MethodHook
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.hook.BasePersistBackgroundHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.hostInfo
import io.github.qauxv.util.requireMinQQVersion
import me.singleneuron.data.MsgRecordData
import xyz.nextalone.util.clazz
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.method
import xyz.nextalone.util.methodWithSuper
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
object MessageInterception : BasePersistBackgroundHook() {
    override fun initOnce() = throwOrTrue {
        val callback: (XC_MethodHook.MethodHookParam) -> Unit = { param ->
            val msgRecordData = MsgRecordData(param.args[0])
            MessageManager.call(msgRecordData)
        }
        if (hostInfo.versionCode >= QQVersion.QQ_8_8_80) {
            // I don't know why hook 3 methods, but it works
            // I don't know why they should be null-tolerant, but the previous version was
            Reflex.findSingleMethod(
                Initiator._C2CMessageManager(),
                Boolean::class.java,
                true,
                Initiator._MessageRecord(),
                Boolean::class.java,
                Boolean::class.java,
                "com.tencent.imcore.message.Message".clazz,
                Boolean::class.java
            ).hookAfter(this, callback)
            Reflex.findSingleMethod(
                Initiator._C2CMessageManager(),
                Boolean::class.java,
                true,
                Initiator._MessageRecord(),
                Boolean::class.java,
                Int::class.java
            ).hookAfter(this, callback)
            // I don't know what will the 3rd method do
            // updateMsgTab
            Initiator._C2CMessageManager().methodWithSuper(
                when {
                    requireMinQQVersion(QQVersion.QQ_9_0_56) -> "x0"
                    requireMinQQVersion(QQVersion.QQ_8_9_63) -> "y0"
                    requireMinQQVersion(QQVersion.QQ_8_9_3) -> "E0"
                    requireMinQQVersion(QQVersion.QQ_8_8_93) -> "A0"
                    else -> "d"
                },
                Boolean::class.java,
                Initiator._MessageRecord(),
            )?.hookAfter(this, callback)
        } else {
            Initiator._C2CMessageManager().method {
                it.parameterTypes.size == 2 && it.returnType == Boolean::class.java && it.parameterTypes[1] == Int::class.java
            }?.hookAfter(this, callback)
        }
    }
}
