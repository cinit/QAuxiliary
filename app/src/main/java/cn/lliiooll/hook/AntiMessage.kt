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

package cn.lliiooll.hook

import cn.lliiooll.msg.MessageReceiver
import cn.lliiooll.util.MsgRecordUtil
import de.robv.android.xposed.XposedHelpers
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import me.singleneuron.data.MsgRecordData
import xyz.nextalone.base.MultiItemDelayableHook
import java.text.Collator
import java.util.Locale

@UiItemAgentEntry
object AntiMessage : MultiItemDelayableHook("qn_anti_message_items"), MessageReceiver {
    override var allItems = setOf<String>()
    override val defaultItems = setOf<String>()
    override var items: MutableList<String> = MsgRecordUtil.MSG_WITH_DESC.keys.sortedWith(chineseSorter).toMutableList()
    override val preferenceTitle: String = "静默指定类型消息通知"
    override val enableCustom = false
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.NOTIFICATION_CATEGORY

    override fun onReceive(data: MsgRecordData?): Boolean {
        if (data?.selfUin.equals(data?.senderUin)) return false
        val items: List<Int> = MsgRecordUtil.parse(activeItems)
        if (items.contains(data?.msgType)) {
            XposedHelpers.setBooleanField(data?.msgRecord, "isread", true)
            return true
        } else if (items.contains(0) and (data?.msg?.contains("@全体成员") == true)) {
            XposedHelpers.setBooleanField(data?.msgRecord, "isread", true)
            return true
        }
        return false
    }

//    override fun listener(): View.OnClickListener {
//        items.forEachIndexed { i: Int, str: String ->
//            items[i] = MsgRecordUtil.getDesc(str)
//        }
//        items = items.sortedWith(SortChinese()).toTypedArray().toMutableList()
//        return super.listener()
//    }

//    override fun getBoolAry(): BooleanArray {
//        val ret = BooleanArray(items.size)
//        for ((i, item) in items.withIndex()) {
//            ret[i] = activeItems.contains(item) or activeItems.contains(MsgRecordUtil.getKey(item))
//        }
//        return ret
//    }

    override val isAvailable: Boolean get() = requireMinQQVersion(QQVersion.QQ_8_0_0)
}

val chineseSorter = Comparator<String>(Collator.getInstance(Locale.CHINA)::compare)
