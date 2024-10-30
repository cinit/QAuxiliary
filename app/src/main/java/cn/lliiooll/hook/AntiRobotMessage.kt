/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the qwq233 Universal License
 * as published on https://github.com/qwq233/license; either
 * version 2 of the License, or any later version and our EULA as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the qwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package cn.lliiooll.hook

import cc.ioctl.hook.notification.MessageInterception
import cc.ioctl.util.msg.MessageReceiver
import io.github.qauxv.base.RuntimeErrorTracer
import io.github.qauxv.util.xpcompat.XposedHelpers
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.bridge.AppRuntimeHelper
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import me.singleneuron.data.MsgRecordData
import xyz.nextalone.util.clazz
import xyz.nextalone.util.invoke

// 自动已读群机器人消息
@FunctionHookEntry
@UiItemAgentEntry
object AntiRobotMessage : CommonSwitchFunctionHook("ll_anti_robot_message"), MessageReceiver {
    override val name: String
        get() = "自动已读群机器人消息"

    override fun initOnce(): Boolean {
        return MessageInterception.initialize()
    }

    override val uiItemLocation: Array<String>
        get() = FunctionEntryRouter.Locations.Auxiliary.NOTIFICATION_CATEGORY

    override fun onReceive(data: MsgRecordData?): Boolean {
        if (data != null && isEnabled) {
            val runtime = AppRuntimeHelper.getAppRuntime()
            val service = runtime?.invoke("getRuntimeService", "com.tencent.mobileqq.troop.robot.api.ITroopRobotService".clazz!!, Class::class.java)
            val isRobot = service?.invoke("isRobotUin", data.senderUin!!, String::class.java) as Boolean
            if (isRobot) {
                XposedHelpers.setBooleanField(data.msgRecord, "isread", true)
            }
        }
        return false
    }

    override val runtimeErrorDependentComponents: List<RuntimeErrorTracer> = listOf(MessageInterception)
}
