/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2024 QAuxiliary developers
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

package cc.ioctl.hook.profile

import android.app.Activity
import cc.hicore.QApp.QAppUtils
import com.github.kyuubiran.ezxhelper.utils.ArgTypes
import com.github.kyuubiran.ezxhelper.utils.Args
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.github.kyuubiran.ezxhelper.utils.newInstance
import com.xiaoniu.util.ContextUtils
import io.github.qauxv.R
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.RecentPopup_onClickAction
import xyz.nextalone.util.get
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object OpenProfileCardMenu : CommonSwitchFunctionHook(
    targets = arrayOf(RecentPopup_onClickAction)
) {
    override val name = "打开资料卡"
    override val description = "在首页加号菜单中添加打开资料卡功能"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.PROFILE_CATEGORY
    override val isAvailable = QAppUtils.isQQnt()

    private fun context() = CommonContextWrapper.createMaterialDesignContext(ContextUtils.getCurrentActivity())

    override fun initOnce() = throwOrTrue {
        val menuItemId = 414414
        val entryMenuItem = Initiator.loadClass("com.tencent.widget.PopupMenuDialog\$MenuItem").newInstance(
            Args(arrayOf(menuItemId, "打开资料卡", "打开资料卡", R.drawable.ic_item_tool_72dp)),
            ArgTypes(arrayOf(Int::class.java, String::class.java, String::class.java, Int::class.java))
        )!!
        Initiator.loadClass("com.tencent.widget.PopupMenuDialog").getDeclaredMethod(
            "conversationPlusBuild",
            Activity::class.java,
            List::class.java,
            Initiator.loadClass("com.tencent.widget.PopupMenuDialog\$OnClickActionListener"),
            Initiator.loadClass("com.tencent.widget.PopupMenuDialog\$OnDismissListener")
        ).hookBefore {
            it.args[1] = it.args[1] as List<*> + listOf(entryMenuItem)
        }
        DexKit.requireMethodFromCache(RecentPopup_onClickAction).hookBefore {
            if (it.args[0].get("id") == menuItemId) {
                OpenProfileCard.onClick(context())
                it.result = null
            }
        }
    }
}