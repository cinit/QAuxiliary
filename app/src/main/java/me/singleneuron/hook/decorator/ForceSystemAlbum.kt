/*
 * QNotified - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 dmca@ioctl.cc
 * https://github.com/ferredoxin/QNotified
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/ferredoxin/QNotified/blob/master/LICENSE.md>.
 */
package me.singleneuron.hook.decorator

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.router.decorator.BaseSwitchFunctionDecorator
import io.github.qauxv.router.decorator.IStartActivityHookDecorator
import io.github.qauxv.router.dispacher.StartActivityHook
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.Log
import io.github.qauxv.util.xpcompat.XC_MethodHook
import me.singleneuron.activity.ChooseAgentActivity

@UiItemAgentEntry
@FunctionHookEntry
object ForceSystemAlbum : BaseSwitchFunctionDecorator(), IStartActivityHookDecorator {

    override val name = "可选使用系统相册"
    override val description = "支持8.3.6及更高"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.UI_CHAT_MSG
    override val dispatcher = StartActivityHook

    override fun onStartActivityIntent(intent: Intent, param: XC_MethodHook.MethodHookParam): Boolean {
        val isWinkHomeActivity = intent.component?.className?.contains("WinkHomeActivity") == true
        val isNewPhotoListActivity = intent.component?.className?.contains("NewPhotoListActivity") == true
        if ((isWinkHomeActivity || (isNewPhotoListActivity && intent.getIntExtra("uintype", -1) != -1)) &&
            (!intent.getBooleanExtra("PhotoConst.IS_CALL_IN_PLUGIN", false)) &&
            (!intent.getBooleanExtra("is_decorated", false))
        ) {
            // must use Activity context as base context to show dialog window
            val uin = intent.getStringExtra("uin")
            if (uin.toString().length > 10) {
                // Filter out calls in the guild
                return true
            }
            val context = param.thisObject as Context
            Log.d("context: ${context.javaClass.name}")
            val activityMap = mapOf(
                "系统相册" to Intent(context, ChooseAgentActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("use_ACTION_PICK", true)
                    putExtras(intent)
                    type = "image/*"
                },
                "系统文档" to Intent(context, ChooseAgentActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtras(intent)
                    type = "image/*"
                },
                "QQ 相册" to intent.apply {
                    putExtra("is_decorated", true)
                }
            )
            val materialContext = CommonContextWrapper.createMaterialDesignContext(context)
            MaterialAlertDialogBuilder(materialContext)
                .setTitle("选择相册")
                .setItems(activityMap.keys.toTypedArray()) { _: DialogInterface, i: Int ->
                    // recursion here
                    context.startActivity(activityMap[activityMap.keys.toTypedArray()[i]])
                }
                .create()
                .show()
            param.result = null
            return true
        }
        return false
    }
}
