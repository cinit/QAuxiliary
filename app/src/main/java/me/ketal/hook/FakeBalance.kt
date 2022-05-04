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

package me.ketal.hook

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import cc.ioctl.util.Reflex
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.input.input
import io.github.qauxv.SyncUtils
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.util.hostInfo
import me.ketal.base.PluginDelayableHook
import me.ketal.data.ConfigData
import me.ketal.ui.view.ConfigView
import me.ketal.util.findClass
import me.ketal.util.getMethod
import me.ketal.util.ignoreResult
import xyz.nextalone.util.clazz
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object FakeBalance : PluginDelayableHook("ketal_qwallet_fakebalance") {

    override val preference = uiClickableItem {
        title = "自定义钱包余额"
        summary = "仅供娱乐"
        onClickListener = { _, it, _ ->
            showDialog(it, null)
        }
    }
    override val targetProcesses = SyncUtils.PROC_TOOL
    override val uiItemLocation = FunctionEntryRouter.Locations.Entertainment.ENTERTAIN_CATEGORY

    override val pluginID = "qwallet_plugin.apk"

    private val moneyKey = ConfigData<String>("ketal_qwallet_fakebalance_money")
    private var money
        get() = moneyKey.getOrDefault("114514")
        set(value) {
            moneyKey.value = value
        }

    fun listener(activity: Activity) = View.OnClickListener {
        showDialog(activity, null)
    }

    private fun showDialog(ctx: Context, textView: TextView?) {
        throwOrTrue {
            val vg = ConfigView(ctx)
            val dialog = MaterialDialog(ctx).show {
                title(text = "自定义钱包余额")
                input(hint = "请输入自定义金额...", prefill = money) { dialog, text ->
                    val enableFake = vg.isChecked
                    isEnabled = enableFake
                    if (enableFake) {
                        money = text.toString()
                        if (!isInitialized) {
                            initialize()
                        }
                    }
                    dialog.dismiss()
                    textView?.text = "114514"
                }.ignoreResult()
                positiveButton(text = "保存")
                negativeButton(text = "取消")
            }
            vg.setText("启用自定义钱包余额")
            vg.view = dialog.getCustomView()
            vg.isChecked = isEnabled
            dialog.view.contentLayout.customView = null
            dialog.customView(view = vg)
        }
    }

    override fun startHook(classLoader: ClassLoader) = throwOrTrue {
        arrayOf(
            "Lcom/qwallet/activity/QWalletHomeActivity;->onCreate(Landroid/os/Bundle;)V",
            "Lcom/qwallet/activity/QvipPayWalletActivity;->onCreate(Landroid/os/Bundle;)V"
        ).getMethod(classLoader)
            ?.hookAfter(this) {
                val ctx = it.thisObject as Activity
                val id = ctx.resources.getIdentifier("root", "id", hostInfo.packageName)
                val rootView = ctx.findViewById<ViewGroup>(id)
                val headerClass = "com.qwallet.view.QWalletHeaderView".findClass(classLoader)
                val headerView = Reflex.getFirstByType(rootView, headerClass)
                val numAnimClass = "com.tencent.mobileqq.activity.qwallet.widget.NumAnim".clazz
                    ?: "com.tencent.mobileqq.qwallet.widget.NumAnim".clazz
                for (f in headerClass.declaredFields) {
                    if (f.type == numAnimClass) {
                        f.isAccessible = true
                        val numAnim = f.get(headerView)
                        val tv = Reflex.getFirstByType(numAnim, TextView::class.java)
                        tv.doAfterTextChanged { v ->
                            if (isEnabled && v.toString() != money)
                                tv.text = money
                        }
                        tv.setOnLongClickListener { v ->
                            showDialog(v.context, tv)
                            true
                        }
                    }
                }
            }

        arrayOf("Lcom/qwallet/activity/QWalletHomeActivity;->onViewCreated(Landroid/view/View;Landroid/os/Bundle;)V").getMethod(classLoader)
            ?.hookAfter(this) {
                val mAct = it.thisObject
                val headerView = Reflex.getFirstByType(mAct, "com.qwallet.view.QWalletHeaderView".clazz) as ViewGroup
                val headerClass = "com.qwallet.view.QWalletHeaderView".findClass(classLoader)
                val numAnimClass = "com.tencent.mobileqq.activity.qwallet.widget.NumAnim".clazz
                    ?: "com.tencent.mobileqq.qwallet.widget.NumAnim".clazz
                for (f in headerClass.declaredFields) {
                    if (f.type == numAnimClass) {
                        f.isAccessible = true
                        val numAnim = f.get(headerView)
                        val tv = Reflex.getFirstByType(numAnim, TextView::class.java)
                        tv.doAfterTextChanged { v ->
                            if (isEnabled && v.toString() != money)
                                tv.text = money
                        }
                        tv.setOnLongClickListener { v ->
                            showDialog(v.context, tv)
                            true
                        }
                    }
                }
            }
    }
}
