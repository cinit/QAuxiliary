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

package cc.ioctl.fragment

import cc.ioctl.hook.RevokeMsgHook
import io.github.qauxv.base.ISwitchCellAgent
import io.github.qauxv.dsl.item.DslTMsgListItemInflatable
import io.github.qauxv.dsl.item.TextSwitchItem
import io.github.qauxv.fragment.BaseHierarchyFragment
import kotlin.reflect.KMutableProperty

class RevokeMsgConfigFragment : BaseHierarchyFragment() {

    override fun getTitle() = "防撤回设置"

    override val hierarchy: Array<DslTMsgListItemInflatable> by lazy {
        arrayOf(
            TextSwitchItem(
                "总开关",
                summary = "关闭后将不会拦截撤回消息",
                switchAgent = forBoolean(RevokeMsgHook.INSTANCE::isEnabled)
            ),
            TextSwitchItem(
                "显示消息 shmsgseq",
                summary = "在撤回提示灰字中显示被撤回消息的 shmsgseq",
                switchAgent = forBoolean(RevokeMsgHook.INSTANCE, RevokeMsgHook::isShowShmsgseqEnabled, RevokeMsgHook::setShowShmsgseqEnabled)
            ),
            TextSwitchItem(
                "保留自己撤回的消息",
                summary = null,
                switchAgent = forBoolean(RevokeMsgHook.INSTANCE, RevokeMsgHook::isKeepSelfMsgEnabled, RevokeMsgHook::setKeepSelfMsgEnabled)
            )
        )
    }

    private inline fun <reified T> forBoolean(
        this0: T,
        crossinline getter: (T) -> Boolean,
        crossinline setter: (T, Boolean) -> Unit
    ): ISwitchCellAgent {
        return object : ISwitchCellAgent {
            override val isCheckable: Boolean = true
            override var isChecked: Boolean
                get() = getter.invoke(this0)
                set(value) {
                    setter.invoke(this0, value)
                }
        }
    }

    private fun forBoolean(
        property: KMutableProperty<Boolean>,
    ): ISwitchCellAgent {
        return object : ISwitchCellAgent {
            override val isCheckable: Boolean = true
            override var isChecked: Boolean
                get() = property.getter.call()
                set(value) {
                    property.setter.call(value)
                }
        }
    }

}
