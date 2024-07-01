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
import io.github.qauxv.bridge.GreyTipBuilder
import io.github.qauxv.bridge.QQMessageFacade
import io.github.qauxv.bridge.kernelcompat.ContactCompat
import io.github.qauxv.bridge.ntapi.ChatTypeConstants
import io.github.qauxv.bridge.ntapi.NtGrayTipHelper
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Log
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.dexkit.CMessageRecordFactory
import io.github.qauxv.util.dexkit.NContactUtils_getBuddyName
import io.github.qauxv.util.dexkit.NContactUtils_getDiscussionMemberShowName
import io.github.qauxv.util.requireMinQQVersion

@FunctionHookEntry
@UiItemAgentEntry
object GagInfoDisclosure : CommonSwitchFunctionHook(
    // TODO: 2020/6/12 Figure out whether MSF is really needed
    targetProc = SyncUtils.PROC_MAIN or SyncUtils.PROC_MSF,
    targets = arrayOf(
        CMessageRecordFactory,
        NContactUtils_getDiscussionMemberShowName,
        NContactUtils_getBuddyName,
    )
) {

    override val name = "显示设置禁言的管理"
    override val description = "总是显示哪个管理员设置了禁言"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY

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

    private fun addTroopGrayTipMsg(troopUin: String, jsonStr: String) {
        NtGrayTipHelper.addLocalJsonGrayTipMsg(
            AppRuntimeHelper.getAppRuntime()!!,
            ContactCompat(ChatTypeConstants.GROUP, troopUin, ""),
            NtGrayTipHelper.createLocalJsonElement(NtGrayTipHelper.AIO_AV_GROUP_NOTICE.toLong(), jsonStr, ""),
            true,
            true
        ) { result, uin ->
            if (result != 0) {
                Log.e("GagInfoDisclosure error: addLocalJsonGrayTipMsg failed, result=$result, uin=$uin")
            }
        }
    }

    override fun initOnce(): Boolean {
        val clzGagMgr = Initiator._TroopGagMgr()
        if (requireMinQQVersion(QQVersion.QQ_9_0_25)) {
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
                val troopUin = param.args[1] as Long
                val opUin = param.args[2] as Long
                val opName = ContactUtils.getDiscussionMemberShowName(AppRuntimeHelper.getAppRuntime()!!, troopUin.toString(), opUin.toString())
                val builder = NtGrayTipHelper.NtGrayTipJsonBuilder()
                builder.appendText("操作者: ")
                if (opUin.toString() == selfUin) {
                    builder.appendText("你")
                } else {
                    builder.appendText("$opName[$opUin]")
                }
                addTroopGrayTipMsg(troopUin.toString(), builder.build().toString())
            }
        } else {
            val method1 = clzGagMgr.declaredMethods.single { method ->
                val params = method.parameterTypes; params.size == 7
                && params[0] == String::class.java
                && params[1] == Long::class.java
                && params[2] == Long::class.java
                && params[3] == Int::class.java
                && params[4] == String::class.java
                && params[4] == String::class.java
                && params[4] == Boolean::class.java
                && method.returnType == Void.TYPE
            }
            hookBeforeIfEnabled(method1) { param ->
                val selfUin = AppRuntimeHelper.getAccount()
                val troopUin = param.args[0] as String
                val time = param.args[1] as Long
                val interval = param.args[2] as Long
                val msgSeq = param.args[3] as Int
                val opUin = param.args[4] as String
                val victimUin = param.args[5] as String
                val victimName = ContactUtils.getTroopMemberNick(troopUin, victimUin)
                val opName = ContactUtils.getTroopMemberNick(troopUin, opUin)
                if (!QAppUtils.isQQnt()) {
                    val builder = GreyTipBuilder.create(GreyTipBuilder.MSG_TYPE_TROOP_GAP_GRAY_TIPS)
                    if (victimUin == selfUin) {
                        builder.append("你")
                    } else {
                        builder.appendTroopMember(victimUin, victimName)
                    }
                    builder.append(" 被 ")
                    if (opUin == selfUin) {
                        builder.append("你")
                    } else {
                        builder.appendTroopMember(opUin, opName)
                    }
                    if (interval == 0L) {
                        builder.append(" 解除禁言 ")
                    } else {
                        builder.append(" 禁言${getSecStr(interval)} ")
                    }
                    val msg = builder.build(troopUin, 1, opUin, time, msgSeq.toLong())
                    val list = ArrayList<Any>() + msg
                    QQMessageFacade.commitMessageRecordList(list)
                } else {
                    val builder = NtGrayTipHelper.NtGrayTipJsonBuilder()
                    if (victimUin == selfUin) {
                        builder.appendText("你")
                    } else {
                        builder.appendText("$victimName[$victimUin]")
                    }
                    builder.appendText(" 被 ")
                    if (opUin == selfUin) {
                        builder.appendText("你")
                    } else {
                        builder.appendText("$opName[$opUin]")
                    }
                    if (interval == 0L) {
                        builder.appendText(" 解除禁言 ")
                    } else {
                        builder.appendText(" 禁言${getSecStr(interval)} ")
                    }
                    addTroopGrayTipMsg(troopUin, builder.build().toString())
                }
                param.setResult(null)
            }
            val method2 = clzGagMgr.declaredMethods.single { method ->
                val params = method.parameterTypes; params.size == 7
                && params[0] == String::class.java
                && params[1] == String::class.java
                && params[2] == Long::class.java
                && params[3] == Long::class.java
                && params[4] == Int::class.java
                && params[4] == Boolean::class.java
                && params[4] == Boolean::class.java
                && method.returnType == Void.TYPE
            }
            hookBeforeIfEnabled(method2) { param ->
                val selfUin = AppRuntimeHelper.getAccount()
                val troopUin = param.args[0] as String
                val opUin = param.args[1] as String
                val time = param.args[2] as Long
                val interval = param.args[3] as Long
                val msgSeq = param.args[4] as Int
                val gagTroop = param.args[5] as Boolean
                val opName = ContactUtils.getTroopMemberNick(troopUin, opUin)
                if (!QAppUtils.isQQnt()) {
                    val builder = GreyTipBuilder.create(GreyTipBuilder.MSG_TYPE_TROOP_GAP_GRAY_TIPS)
                    if (gagTroop) {
                        if (opUin == selfUin) {
                            builder.append("你")
                        } else {
                            builder.appendTroopMember(opUin, opName)
                        }
                        if (interval == 0L) {
                            builder.append(" 关闭了全员禁言 ")
                        } else {
                            builder.append(" 开启了全员禁言 ")
                        }
                    } else {
                        builder.append("你")
                        builder.append(" 被 ")
                        builder.appendTroopMember(opUin, opName)
                        if (interval == 0L) {
                            builder.append(" 解除禁言 ")
                        } else {
                            builder.append(" 禁言${getSecStr(interval)} ")
                        }
                    }
                    val msg = builder.build(troopUin, 1, opUin, time, msgSeq.toLong())
                    val list = ArrayList<Any>() + msg
                    QQMessageFacade.commitMessageRecordList(list)
                } else {
                    val builder = NtGrayTipHelper.NtGrayTipJsonBuilder()
                    if (gagTroop) {
                        if (opUin == selfUin) {
                            builder.appendText("你")
                        } else {
                            builder.appendText("$opName[$opUin]")
                        }
                        if (interval == 0L) {
                            builder.appendText(" 关闭了全员禁言 ")
                        } else {
                            builder.appendText(" 开启了全员禁言 ")
                        }
                    } else {
                        builder.appendText("你")
                        builder.appendText(" 被 ")
                        builder.appendText("$opName[$opUin]")
                        if (interval == 0L) {
                            builder.appendText(" 解除禁言 ")
                        } else {
                            builder.appendText(" 禁言${getSecStr(interval)} ")
                        }
                    }
                    addTroopGrayTipMsg(troopUin, builder.build().toString())
                }
                param.setResult(null)
            }
        }
        return true
    }
}