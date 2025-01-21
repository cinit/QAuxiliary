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
import cc.ioctl.util.hookAfterIfEnabled
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
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.TIMVersion
import io.github.qauxv.util.dexkit.CMessageRecordFactory
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.Hd_GagInfoDisclosure_Method
import io.github.qauxv.util.dexkit.NContactUtils_getBuddyName
import io.github.qauxv.util.dexkit.NContactUtils_getDiscussionMemberShowName
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.requireMinTimVersion
import xyz.nextalone.util.get

@FunctionHookEntry
@UiItemAgentEntry
object GagInfoDisclosure : CommonSwitchFunctionHook(
    // TODO: 2020/6/12 Figure out whether MSF is really needed
    targetProc = SyncUtils.PROC_MAIN or SyncUtils.PROC_MSF,
    targets = arrayOf(
        CMessageRecordFactory,
        NContactUtils_getDiscussionMemberShowName,
        NContactUtils_getBuddyName,
        Hd_GagInfoDisclosure_Method,
    )
) {

    override val name = "显示设置禁言的管理"
    override val description = "总是显示哪个管理员设置了禁言"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY
    override val isAvailable = QAppUtils.isQQnt()

    private fun getLongData(bArr: ByteArray, i: Int): Long {
        return (((bArr[i].toInt() and 255) shl 24) + ((bArr[i + 1].toInt() and 255) shl 16) + ((bArr[i + 2].toInt() and 255) shl 8) + ((bArr[i + 3].toInt() and 255) shl 0)).toLong()
    }

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
        val uid = RelationNTUinAndUidApi.getUidFromUin(uin).takeIf { it.isNullOrEmpty().not() } ?: "u_0000000000000000000000"
        this.append(NtGrayTipHelper.NtGrayTipJsonBuilder.UserItem(uin, uid, name))
    }

    private fun addGagTipMsg(selfUin: String, troopUin: String, opUin: String, victimUin: String, victimTime: Long) {
        val opName = ContactUtils.getTroopMemberNick(troopUin, opUin)
        val victimName = ContactUtils.getTroopMemberNick(troopUin, victimUin)
        val builder = NtGrayTipHelper.NtGrayTipJsonBuilder()
        when (victimUin) {
            "0" -> {
                if (opUin == selfUin) builder.appendUserItem(selfUin, "你") else builder.appendUserItem(opUin, opName)
                builder.appendText(if (victimTime == 0L) "关闭了全员禁言" else "开启了全员禁言")
            }

            selfUin -> {
                builder.appendUserItem(selfUin, "你")
                builder.appendText("被")
                builder.appendUserItem(opUin, opName)
                builder.appendText(if (victimTime == 0L) "解除禁言" else "禁言${getSecStr(victimTime)}")
            }

            else -> {
                builder.appendUserItem(victimUin, victimName)
                builder.appendText("被")
                if (opUin == selfUin) builder.appendUserItem(selfUin, "你") else builder.appendUserItem(opUin, opName)
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

    override fun initOnce(): Boolean {
        if (requireMinQQVersion(QQVersion.QQ_9_0_73) || requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA)) {
            hookAfterIfEnabled(DexKit.requireMethodFromCache(Hd_GagInfoDisclosure_Method)) { param ->
                val msgInfo = param.args[1]
                val vMsg = msgInfo.get("vMsg") as ByteArray? ?: return@hookAfterIfEnabled
                if (vMsg[4].toInt() == 12) {
                    val selfUin = AppRuntimeHelper.getAccount()
                    val troopUin = getLongData(vMsg, 0).toString()
                    val opUinTmp = getLongData(vMsg, 6)
                    val opUin = (opUinTmp.takeIf { it > 0 } ?: (opUinTmp and 0xFFFFFFFFL)).toString()
                    val victimUinTmp = getLongData(vMsg, 16)
                    val victimUin = (victimUinTmp.takeIf { it > 0 } ?: (victimUinTmp and 0xFFFFFFFFL)).toString()
                    val victimTime = getLongData(vMsg, 20)
                    addGagTipMsg(selfUin, troopUin, opUin, victimUin, victimTime)
                }
            }
        } else {
            val troopGagClass = Initiator.loadClass("com.tencent.mobileqq.troop.utils.TroopGagMgr")
            val method = troopGagClass.declaredMethods.single { method ->
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
                val pushParams = param.args[4] as ArrayList<*>
                val victimUin = pushParams[0].get("uin") as String
                val victimTime = pushParams[0].get("gagLength") as Long
                addGagTipMsg(selfUin, troopUin, opUin, victimUin, victimTime)
            }
        }
        return true
    }
}