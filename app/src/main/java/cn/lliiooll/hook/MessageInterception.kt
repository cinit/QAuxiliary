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

package cn.lliiooll.hook

import cn.lliiooll.msg.MessageManager
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.hook.BasePersistBackgroundHook
import io.github.qauxv.util.Initiator
import me.singleneuron.data.MsgRecordData

@FunctionHookEntry
object MessageInterception : BasePersistBackgroundHook() {

    @Throws(Exception::class)
    override fun initOnce(): Boolean {
        val clazz = Initiator._C2CMessageManager()
        for (m in clazz.declaredMethods) {
            if (m.parameterTypes.size == 2 && m.returnType == MessageManager.booleanType && m.parameterTypes[1] == MessageManager.intType) {
                XposedBridge.hookMethod(m, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val msgRecord = param.args[0]
                        val msgRecordData = MsgRecordData(msgRecord)
                        MessageManager.call(msgRecordData)
                    }
                })
            }
        }
        return true
    }
}
