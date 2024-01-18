/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
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

import cc.ioctl.util.HookUtils
import cc.ioctl.util.MsgRecordUtil
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.loadClass
import com.github.kyuubiran.ezxhelper.utils.paramCount
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter.Locations.Simplify
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import me.ketal.util.hookMethod
import xyz.nextalone.base.MultiItemDelayableHook
import java.text.Collator
import java.util.Locale

@FunctionHookEntry
@UiItemAgentEntry
object AntiNickBlock : MultiItemDelayableHook("ll_anti_nickblock") {
    override var allItems = setOf<String>()
    override val defaultItems = setOf<String>()
    override var items: MutableList<String> = MsgRecordUtil.NICK_BLOCKS.keys.sortedWith(chineseSorter).toMutableList()
    override val preferenceTitle: String = "屏蔽群昵称图标"
    override val dialogDesc = "屏蔽群昵称图标"
    override val enableCustom = false
    override val uiItemLocation = Simplify.UI_CHAT_MSG


    override fun initOnce(): Boolean {
        val providerList = arrayOf(
            "com.tencent.mobileqq.vas.vipicon.g",
            "com.tencent.qqnt.aio.nick.f",
            "com.tencent.mobileqq.aio.msglist.holder.component.nick.block.f",
            "com.tencent.mobileqq.activity.qcircle.c",
        )
        val callBack = HookUtils.afterIfEnabled(this) { param ->
            if (param.result != null) {
                val modified = (param.result as List<*>).toMutableList()
                val sources = (param.result as List<*>).toMutableList()
                modified.clear()
                val items: List<String> = MsgRecordUtil.parseNickBlocks(activeItems)
                for (i in 0 until sources.size) {
                    if (!items.contains(sources[i]?.javaClass?.simpleName)) {
                        modified.add(sources[i])
                    }
                }
                param.result = modified.toList()
            }
        }

        providerList.forEach { provider ->
            loadClass(provider).findMethod {
                //protected List<AbsNickBlock> a(@NotNull Context context, @NotNull LinearLayout rootView)
                paramCount == 2
            }.hookMethod(callBack)
        }
        return true
    }

    override var isEnabled: Boolean
        get() = activeItems.isNotEmpty()
        set(value) {}

    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_80)

}

val chineseSorter = Comparator<String>(Collator.getInstance(Locale.CHINA)::compare)
