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

package xyz.nextalone.hook

import android.widget.ImageView
import android.widget.RelativeLayout
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.list.listItems
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.DexKit
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Log
import io.github.qauxv.util.Toasts
import me.kyuubiran.util.getDefaultCfg
import xyz.nextalone.util.*

@FunctionHookEntry
@UiItemAgentEntry
object CleanRecentChat : CommonSwitchFunctionHook() {

    override val name = "清理最近聊天"

    override val description = "长按右上角加号"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY

    private const val RecentAdapter = "com.tencent.mobileqq.activity.recent.RecentAdapter"
    private const val RecentUserBaseData = "com.tencent.mobileqq.activity.recent.RecentUserBaseData"
    private const val RecentBaseData = "com.tencent.mobileqq.activity.recent.RecentBaseData"
    private const val FriendsStatusUtil = "com.tencent.mobileqq.app.utils.FriendsStatusUtil"
    private val INCLUDE_TOPPED = "CleanRecentChat_include_topped"
    private var includeTopped = getDefaultCfg().getBooleanOrDefault(INCLUDE_TOPPED, false)

    override fun initOnce(): Boolean = tryOrFalse {
        DexKit.doFindMethod(DexKit.N_Conversation_onCreate)
            ?.hookAfter(this) {
                val recentAdapter = it.thisObject.get(RecentAdapter.clazz)
                val app = it.thisObject.get("mqq.app.AppRuntime".clazz)
                val relativeLayout = it.thisObject.get(RelativeLayout::class.java)
                val plusView = relativeLayout?.findHostView<ImageView>("ba3")
                    ?: relativeLayout?.parent?.findHostView<ImageView>("ba3")
                plusView?.setOnLongClickListener { view ->
                    val contextWrapper = CommonContextWrapper.createMaterialDesignContext(view.context)
                    val list = listOf("清理群消息", "清理其他消息", "清理所有消息")
                    MaterialDialog(contextWrapper).show {
                        title(text = "消息清理")
                        checkBoxPrompt(text = "包含置顶消息", isCheckedDefault = includeTopped) { checked ->
                            includeTopped = checked
                            putDefault(INCLUDE_TOPPED, includeTopped)
                        }
                        listItems(items = list) { dialog, _, text ->
                            Toasts.showToast(dialog.context, Toasts.TYPE_INFO, text, Toasts.LENGTH_SHORT)
                            when (text) {
                                "清理群消息" -> {
                                    handler(recentAdapter, app, all = false, other = false, includeTopped)
                                }
                                "清理其他消息" -> {
                                    handler(recentAdapter, app, all = false, other = true, includeTopped)
                                }
                                "清理所有消息" -> {
                                    handler(recentAdapter, app, all = true, other = true, includeTopped)
                                }
                            }
                        }
                    }
                    true
                }
            }
    }


    private fun handler(recentAdapter: Any?, app: Any?, all: Boolean, other: Boolean, includeTopped: Boolean) {
        try {
            val list = recentAdapter.get(List::class.java) as List<*>
            val chatSize = list.size
            val method = try {
                RecentAdapter.clazz?.method(
                    "b",
                    Void.TYPE,
                    RecentBaseData.clazz,
                    String::class.java,
                    String::class.java
                )
            } catch (e: Throwable) {
                RecentAdapter.clazz?.method(
                    "b",
                    Void.TYPE,
                    RecentUserBaseData.clazz,
                    String::class.java,
                    String::class.java
                )
            }
            method?.isAccessible = true
            var chatCurrentIndex = 0

            for (chatIndex in 0 until chatSize) {
                val chatItem = list[chatCurrentIndex]
                val mUser = chatItem.get("mUser")
                val uin = mUser.get("uin") as String
                val type = (mUser.get("type") as Int).toInt()
                val included = includeTopped || !isAtTop(app, uin, type)
                if (included && ((type == 1) && !other || (type !in arrayListOf(0, 1) && other) || all)) {
                    method?.invoke(recentAdapter, chatItem, "删除", "2")
                    continue
                }
                chatCurrentIndex++
            }
        } catch (e: Throwable) {
            Log.e(e)
        }
    }

    private fun isAtTop(app: Any?, str: String, i: Int): Boolean {
        return try {
            FriendsStatusUtil.clazz?.method("a", Boolean::class.java, Initiator._QQAppInterface(), String::class.java, Int::class.java)
                ?.invoke(null, app, str, i) as Boolean
        } catch (e: Throwable) {
            Log.e(e)
            false
        }
    }
}
