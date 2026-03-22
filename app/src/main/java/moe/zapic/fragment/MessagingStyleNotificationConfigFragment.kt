/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2025 QAuxiliary developers
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

package moe.zapic.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.github.qauxv.base.ISwitchCellAgent
import io.github.qauxv.core.HookInstaller
import io.github.qauxv.dsl.item.DslTMsgListItemInflatable
import io.github.qauxv.dsl.item.TextSwitchItem
import io.github.qauxv.fragment.BaseHierarchyFragment
import moe.zapic.hook.MessagingStyleNotification

class MessagingStyleNotificationConfigFragment : BaseHierarchyFragment() {

    override fun doOnCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        title = "MessagingStyle通知设置"
        return super.doOnCreateView(inflater, container, savedInstanceState)
    }

    override val hierarchy: Array<DslTMsgListItemInflatable> by lazy {
        arrayOf(
            TextSwitchItem(
                "总开关",
                summary = "开启后将通知将会替换为 MessagingStyle 通知",
                switchAgent = createMainSwitchAgent()
            ),
            TextSwitchItem(
                "禁用子渠道发送通知",
                summary = "不通过会话子渠道发送通知，以降低通知渠道管理复杂度",
                switchAgent = createSwitchAgent(
                    getter = { MessagingStyleNotification.disableConversationSubChannel },
                    setter = { MessagingStyleNotification.disableConversationSubChannel = it }
                )
            ),
            TextSwitchItem(
                "禁用通知气泡",
                summary = "禁用通知气泡，以适配部分系统无法单独控制气泡功能或气泡功能异常的状况",
                switchAgent = createSwitchAgent(
                    getter = { MessagingStyleNotification.disableBubble },
                    setter = { MessagingStyleNotification.disableBubble = it }
                )
            )
        )
    }

    private fun createMainSwitchAgent(): ISwitchCellAgent = object : ISwitchCellAgent {
        override val isCheckable: Boolean = true
        override var isChecked: Boolean
            get() = MessagingStyleNotification.isEnabled
            set(value) {
                MessagingStyleNotification.isEnabled = value
                if (value && !MessagingStyleNotification.isInitialized) {
                    HookInstaller.initializeHookForeground(requireContext(), MessagingStyleNotification)
                }
            }
    }

    private fun createSwitchAgent(
        getter: () -> Boolean,
        setter: (Boolean) -> Unit
    ): ISwitchCellAgent = object : ISwitchCellAgent {
        override val isCheckable: Boolean = true
        override var isChecked: Boolean
            get() = getter()
            set(value) {
                setter(value)
            }
    }
}
