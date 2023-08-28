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

package cc.microblock.hook

import android.app.Activity
import android.content.DialogInterface
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import cc.hicore.QApp.QAppUtils
import cc.ioctl.hook.ui.main.ContactListSortHook
import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import io.github.qauxv.R
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.core.HookInstaller
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.util.dexkit.AIOSendMsg
import io.github.qauxv.util.dexkit.DexKit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import me.ketal.hook.ChatItemShowQQUin
import xyz.nextalone.util.get
import xyz.nextalone.util.set

@FunctionHookEntry
@UiItemAgentEntry
object ImageCustomSummary : CommonConfigFunctionHook("ImageCustomSummary", arrayOf(AIOSendMsg)) {
    override val name = "图片/表情包附加自定义概要"

    override val description = "就是改那个在消息概要里显示的"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY

    override fun initOnce(): Boolean {
        DexKit.requireMethodFromCache(AIOSendMsg).hookBefore {
            Log.e("AA");
            for(element in (it.args[0] as List<Any>)){
                if(element.get("d") != null){
                    val picElement = element.get("d");
                    picElement?.set("e", summaryText);
                    picElement?.set("d", 0); // subType
                }
            }
        }
        return true;
    }

    override val valueState: MutableStateFlow<String?> by lazy {
        MutableStateFlow(if (isEnabled) "已开启" else "禁用")
    }

    var summaryText: String
        get() {
            val cfg = ConfigManager.getDefaultConfig()
            val summary = cfg.getString("customSummary.summaryText")
            if(summary == null) return "喵喵喵"
            else return summary
        }
        set(value) {
            val cfg = ConfigManager.getDefaultConfig()
            cfg.putString("customSummary.summaryText", value)
        }

    override val onUiItemClickListener: (IUiItemAgent, Activity, View) -> Unit = { _, ctx, _ ->
        val builder = AlertDialog.Builder(ctx)
        val root = LinearLayout(ctx)
        root.orientation = LinearLayout.VERTICAL

        val enable = CheckBox(ctx)
        enable.text = "启用自定义概要"
        enable.isChecked = isEnabled


        val summaryTextLabel = AppCompatTextView(ctx).apply {
            setText("文本内容")
        }

        val summaryTextEdit: EditText = AppCompatEditText(ctx).apply {
            setText(summaryText)
            textSize = 16f
            setTextColor(ctx.resources.getColor(R.color.firstTextColor, ctx.theme))
            hint = "文本内容"
        }

        // TODO: complete this
        val rangeTextLabel = AppCompatTextView(ctx).apply {
            setText("生效联系人列表（,分割）")
        }

        val rangeTextEdit: EditText = EditText(ctx).apply {
            setText("")
            textSize = 16f
            setTextColor(ctx.resources.getColor(R.color.firstTextColor, ctx.theme))
            hint = "114514, 1919810"
        }

        root.apply {
            addView(enable)
            addView(summaryTextLabel)
            addView(summaryTextEdit)
        }

        builder.setView(root)
            .setTitle("自定义图片概要设置")
            .setPositiveButton("确定") { dialog: DialogInterface?, which: Int ->
                this.isEnabled = enable.isChecked
                this.summaryText = summaryTextEdit.text.toString()

                ImageCustomSummary.valueState.update { if (isEnabled) "已开启" else "禁用" }

                if (isEnabled && !isInitialized) {
                    HookInstaller.initializeHookForeground(ctx, ContactListSortHook.INSTANCE)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override val isAvailable = QAppUtils.isQQnt();
}
