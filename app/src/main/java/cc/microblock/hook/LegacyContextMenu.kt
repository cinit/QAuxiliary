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

package cc.microblock.hook

import android.widget.LinearLayout
import cc.hicore.QApp.QAppUtils
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.hookBefore
import xyz.nextalone.util.method
import xyz.nextalone.util.set

@FunctionHookEntry
@UiItemAgentEntry
object LegacyContextMenu : CommonSwitchFunctionHook() {
    override val name = "老式消息菜单"
    override val description = "去除消息菜单的图标"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY
    override val isAvailable = QAppUtils.isQQnt()

    override fun initOnce(): Boolean {
        val dip2pxMethod = "Lcom/tencent/mobileqq/utils/ViewUtils;->dip2px(F)I".method
        val menuClass = Initiator.loadClass("com.tencent.qqnt.aio.menu.ui.QQCustomMenuExpandableLayout")
        val getBtnLayoutMethod = menuClass.method("o")!!
        getBtnLayoutMethod.hookBefore(this) {
            val defaultHeight = 71f
            val scale = 1.5f
            val height = dip2pxMethod.invoke(null, defaultHeight / scale)!!
            it.thisObject.set("n", height)
        }
        getBtnLayoutMethod.hookAfter(this) {
            (it.result as LinearLayout).removeViewAt(0)
        }
        return true
    }
}