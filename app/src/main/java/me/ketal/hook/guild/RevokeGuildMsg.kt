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

package me.ketal.hook.guild

import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.tencent.mobileqq.data.MessageRecord
import com.tencent.mobileqq.guild.message.api.IGuildNicknameApi
import com.tencent.mobileqq.qroute.QRoute
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator._MessageRecord
import io.github.qauxv.util.SyncUtils
import me.singleneuron.data.MsgRecordData
import mqq.app.AppRuntime
import tencent.im.group_pro_proto.common.common
import xyz.nextalone.util.clazz
import xyz.nextalone.util.get
import xyz.nextalone.util.hookBefore
import xyz.nextalone.util.invoke
import xyz.nextalone.util.method
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object RevokeGuildMsg : CommonSwitchFunctionHook(SyncUtils.PROC_MAIN or SyncUtils.PROC_MSF) {

    override val name = "频道防撤回消息"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.GUILD_CATEGORY

    override fun initOnce() = throwOrTrue {
        "com/tencent/mobileqq/guild/message/eventflow/api/impl/GuildEventFlowServiceImpl"
            .clazz?.method("handleDeleteEvent")
            ?.hookBefore(this) { param ->
                val thisObject = param.thisObject
                val appRuntime = thisObject.get("appRuntime")!!
                val msgRecord = param.args[0] ?: return@hookBefore
                val event = param.args[1] as common.Event
                val senderUin = MsgRecordData(msgRecord).senderUin!!

                val opInfo = event.op_info
                if (!opInfo.has()) return@hookBefore
                val operatorTinyid = opInfo.operator_tinyid
                if (!operatorTinyid.has()) return@hookBefore
                val operatorId = operatorTinyid.get().toString()
                val selfTinyId = appRuntime.invoke(
                    "getRuntimeService",
                    "com.tencent.mobileqq.qqguildsdk.api.IGPSService".clazz!!,
                    Class::class.java
                )?.invoke("getSelfTinyId")

                val revokerNick = getNickName(appRuntime, operatorId, msgRecord)
                val authorNick = getNickName(appRuntime, senderUin, msgRecord)
                val msg = MsgRecordData(msgRecord).msg
                val greyMsg = if (senderUin == operatorId) {
                    "\"$revokerNick\u202d\" 尝试撤回一条消息: $msg "
                } else {
                    "\"$revokerNick\u202d\"撤回了\"$authorNick\u202d\"的消息: $msg"
                }
                msgRecord.invoke(
                    "saveExtInfoToExtStr",
                    "wording", greyMsg,
                    String::class.java, String::class.java
                )
                thisObject.invoke(
                    "addGreyTipsForDeletedMsg",
                    msgRecord, event,
                    _MessageRecord(), event.javaClass
                )

                if (selfTinyId != operatorId) {
                    param.result = null
                }
            }

        "Lcom/tencent/mobileqq/guild/message/msgtype/MessageForGuildRevokeGrayTip;->init(Lcom/tencent/mobileqq/data/MessageRecord;Ljava/lang/String;ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)V"
            .method.hookBefore(this) {
                val msg = it.args[0]
                val wording = msg.invoke(
                    "getExtInfoFromExtStr",
                    "wording", String::class.java
                )
                if (wording == null || "" == wording) return@hookBefore
                it.args[1] = wording
            }

        "com/tencent/mobileqq/guild/message/eventflow/api/impl/GuildEventFlowServiceImpl".clazz!!.findMethod {
            name == "processUpdateEvent"
        }.hookBefore(this) {
            if (it.args[1].get("msgtype") != it.args[2].get("msgtype")) {
                it.result = true
            }
        }
    }

    private fun getNickName(
        appRuntime: Any,
        uin: String,
        msg: Any
    ): String {
        val msgData = MsgRecordData(msg)
        return QRoute.api(IGuildNicknameApi::class.java).getDisplayName(
            appRuntime as AppRuntime, msgData.friendUin!!, uin, msg as MessageRecord
        )
    }
}
