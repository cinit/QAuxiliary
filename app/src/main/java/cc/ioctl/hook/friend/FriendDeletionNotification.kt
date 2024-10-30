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
package cc.ioctl.hook.friend

import android.app.Activity
import android.content.Context
import android.view.View
import cc.ioctl.fragment.ExfriendListFragment
import cc.ioctl.hook.DeletionObserver
import cc.ioctl.util.ExfriendManager
import io.github.qauxv.activity.SettingsUiFragmentHostActivity.Companion.startFragmentWithContext
import io.github.qauxv.base.IEntityAgent
import io.github.qauxv.base.ISwitchCellAgent
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.RuntimeErrorTracer
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.bridge.AppRuntimeHelper
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.BaseFunctionHook
import io.github.qauxv.hook.BasePlainUiAgentItem
import kotlinx.coroutines.flow.MutableStateFlow

@FunctionHookEntry
@UiItemAgentEntry
object FriendDeletionNotification : BaseFunctionHook(defaultEnabled = true), IUiItemAgent {

    override val titleProvider: (IEntityAgent) -> String = { "被删好友检测通知" }
    override val summaryProvider: ((IEntityAgent, Context) -> CharSequence?)? = { _, _ -> "检测到被删好友时将发出通知" }
    override val uiItemAgent = this
    override val runtimeErrors: List<Throwable> get() = DeletionObserver.INSTANCE.runtimeErrors
    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Auxiliary.FRIEND_CATEGORY
    override val valueState: MutableStateFlow<String?>? = null
    override val validator: ((IUiItemAgent) -> Boolean)? = null
    override var isEnabled = true
    override val onClickListener: ((IUiItemAgent, Activity, View) -> Unit)? = null
    override val extraSearchKeywordProvider: ((IUiItemAgent, Context) -> Array<String>?)? = null
    override val runtimeErrorDependentComponents: List<RuntimeErrorTracer>? = null

    override val switchProvider by lazy {
        object : ISwitchCellAgent {
            override var isChecked: Boolean
                get() {
                    val uin = AppRuntimeHelper.getLongAccountUin()
                    if (uin < 10000) {
                        return false
                    }
                    val exf = ExfriendManager.get(uin)
                    return exf.isNotifyWhenDeleted
                }
                set(value) {
                    val uin = AppRuntimeHelper.getLongAccountUin()
                    if (uin < 10000) {
                        return
                    }
                    val exf = ExfriendManager.get(uin)
                    exf.isNotifyWhenDeleted = value
                }

            override val isCheckable = AppRuntimeHelper.getLongAccountUin() >= 10000L
        }
    }

    @Throws(Exception::class)
    override fun initOnce(): Boolean {
        return DeletionObserver.INSTANCE.initialize()
    }

    @UiItemAgentEntry
    object ExFriendListEntry : BasePlainUiAgentItem(
        title = "历史好友",
        description = "得不到的永远在骚动, 被偏爱的都有恃无恐."
    ) {
        override val onClickListener: ((IUiItemAgent, Activity, View) -> Unit) = { _, activity, _ ->
            startFragmentWithContext(
                activity,
                ExfriendListFragment::class.java, null
            )
        }

        override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Auxiliary.FRIEND_CATEGORY
    }
}
