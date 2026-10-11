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

import cc.hicore.message.common.MsgBuilder
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
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
import io.github.qauxv.util.xpcompat.XC_MethodHook
import me.singleneuron.data.MiniAppArkData
import me.singleneuron.data.StructMsgData
import org.json.JSONObject
import xyz.nextalone.util.get
import xyz.nextalone.util.invoke
import xyz.nextalone.util.set

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
        return if (Initiator.loadClass("com.tencent.mobileqq.data.MessageForArkApp").isAssignableFrom(chatMessage.javaClass)) {
            val arkAppMsg = chatMessage.get("ark_app_message") ?: return false
            val json = arkAppMsg.invoke("toAppXml") as String
            val jsonObj = JSONObject(json)
            if (jsonObj.optString("app").contains("com.tencent.miniapp", true)) {
                val miniAppArkData = json.decodeToDataClass<MiniAppArkData>()
                val structMsgJson = StructMsgData.fromMiniApp(miniAppArkData).toString()
                arkAppMsg.invoke("fromAppXml", structMsgJson)
                true
            } else false
        } else false
    }

    override fun onNtCreateItemHook(
        msgRecord: MsgRecord,
        param: XC_MethodHook.MethodHookParam
    ): Boolean {
        val arkText = msgRecord.elements.firstOrNull { it.arkElement != null }?.arkElement?.bytesData ?: return false
        val jsonObj = JSONObject(arkText)
        return if (jsonObj.optString("app").contains("com.tencent.miniapp", true)) {
            val metaObj = jsonObj.optJSONObject("meta") ?: return false
            val prompt = jsonObj.optString("prompt") ?: "未知小程序"
            val detailObj = metaObj.optJSONObject("detail_1") // [QQ小程序]
            val qqUrl = detailObj?.optString("qqdocurl") ?: detailObj?.optString("url")
            val miniAppObj = metaObj.optJSONObject("miniapp") // [微信小程序]
            val wxUrl = miniAppObj?.optString("jumpUrl")
            val url = qqUrl ?: wxUrl ?: "解析链接异常"
            if (url.startsWith("mqqapi://")) return false // 内部链接
            msgRecord.apply {
                elements.apply {
                    clear()
                    add(MsgBuilder.nt_build_text("${prompt}\n${url}"))
                }
                set("msgType", 2)
                set("subMsgType", 0)
            }
            true
        } else false
    }
}
