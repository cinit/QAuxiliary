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

package cc.ioctl.hook.qwallet

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import cc.ioctl.util.Reflex.getFirstByType
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import me.ketal.base.PluginDelayableHook
import me.ketal.util.getField
import me.ketal.util.getMethod
import xyz.nextalone.util.clazz
import xyz.nextalone.util.getIdentifier
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.throwOrTrue

//@FunctionHookEntry
//@UiItemAgentEntry
object QWalletNoAD : PluginDelayableHook("ketal_qwallet_noad") {
    override val preference = uiSwitchPreference {
        title = "隐藏QQ钱包超值精选"
    }
    override val targetProcesses = SyncUtils.PROC_TOOL
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.SLIDING_UI

    override val pluginID = "qwallet_plugin.apk"

    override val isAvailable: Boolean get() = requireMinQQVersion(QQVersion.QQ_8_2_0)

    override fun startHook(classLoader: ClassLoader) = throwOrTrue {
        arrayOf("Lcom/qwallet/activity/QWalletHomeActivity;->onCreate(Landroid/os/Bundle;)V", "Lcom/qwallet/activity/QvipPayWalletActivity;->onCreate(Landroid/os/Bundle;)V").getMethod(classLoader)
            ?.hookAfter(this) {
                val ctx = it.thisObject as Activity
                val id = ctx.getIdentifier("id", "root")!!
                val rootView = ctx.findViewById<ViewGroup>(id)
                val midView = rootView.getChildAt(rootView.childCount - 1)
                if (!requireMinQQVersion(QQVersion.QQ_8_8_17)) {
                    rootView.removeView(midView)
                }
                val headerView =
                    "Lcom/qwallet/view/QWalletHeaderViewRootLayout;->a:Lcom/qwallet/view/QWalletHeaderView;"
                        .getField(classLoader)
                        ?.get(rootView) as ViewGroup
                headerView.viewTreeObserver.addOnGlobalLayoutListener(object :
                    OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                        val webView = getFirstByType(headerView,
                                "com.tencent.biz.ui.TouchWebView".clazz) as? View
                                ?: return
                        headerView.removeView(webView)
                        headerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                })
            }
        arrayOf("Lcom/qwallet/activity/QWalletHomeActivity;->onViewCreated(Landroid/view/View;Landroid/os/Bundle;)V").getMethod(classLoader)
            ?.hookAfter(this) {
                val mAct = it.thisObject
                val headerView = getFirstByType(mAct,"com.qwallet.view.QWalletHeaderView".clazz) as ViewGroup

                headerView.viewTreeObserver.addOnGlobalLayoutListener(object :
                    OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        val webView =
                            getFirstByType(headerView,
                                "com.tencent.biz.ui.TouchWebView".clazz) as? View
                                ?: return
                        headerView.removeView(webView)
                        headerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                })
            }
    }
}
