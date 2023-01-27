/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
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

package cc.ioctl.hook.qzone

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import cc.ioctl.util.Reflex
import cc.ioctl.util.hookAfterIfEnabled
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.requireMinQQVersion

/**
 * Hide QZone-Moment title bar entrance
 */
@[FunctionHookEntry UiItemAgentEntry]
object HideQZMTitleBarEntrance : CommonSwitchFunctionHook(
    targetProc = SyncUtils.PROC_MAIN or SyncUtils.PROC_QZONE
) {

    override val name = "隐藏QQ空间此刻按钮"

    override val description = "隐藏QQ空间动态顶端的\"此刻\"按钮, 未经测试"

    // currently experimental
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.UI_MISC

    override val isAvailable: Boolean get() = requireMinQQVersion(QQVersion.QQ_8_9_25)

    override fun initOnce(): Boolean {
        val kQZMTitleBarEntranceManager = Initiator.loadClass("com.qzone.reborn.qzmoment.itemview.QZMTitleBarEntranceManager")
        // com.tencent.mobileqq.zplan_impl.R.id.qzm_entrance_root
        val qzm_entrance_root = Initiator.loadClass("com.tencent.mobileqq.R\$id")
            .getDeclaredField("qzm_entrance_root").get(null) as Int
        check(qzm_entrance_root != 0) { "qzm_entrance_root not found" }
        hookAfterIfEnabled(
            Reflex.findSingleMethod(
                kQZMTitleBarEntranceManager, Void.TYPE, false,
                Context::class.java, ViewGroup::class.java, ImageView::class.java
            ),
        ) {
            // Log.e("HideQZMTitleBarEntrance here", Throwable())
            val view = it.args[1] as ViewGroup
            val entrance: View? = view.findViewById(qzm_entrance_root)
            entrance?.visibility = View.GONE
            it.result = null
        }
        return true
    }

}
