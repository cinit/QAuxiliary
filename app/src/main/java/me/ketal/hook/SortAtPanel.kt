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
package me.ketal.hook

import cc.ioctl.util.Reflex.getFirstByType
import com.github.kyuubiran.ezxhelper.utils.getObjectByTypeAs
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.tlb.ConfigTable
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.PlayQQVersion
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.TIMVersion
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.NAtPanel_refreshUI
import io.github.qauxv.util.dexkit.NAtPanel_showDialogAtView
import io.github.qauxv.util.requireMinVersion
import xyz.nextalone.data.TroopInfo
import xyz.nextalone.util.get
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.hookBefore
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object SortAtPanel : CommonSwitchFunctionHook(
    arrayOf(NAtPanel_refreshUI, NAtPanel_showDialogAtView)
) {

    override val name = "修改@界面排序"
    override val description = "排序由群主管理员至正常人员"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY
    override val extraSearchKeywords: Array<String> = arrayOf("艾特", "at")
    override val isAvailable: Boolean
        get() = requireMinVersion(QQVersion.QQ_8_1_3, TIMVersion.TIM_3_1_1, PlayQQVersion.PlayQQ_8_2_9)

    const val sessionInfoTroopUin = "SortAtPanel.sessionInfoTroopUin"
    private var isSort: Boolean? = null
    override fun initOnce() = throwOrTrue {
        val showDialogAtView = DexKit.doFindMethod(NAtPanel_showDialogAtView)
        showDialogAtView?.hookAfter(this) {
            isSort = (it.args[1] as String?)?.isNotEmpty()
        }
        val refreshUI = DexKit.doFindMethod(NAtPanel_refreshUI)
        refreshUI?.hookBefore(this) {
            if (isSort == true) return@hookBefore
            val sessionInfo = getFirstByType(it.thisObject, Initiator._SessionInfo())
            check(sessionInfo != null) { "sessionInfo is null" }
            val troopUin = getTroopUin(sessionInfo)
            check(troopUin != null) { "troopUin is null" }
            val troopInfo = TroopInfo(troopUin)
            val list = it.args[0].getObjectByTypeAs<MutableList<Any>>(MutableList::class.java)
            val isAdmin = "0" == getMemberUin(list[0])
            val admin = mutableListOf<Any>()
            list.forEach { member ->
                when (getMemberUin(member)) {
                    "0" -> return@forEach
                    troopInfo.troopOwnerUin -> admin.add(0, member)
                    in troopInfo.troopAdmin!! -> admin.add(member)
                }
            }
            list.removeAll(admin.toSet())
            list.addAll(if (isAdmin) 1 else 0, admin)
        }
    }

    private fun getTroopUin(sessionInfo: Any?): String? =
        sessionInfo.get("troopUin", String::class.java)
            ?: sessionInfo.get(ConfigTable.getConfig(sessionInfoTroopUin), String::class.java)

    private fun getMemberUin(member: Any?): String? =
        member.get("uin", String::class.java)
            ?: member.get("a", String::class.java)
}
