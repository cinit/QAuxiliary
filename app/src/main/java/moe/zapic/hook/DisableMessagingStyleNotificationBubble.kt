/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the qwq233 Universal License
 * as published on https://github.com/qwq233/license; either
 * version 2 of the License, or any later version and our EULA as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the qwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package moe.zapic.hook

import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.SyncUtils

@FunctionHookEntry
@UiItemAgentEntry
object DisableMessagingStyleNotificationBubble : CommonSwitchFunctionHook(SyncUtils.PROC_MAIN) {

    override val isAvailable: Boolean
        get() = MessagingStyleNotification.isAvailable

    override val name: String = "禁用通知气泡"

    override val description: String =
        "禁用会话通知气泡，以适配部分系统无法单独控制气泡功能或气泡功能异常的状况"

    override val extraSearchKeywords: Array<String> =
        arrayOf("Bubble", "气泡通知", "会话通知", "通知气泡")

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.NOTIFICATION_CATEGORY

    override var isEnabled: Boolean
        get() = MessagingStyleNotificationConfig.disableBubble
        set(value) {
            MessagingStyleNotificationConfig.disableBubble = value
        }

    override fun initOnce(): Boolean = true
}
