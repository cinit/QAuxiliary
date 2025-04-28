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

package me.hd.hook

import com.github.kyuubiran.ezxhelper.utils.hookBefore
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.xpcompat.XposedHelpers
import xyz.nextalone.util.clazz
import xyz.nextalone.util.method

@FunctionHookEntry
@UiItemAgentEntry
object ChangeGroupStatus : CommonSwitchFunctionHook() {

    override val name = "更改群聊状态"
    override val description = """
        查看已退出、被停用或解散群聊的消息
        在群聊列表长按置顶, 即可显示群聊在消息列表
        可能导致 QQ 界面卡顿, 若非必要请保持关闭状态
    """.trimIndent()
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.GROUP_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override fun initOnce(): Boolean {
        if (requireMinQQVersion(QQVersion.QQ_9_1_50)) {
            "Lcom/tencent/mobileqq/aio/input/reply/i;->m()Z".method.hookBefore {
                it.result = false
            }
            "Lcom/tencent/mobileqq/aio/msglist/holder/component/b;->r()Z".method.hookBefore {
                it.result = false
            }
            "Lcom/tencent/mobileqq/data/troop/TroopInfo;->isUnreadableBlock()Z".method.hookBefore {
                it.result = false
            }
            "Lcom/tencent/mobileqq/troop/troopsetting/vm/TroopSettingViewModel;->Q2()V".method.hookBefore {
                it.result = null
            }
            "Lcom/tencent/qqnt/troop/TroopListRepo;->isExit(Ljava/lang/String;Ljava/lang/String;Z)Z".method.hookBefore {
                it.result = false
            }
        } else if (requireMinQQVersion(QQVersion.QQ_9_0_8)) {
            "Lcom/tencent/qqnt/aio/helper/TroopBlockHelper\$groupListener\$1;->a(Ljava/util/List;)V".method.hookBefore {
                val list = it.args[0] as List<*>
                list.forEach { troopInfo -> XposedHelpers.setBooleanField(troopInfo, "isTroopBlocked", false) }
            }
        } else {
            "Lcom/tencent/qqnt/aio/helper/TroopBlockHelper\$groupListener\$1;->onGroupListUpdate(Lcom/tencent/qqnt/kernel/nativeinterface/GroupListUpdateType;Ljava/util/ArrayList;)V".method.hookBefore {
                val arrayList = it.args[1] as ArrayList<*>
                val fieldEnable = XposedHelpers.getStaticObjectField("Lcom/tencent/qqnt/kernel/nativeinterface/GroupStatus;".clazz, "KENABLE")
                arrayList.forEach { groupSimpleInfo -> XposedHelpers.callMethod(groupSimpleInfo, "setGroupStatus", fieldEnable) }
            }
        }
        return true
    }
}
