/*
 * QNotified - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 dmca@ioctl.cc
 * https://github.com/ferredoxin/QNotified
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/ferredoxin/QNotified/blob/master/LICENSE.md>.
 */

package me.singleneuron.hook.decorator

import cc.ioctl.util.Reflex
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedHelpers
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.router.decorator.BaseSwitchFunctionDecorator
import io.github.qauxv.router.decorator.IItemBuilderFactoryHookDecorator
import io.github.qauxv.router.dispacher.ItemBuilderFactoryHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.decodeToDataClass
import io.github.qauxv.util.hostInfo
import me.singleneuron.data.MiniAppArkData
import me.singleneuron.data.StructMsgData
import org.json.JSONObject

@UiItemAgentEntry
@FunctionHookEntry
object MiniAppToStruckMsg : BaseSwitchFunctionDecorator(), IItemBuilderFactoryHookDecorator {

    override val name = "小程序分享转链接（接收）"
    override val description = "可能导致聊天界面滑动掉帧"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY
    override val dispatcher = ItemBuilderFactoryHook

    override fun onGetMsgTypeHook(
            result: Int,
            chatMessage: Any,
            param: XC_MethodHook.MethodHookParam
    ): Boolean {
        if (hostInfo.versionCode < QQVersion.QQ_8_2_0) return false
        return if (Initiator.loadClass("com.tencent.mobileqq.data.MessageForArkApp")
                        .isAssignableFrom(chatMessage.javaClass)
        ) {
            val arkAppMsg = Reflex.getInstanceObjectOrNull(chatMessage, "ark_app_message")
            val json = Reflex.invokeVirtual(arkAppMsg, "toAppXml", *arrayOfNulls(0)) as String
            val jsonObject = JSONObject(json)
            if (jsonObject.optString("app").contains("com.tencent.miniapp", true)) {
                val miniAppArkData = json.decodeToDataClass<MiniAppArkData>()
                val structMsgJson = StructMsgData.fromMiniApp(miniAppArkData).toString()
                //Log.d(structMsgJson)
                XposedHelpers.callMethod(arkAppMsg, "fromAppXml", structMsgJson)
                true
            } else false
        } else false
    }
}
