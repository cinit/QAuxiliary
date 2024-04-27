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

import android.os.Bundle
import cc.ioctl.util.hookAfterIfEnabled
import cc.ioctl.util.hookBeforeIfEnabled
import de.robv.android.xposed.XposedBridge
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.HdMethodFakePhone
import io.github.qauxv.util.requireMinQQVersion
import java.util.Objects

@FunctionHookEntry
@UiItemAgentEntry
object FakePhone : CommonSwitchFunctionHook(
    arrayOf(HdMethodFakePhone)
) {

    override val name = "自定义手机号码"
    override val description = "自定义设置页手机号码显示内容(待完善)"
    override val uiItemLocation = FunctionEntryRouter.Locations.Entertainment.ENTERTAIN_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override fun initOnce(): Boolean {
        val onUpdateClass = Initiator.loadClass("com.tencent.mobileqq.app.cd")
        val onUpdateMethod = onUpdateClass.getDeclaredMethod("onUpdate", Int::class.java, Boolean::class.java, Object::class.java)
        //val onUpdateMethodCache = DexKit.requireMethodFromCache(HdMethodFakePhone)
        hookBeforeIfEnabled(onUpdateMethod) { param ->
            if (param.args[0] == 5) {
                val bundle = param.args[2] as Bundle
                bundle.putString("phone", "100******00")
                param.args[2] = bundle
            }
        }
        return true
    }
}