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
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.SyncUtils

@FunctionHookEntry
@UiItemAgentEntry
object DisableConversationNotificationAndBubble : CommonSwitchFunctionHook(SyncUtils.PROC_MAIN) {

    private const val CFG_KEY = "MessagingStyleNotification.disableConversationNotificationAndBubble"

    override val isAvailable: Boolean
        get() = MessagingStyleNotification.isAvailable

    override val name: String = "禁用通知会话子渠道与气泡"

    override val description: String =
        "不创建通知会话子渠道与气泡，以在某些深度定制安卓中获得更好的通知体验"

    override val extraSearchKeywords: Array<String> =
        arrayOf("会话通知", "会话子渠道", "Bubble", "气泡通知")

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.NOTIFICATION_CATEGORY

    override var isEnabled: Boolean
        get() = ConfigManager.getDefaultConfig().getBooleanOrDefault(CFG_KEY, false)
        set(value) {
            ConfigManager.getDefaultConfig().putBoolean(CFG_KEY, value)
            MessagingStyleNotificationChannelCleaner.cleanupIfNeeded(value)
        }

    override fun initOnce(): Boolean = true
}
