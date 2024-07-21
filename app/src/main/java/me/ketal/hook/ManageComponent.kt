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
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.view.View
import androidx.appcompat.app.AlertDialog
import io.github.qauxv.BuildConfig
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.hostInfo
import kotlinx.coroutines.flow.MutableStateFlow
import me.ketal.ui.activity.QFileShareToIpadActivity.Companion.ENABLE_SEND_TO_IPAD
import me.ketal.ui.activity.QFileShareToIpadActivity.Companion.ENABLE_SEND_TO_IPAD_STATUS
import me.ketal.ui.activity.QFileShareToIpadActivity.Companion.SEND_TO_IPAD_CMD
import me.ketal.util.getEnable
import me.ketal.util.setEnable

@FunctionHookEntry
@UiItemAgentEntry
object ManageComponent : CommonConfigFunctionHook("Ketal_ManageComponent") {

    override val name = "管理QQ组件"
    override val valueState: MutableStateFlow<String?>? = null

    override val onUiItemClickListener: (IUiItemAgent, Activity, View) -> Unit = { _, activity, _ ->
        showDialog(activity)
    }

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MISC_CATEGORY

    override fun initOnce() = true

    private val components = mapOf(
        "发送到我的电脑" to ComponentName(hostInfo.packageName, "com.tencent.mobileqq.activity.qfileJumpActivity"),
        "保存到QQ收藏" to ComponentName(hostInfo.packageName, "cooperation.qqfav.widget.QfavJumpActivity"),
        "面对面快传" to ComponentName(hostInfo.packageName, "cooperation.qlink.QlinkShareJumpActivity"),
        "发送到我的iPad" to ComponentName(BuildConfig.APPLICATION_ID, "me.ketal.ui.activity.QFileShareToIpadActivity")
    )

    private fun showDialog(context: Context) {
        val keys = components.keys.toTypedArray()
        val enable = keys.run {
            val ary = BooleanArray(size)
            forEachIndexed { i, k ->
                ary[i] = components[k]?.getEnable(context) == true
            }
            ary
        }
        val cache = enable.clone()
        AlertDialog.Builder(CommonContextWrapper.createAppCompatContext(context))
            .setTitle("选择要启用的组件")
            .setMultiChoiceItems(keys, enable) { _: DialogInterface, i: Int, _: Boolean ->
                cache[i] = !cache[i]
            }
            .setNegativeButton("取消", null)
            .setPositiveButton("确定") { _: DialogInterface, _: Int ->
                cache.forEachIndexed { i, b ->
                    if (!cache[keys.indexOf("发送到我的电脑")] && cache[keys.indexOf("发送到我的iPad")]) {
                        Toasts.error(context, "启用发送到iPad需启用发送到电脑")
                        return@setPositiveButton
                    }
                    val k = keys[i]
                    if (k == "发送到我的iPad" && b != components[k]?.getEnable(context)) {
                        val intent = Intent().apply {
                            component = ComponentName(BuildConfig.APPLICATION_ID, "io.github.qauxv.activity.ConfigV2Activity")
                            putExtra(SEND_TO_IPAD_CMD, ENABLE_SEND_TO_IPAD)
                            putExtra(ENABLE_SEND_TO_IPAD_STATUS, b)
                        }
                        Toasts.info(context, "已保存组件状态")
                        context.startActivity(intent)
                        return@setPositiveButton
                    }
                    components[k]?.setEnable(context, b)
                }
                Toasts.info(context, "已保存组件状态")
            }
            .show()
    }
}
