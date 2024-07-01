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

package cc.ioctl.hook.chat

import cc.hicore.QApp.QAppUtils
import cc.ioctl.util.hookBeforeIfEnabled
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.bridge.AppRuntimeHelper
import io.github.qauxv.bridge.ContactUtils
import io.github.qauxv.bridge.kernelcompat.ContactCompat
import io.github.qauxv.bridge.ntapi.ChatTypeConstants
import io.github.qauxv.bridge.ntapi.NtGrayTipHelper
import io.github.qauxv.bridge.ntapi.RelationNTUinAndUidApi
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Log
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.dexkit.CMessageRecordFactory
import io.github.qauxv.util.dexkit.NContactUtils_getDiscussionMemberShowName
import xyz.nextalone.util.get

@FunctionHookEntry
@UiItemAgentEntry
object GagInfoDisclosure : CommonSwitchFunctionHook(
    // TODO: 2020/6/12 Figure out whether MSF is really needed
    targetProc = SyncUtils.PROC_MAIN or SyncUtils.PROC_MSF,
    targets = arrayOf(
        CMessageRecordFactory,
        NContactUtils_getDiscussionMemberShowName,
    )
) {

    override val name = "显示设置禁言的管理"
    override val description = "总是显示哪个管理员设置了禁言"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY
    override val isAvailable = QAppUtils.isQQnt()

    private fun getSecStr(sec: Long): String {
        val (min, hour, day) = Triple("分", "时", "天")
        val d = sec / 86400
        val h = (sec % 86400) / 3600
        val m = ((sec % 86400) % 3600) / 60
        val ret = StringBuilder()
        if (d > 0) ret.append(d).append(day)
        if (h > 0) ret.append(h).append(hour)
        if (m > 0) ret.append(m).append(min)
        return ret.toString()
    }

    private fun NtGrayTipHelper.NtGrayTipJsonBuilder.appendUserItem(uin: String, name: String) {
        val uid = RelationNTUinAndUidApi.getUidFromUin(uin).takeIf { it.isNullOrEmpty() } ?: "u_0000000000000000000000"
        this.append(NtGrayTipHelper.NtGrayTipJsonBuilder.UserItem(uin, uid, name))
    }

    override fun initOnce(): Boolean {
        val clzGagMgr = Initiator._TroopGagMgr()
        val method = clzGagMgr.declaredMethods.single { method ->
            val params = method.parameterTypes; params.size == 5
            && params[0] == Int::class.java
            && params[1] == Long::class.java
            && params[2] == Long::class.java
            && params[3] == Long::class.java
            && params[4] == ArrayList::class.java
            && method.returnType == Void.TYPE
        }
        hookBeforeIfEnabled(method) { param ->
            val selfUin = AppRuntimeHelper.getAccount()
            val troopUin = param.args[1].toString()
            val opUin = param.args[2].toString()
            val opName = ContactUtils.getDiscussionMemberShowName(AppRuntimeHelper.getAppRuntime()!!, troopUin, opUin)
            val pushParams = param.args[4] as ArrayList<*>
            val victimUin = pushParams[0].get("uin") as String
            val victimName = ContactUtils.getDiscussionMemberShowName(AppRuntimeHelper.getAppRuntime()!!, troopUin, victimUin)
            val victimTime = pushParams[0].get("gagLength") as Long
            val builder = NtGrayTipHelper.NtGrayTipJsonBuilder()
            when (victimUin) {
                "0" -> {
                    if (opUin == selfUin) builder.appendUserItem(selfUin, "你") else builder.appendUserItem(opUin, "$opName")
                    builder.appendText(if (victimTime == 0L) " 关闭了全员禁言 " else " 开启了全员禁言 ")
                }

                selfUin -> {
                    builder.appendUserItem(selfUin, "你")
                    builder.appendText(" 被 ")
                    builder.appendUserItem(opUin, "$opName")
                    builder.appendText(if (victimTime == 0L) "解除禁言" else "禁言${getSecStr(victimTime)}")
                }

                else -> {
                    builder.appendUserItem(victimUin, "$victimName")
                    builder.appendText("被")
                    if (opUin == selfUin) builder.appendUserItem(selfUin, "你") else builder.appendUserItem(opUin, "$opName")
                    builder.appendText(if (victimTime == 0L) "解除禁言" else "禁言${getSecStr(victimTime)}")
                }
            }
            NtGrayTipHelper.addLocalJsonGrayTipMsg(
                AppRuntimeHelper.getAppRuntime()!!,
                ContactCompat(ChatTypeConstants.GROUP, troopUin, ""),
                NtGrayTipHelper.createLocalJsonElement(NtGrayTipHelper.AIO_AV_GROUP_NOTICE.toLong(), builder.build().toString(), ""),
                true,
                true
            ) { result, uin ->
                if (result != 0) {
                    Log.e("GagInfoDisclosure error: addLocalJsonGrayTipMsg failed, result=$result, uin=$uin")
                }
            }
        }
        return true
    }
}