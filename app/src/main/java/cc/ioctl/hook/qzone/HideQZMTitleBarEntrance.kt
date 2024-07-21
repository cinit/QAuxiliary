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

package cc.ioctl.hook.qzone

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import cc.ioctl.util.HookUtils.hookBeforeIfEnabled
import cc.ioctl.util.Reflex
import cc.ioctl.util.hookAfterIfEnabled
import cc.ioctl.util.hookBeforeIfEnabled
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import io.github.qauxv.util.xpcompat.XC_MethodHook.MethodHookParam
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.NQZMoment_EntranceEnabled
import io.github.qauxv.util.dexkit.QZoneFeedxTopEntranceMethod
import io.github.qauxv.util.requireMinQQVersion

/**
 * Hide QZone-Moment title bar entrance
 */
@[FunctionHookEntry UiItemAgentEntry]
object HideQZMTitleBarEntrance : CommonSwitchFunctionHook(
    targetProc = SyncUtils.PROC_MAIN or SyncUtils.PROC_QZONE,
    targets = arrayOf(NQZMoment_EntranceEnabled, QZoneFeedxTopEntranceMethod)
) {

    override val name = "隐藏QQ空间动态的\"此刻\""

    override val description = "隐藏QQ空间动态的\"此刻\"按钮或横幅"

    // currently experimental
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.UI_MISC

    override val isAvailable: Boolean get() = requireMinQQVersion(QQVersion.QQ_8_9_25)

    private val qZoneFeedxTopEntranceMethodName = when {
        requireMinQQVersion(QQVersion.QQ_9_0_30) -> "c0"
        requireMinQQVersion(QQVersion.QQ_9_0_25) -> "e0"
        requireMinQQVersion(QQVersion.QQ_9_0_20) -> "f0"
        else -> "e0"
    }

    override fun initOnce(): Boolean {
        try {
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
        } catch (_: Exception) {
            DexKit.requireMethodFromCache(NQZMoment_EntranceEnabled).hookBefore { it.result = false }
        }

        try {
            val qZoneFeedxTopEntranceClass = Initiator.loadClass("com.qzone.reborn.feedx.widget.entrance.QZoneFeedxTopEntranceManagerView;")
            val qZoneFeedxTopEntranceMethod = qZoneFeedxTopEntranceClass.getDeclaredMethod(qZoneFeedxTopEntranceMethodName)
            hookBeforeIfEnabled(this, qZoneFeedxTopEntranceMethod) { param: MethodHookParam ->
                val obj = param.thisObject as View
                obj.isClickable = false
                param.setResult(null)
            }
        } catch (_: Exception) {
            hookBeforeIfEnabled(DexKit.requireMethodFromCache(QZoneFeedxTopEntranceMethod)){
                (it.thisObject as View).isClickable = false
                it.result = null
            }
        }
        return true
    }

}
