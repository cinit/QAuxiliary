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
object AntiNickBlock : MultiItemDelayableHook(
    keyName = "ll_anti_nickblock"
) {
    override var allItems = setOf<String>()
    override val defaultItems = setOf<String>()
    override var items: MutableList<String> = MsgRecordUtil.NICK_BLOCKS.keys.sortedWith(chineseSorter).toMutableList()
    override val preferenceTitle: String = "屏蔽群昵称图标"
    override val dialogDesc = "屏蔽群昵称图标"
    override val enableCustom = false
    override val uiItemLocation = Simplify.UI_CHAT_MSG


    override fun initOnce(): Boolean {
        // com.tencent.mobileqq.aio.msglist.holder.component.nick.block.NickBlockInject
        //
        //     f149116b = arrayList;
        //     arrayList.add(NickBlockProvider.class);
        //     arrayList.add(ExtNickBlockProvider.class);
        //     arrayList.add(VasNickBlockProvider.class);
        //     arrayList.add(com.tencent.mobileqq.activity.qcircle.c.class);
        //
        // com.tencent.mobileqq.aio.msglist.holder.component.nick.block.AbsNickBlockProvider:
        // 9.0.20~9.0.56 -> .super Lcom/tencent/mobileqq/aio/msglist/holder/component/nick/block/d;
        // 9.0.60~9.1.5 -> .super Lcom/tencent/mobileqq/aio/msglist/holder/component/nick/block/b;
        //
        //public class ? extends com.tencent.mobileqq.aio.msglist.holder.component.nick.block.AbsNickBlockProvider
        val providerList = arrayOf(
            when {
                requireMinQQVersion(QQVersion.QQ_9_0_95) -> "com.tencent.mobileqq.activity.qcircle.i"
                requireMinQQVersion(QQVersion.QQ_9_0_65) -> "com.tencent.mobileqq.activity.qcircle.g"
                requireMinQQVersion(QQVersion.QQ_9_0_60) -> "com.tencent.mobileqq.activity.qcircle.h"
                requireMinQQVersion(QQVersion.QQ_9_0_35) -> "com.tencent.mobileqq.activity.qcircle.e"
                requireMinQQVersion(QQVersion.QQ_9_0_30) -> "com.tencent.mobileqq.activity.qcircle.f"
                else -> "com.tencent.mobileqq.activity.qcircle.c"
            },
            when {// com.tencent.mobileqq.aio.msglist.holder.component.nick.block.NickBlockProvider
                requireMinQQVersion(QQVersion.QQ_9_0_60) -> "com.tencent.mobileqq.aio.msglist.holder.component.nick.block.c"
                requireMinQQVersion(QQVersion.QQ_9_0_30) -> "com.tencent.mobileqq.aio.msglist.holder.component.nick.block.e"
                else -> "com.tencent.mobileqq.aio.msglist.holder.component.nick.block.f"
            },
            when {// com.tencent.mobileqq.vas.vipicon.VasNickBlockProvider
                requireMinQQVersion(QQVersion.QQ_9_0_60) -> "com.tencent.mobileqq.vas.vipicon.b"
                else -> "com.tencent.mobileqq.vas.vipicon.g"
            },
            when {// com.tencent.qqnt.aio.nick.ExtNickBlockProvider
                requireMinQQVersion(QQVersion.QQ_9_0_70) -> "com.tencent.qqnt.aio.nick.d"
                requireMinQQVersion(QQVersion.QQ_9_0_60) -> "com.tencent.qqnt.aio.nick.e"
                requireMinQQVersion(QQVersion.QQ_9_0_20) -> "com.tencent.qqnt.aio.nick.g"
                else -> "com.tencent.qqnt.aio.nick.f"
            },
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
                // protected List<AbsNickBlock> a(@NotNull Context context, @NotNull LinearLayout linearLayout) (9.0.20+)
                // protected List<AbsNickBlock> a(@NotNull Context context, @NotNull LinearLayout rootView)
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
