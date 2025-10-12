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

import cc.hicore.Utils.XLog
import cc.ioctl.util.Reflex.getFirstByType
import com.github.kyuubiran.ezxhelper.utils.getObjectByTypeAs
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.paramCount
import io.github.qauxv.util.xpcompat.XposedHelpers
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
import io.github.qauxv.util.requireMinTimVersion
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
        get() = requireMinVersion(QQVersion.QQ_8_2_0, TIMVersion.TIM_3_1_1, PlayQQVersion.PlayQQ_8_2_9)

    const val sessionInfoTroopUin = "SortAtPanel.sessionInfoTroopUin"
    private var isSort: Boolean? = null
    override fun initOnce() = throwOrTrue {

        val showDialogAtView = DexKit.loadMethodFromCache(NAtPanel_showDialogAtView)
        showDialogAtView?.hookAfter(this) {
            isSort = (it.args[1] as String?)?.isNotEmpty()
        }
        val refreshUI = DexKit.loadMethodFromCache(NAtPanel_refreshUI)
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

        // NT QQ clazz
        val clazz = Initiator.load("com.tencent.mobileqq.aio.input.at.business.AIOAtSelectMemberUseCase") ?: return@throwOrTrue

        // for TIM_NT
        if (requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA)) {
            for (method in clazz.declaredMethods) {
                if (method.parameterTypes.size == 1 &&
                    method.returnType == Map::class.java &&
                    method.parameterTypes[0] == List::class.java
                ) {

                    method.hookAfter {
                        val resultMap = it.result as? Map<String, List<Any>> ?: return@hookAfter
                        val mutableMap = resultMap.toMutableMap()

                        // 清空原数据
                        mutableMap.clear()

                        // 处理每个分组
                        resultMap.forEach { (key, value) ->
                            val normalMembers = mutableListOf<Any>()
                            val owners = mutableListOf<Any>()
                            val admins = mutableListOf<Any>()

                            // 分类成员
                            value.forEach { member ->
                                var isOwner = false
                                var isAdmin = false

                                // 查找包含 MemberInfo 的字段
                                for (field in member.javaClass.declaredFields) {
                                    if (field.type.name.contains("MemberInfo")) {
                                        field.isAccessible = true
                                        val memberInfo = field.get(member) ?: continue

                                        // 获取角色信息
                                        try {
                                            val roleField = memberInfo.javaClass.getDeclaredField("role")
                                            roleField.isAccessible = true
                                            val role = roleField.get(memberInfo)?.toString() ?: continue

                                            if (role.contains("OWNER")) {
                                                isOwner = true
                                                break
                                            } else if (role.contains("ADMIN")) {
                                                isAdmin = true
                                                break
                                            }
                                        } catch (e: Exception) {
                                            XLog.e("SortAtPanel", "Exception", e)
                                        }
                                    }
                                }

                                // 根据角色分类
                                when {
                                    isOwner -> owners.add(member)
                                    isAdmin -> admins.add(member)
                                    else -> normalMembers.add(member)
                                }
                            }

                            // 重新组织数据
                            mutableMap[key] = normalMembers

                            // 将群主和管理员添加到特殊分组
                            val specialKey = "★"
                            val specialMembers = (mutableMap.getOrDefault(specialKey, emptyList()).toMutableList()).apply {
                                addAll(owners)
                                addAll(admins)
                            }
                            mutableMap[specialKey] = specialMembers
                        }

                        it.result = mutableMap.toMap()
                    }
                    break
                }
            }
        }
        // for NT QQ 8.9.68.11450
        else {
            for (m in clazz.declaredMethods) {
                if (m.paramCount == 1 && m.returnType == Map::class.java && m.parameterTypes[0] == List::class.java) {
                    m.hookAfter {
                        val backMap = it.result as Map<String, List<Any>>
                        val map = backMap.toMutableMap()
                        map.clear()
                        backMap.forEach { (k, l) ->
                            val list = l.toMutableList()
                            val ob = l.toMutableList()
                            val ab = l.toMutableList()
                            list.clear()
                            ob.clear()
                            ab.clear()
                            l.forEach { v ->
                                var added = false
                                for (vf in v.javaClass.declaredFields) {
                                    if (vf.type.name.contains("MemberInfo")) {
                                        val info = XposedHelpers.getObjectField(v, vf.name)
                                        val role = XposedHelpers.getObjectField(info, "role")
                                        if (role.toString().contains("OWNER")) {
                                            // 群主
                                            ob.add(v)
                                            added = true
                                            break
                                        } else if (role.toString().contains("ADMIN")) {
                                            // 管理
                                            ab.add(v)
                                            added = true
                                            break
                                        }
                                    }
                                }
                                if (!added) {
                                    list.add(v)
                                }

                            }

                            map[k] = list
                            val f = "★"
                            val c = map.getOrDefault(f, arrayListOf()).toMutableList()
                            c.addAll(ob)
                            c.addAll(ab)
                            map[f] = c.toList()
                        }
                        it.result = map.toMap()
                    }
                }

            }
        }
    }

    private fun getTroopUin(sessionInfo: Any?): String? =
        sessionInfo.get("troopUin", String::class.java)
            ?: sessionInfo.get(ConfigTable.getConfig(sessionInfoTroopUin), String::class.java)

    private fun getMemberUin(member: Any?): String? =
        member.get("uin", String::class.java)
            ?: member.get("a", String::class.java)
}
