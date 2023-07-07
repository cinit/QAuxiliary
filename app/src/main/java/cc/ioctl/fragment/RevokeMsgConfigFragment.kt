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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cc.ioctl.hook.msg.RevokeMsgHook
import io.github.qauxv.base.ISwitchCellAgent
import io.github.qauxv.core.HookInstaller
import io.github.qauxv.dsl.item.DslTMsgListItemInflatable
import io.github.qauxv.dsl.item.TextSwitchItem
import io.github.qauxv.fragment.BaseHierarchyFragment
import java.util.Locale
import kotlin.reflect.KMutableProperty

class RevokeMsgConfigFragment : BaseHierarchyFragment() {

    override fun doOnCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        title = "防撤回设置"
        return super.doOnCreateView(inflater, container, savedInstanceState)
    }

    override val hierarchy: Array<DslTMsgListItemInflatable> by lazy {
        arrayOf(
            TextSwitchItem(
                "总开关",
                summary = "关闭后将不会拦截撤回消息",
                switchAgent = object : ISwitchCellAgent {
                    override var isChecked: Boolean
                        get() = RevokeMsgHook.INSTANCE.isEnabled
                        set(value) {
                            RevokeMsgHook.INSTANCE.setEnabled(value)
                            if (value && !RevokeMsgHook.INSTANCE.isInitialized) {
                                HookInstaller.initializeHookForeground(requireContext(), RevokeMsgHook.INSTANCE)
                            }
                        }
                    override val isCheckable: Boolean = true
                }
            ),
            TextSwitchItem(
                "显示消息 shmsgseq",
                summary = "在撤回提示灰字中显示被撤回消息的 shmsgseq",
                switchAgent = createSwitchAgent(RevokeMsgHook.INSTANCE, "isShowShmsgseqEnabled")
            ),
            /* 坏了
            TextSwitchItem(
                "保留自己撤回的消息",
                summary = null,
                switchAgent = createSwitchAgent(RevokeMsgHook.INSTANCE, "isKeepSelfMsgEnabled")
            )*/
        )
    }

    private inline fun <reified T> createSwitchAgent(
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

    private fun <T : Any> createSwitchAgent(
        owner: T,
        property: KMutableProperty<Boolean>
    ): ISwitchCellAgent {
        return createSwitchAgent(owner, property.name)
    }

    private fun <T : Any> createSwitchAgent(
        owner: T,
        name: String
    ): ISwitchCellAgent {
        if (!name.startsWith("is")) {
            throw NoSuchMethodException("property name must start with 'is'")
        }
        val setterName = "set" + name.substring(2).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
        val getter = owner.javaClass.getMethod(name)
        val setter = owner.javaClass.getMethod(setterName, java.lang.Boolean.TYPE)
        return object : ISwitchCellAgent {
            override val isCheckable: Boolean = true
            override var isChecked: Boolean
                get() = getter.invoke(owner) as Boolean
                set(value) {
                    setter.invoke(owner, value)
                }
        }
    }
}
