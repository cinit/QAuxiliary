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

package cc.ioctl.hook.notification

import cc.ioctl.util.msg.MessageReceiver
import cc.ioctl.util.MsgRecordUtil
import io.github.qauxv.base.RuntimeErrorTracer
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import me.singleneuron.data.MsgRecordData
import xyz.nextalone.base.MultiItemDelayableHook
import xyz.nextalone.util.set
import java.text.Collator
import java.util.Locale

@UiItemAgentEntry
object AntiMessage : MultiItemDelayableHook("qn_anti_message_items"), MessageReceiver {
    override var allItems = setOf<String>()
    override val defaultItems = setOf<String>()
    override var items: MutableList<String> = MsgRecordUtil.MSG_WITH_DESC.keys.sortedWith(chineseSorter).toMutableList()
    override val preferenceTitle: String = "静默指定类型消息通知"
    override val dialogDesc = "静默消息"
    override val enableCustom = false
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.NOTIFICATION_CATEGORY

    override fun onReceive(data: MsgRecordData?): Boolean {
        if (data?.selfUin.equals(data?.senderUin)) return false
        val items: List<Int> = MsgRecordUtil.parse(activeItems)
        if (items.contains(data?.msgType)) {
            data?.msgRecord?.set("isread", true)
            return true
        } else if (data?.msg?.contains("@全体成员") == true && items.contains(0)) {
            data.msgRecord.set("isread", true)
            return true
        }
        return false
    }

    override fun initOnce(): Boolean {
        return MessageInterception.initialize()
    }

    override val runtimeErrorDependentComponents: List<RuntimeErrorTracer> = listOf(MessageInterception)

    override val isAvailable: Boolean get() = requireMinQQVersion(QQVersion.QQ_8_2_0)
}

val chineseSorter = Comparator<String>(Collator.getInstance(Locale.CHINA)::compare)
