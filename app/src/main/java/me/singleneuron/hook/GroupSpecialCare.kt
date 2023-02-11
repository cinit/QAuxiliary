/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
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

package me.singleneuron.hook

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.SyncUtils
import xyz.nextalone.util.clazz
import xyz.nextalone.util.throwOrTrue
import java.util.concurrent.ConcurrentHashMap

@UiItemAgentEntry
object GroupSpecialCare : CommonSwitchFunctionHook(SyncUtils.PROC_MAIN or SyncUtils.PROC_MSF) {

    override val name = "关闭群普通消息特别关心提示"
    override val description: String = "仅在特别关心发送群消息时提示，阻止群内存在特别关心消息时其他成员普通消息使用特别关心提示"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.NOTIFICATION_CATEGORY

    override fun initOnce() = throwOrTrue {

        val notificationIdManager = "com.tencent.util.notification.NotifyIdManager".clazz
        val message = "com.tencent.imcore.message.Message".clazz

        val hook = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val map: ConcurrentHashMap<String, Boolean> = XposedHelpers.getObjectField(param.thisObject, "h") as ConcurrentHashMap<String, Boolean>
                val frienduin: String = XposedHelpers.getObjectField(param.args[1], "frienduin") as String
                map.remove(frienduin)
            }
        }

        XposedHelpers.findAndHookMethod(notificationIdManager, "m", String::class.java, message, hook)

    }
}
