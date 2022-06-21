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

import android.content.Context
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
import io.github.qauxv.util.Log
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.requireMinQQVersion
import me.kyuubiran.util.getDefaultCfg
import xyz.nextalone.util.clazz
import xyz.nextalone.util.findHostView
import xyz.nextalone.util.get
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.method
import xyz.nextalone.util.putDefault
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object CleanRecentChat : CommonSwitchFunctionHook(intArrayOf(DexKit.N_FriendsStatusUtil_isChatAtTop)) {

    override val name = "清理最近聊天"

    override val description = "长按右上角加号"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY

    private const val RecentAdapter = "com.tencent.mobileqq.activity.recent.RecentAdapter"
    private const val RecentUserBaseData = "com.tencent.mobileqq.activity.recent.RecentUserBaseData"
    private const val RecentBaseData = "com.tencent.mobileqq.activity.recent.RecentBaseData"
    private val INCLUDE_TOPPED = "CleanRecentChat_include_topped"
    private var includeTopped = getDefaultCfg().getBooleanOrDefault(INCLUDE_TOPPED, false)

    override fun initOnce(): Boolean = throwOrTrue {
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
                                    handler(recentAdapter, app, all = false, other = false, includeTopped, dialog.context)
                                }
                                "清理其他消息" -> {
                                    handler(recentAdapter, app, all = false, other = true, includeTopped, dialog.context)
                                }
                                "清理所有消息" -> {
                                    handler(recentAdapter, app, all = true, other = true, includeTopped, dialog.context)
                                }
                            }
                        }
                    }
                    true
                }
            }
    }


    private fun handler(recentAdapter: Any?, app: Any?, all: Boolean, other: Boolean, includeTopped: Boolean, context: Context) {
        try {
            val list = recentAdapter.get(List::class.java) as List<*>
            val chatSize = list.size
            val method = RecentAdapter.clazz?.method(
                if (requireMinQQVersion(QQVersion.QQ_8_8_93)) "A" else "b",
                Void.TYPE,
                arrayListOf(RecentUserBaseData, RecentBaseData).clazz,
                String::class.java,
                String::class.java
            )

            method?.isAccessible = true
            var chatCurrentIndex = 0

            for (chatIndex in 0 until chatSize) {
                val chatItem = list[chatCurrentIndex]
                val mUser = chatItem.get("mUser")
                val uin = mUser.get("uin") as String
                val type = (mUser.get("type") as Int).toInt()
                val included = includeTopped || !isAtTop(app, uin, type, context)
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

    private fun isAtTop(app: Any?, str: String, i: Int, context: Context): Boolean {
        return try {
            DexKit.doFindMethod(DexKit.N_FriendsStatusUtil_isChatAtTop)?.invoke(null, app, str, i) as Boolean
        } catch (e: Throwable) {
            Log.e(e)
            Toasts.error(context, "检测置顶失败, 请联系开发者")
            true
        }
    }
}
