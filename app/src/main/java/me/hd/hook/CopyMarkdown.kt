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

package me.hd.hook

import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import com.xiaoniu.dispatcher.OnMenuBuilder
import com.xiaoniu.util.ContextUtils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import io.github.qauxv.R
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.CustomMenu
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.dexkit.AbstractQQCustomMenuItem
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.util.SystemServiceUtils.copyToClipboard

@FunctionHookEntry
@UiItemAgentEntry
object CopyMarkdown : CommonSwitchFunctionHook(
    targets = arrayOf(AbstractQQCustomMenuItem)
), OnMenuBuilder {

    override val name = "复制Markdown消息"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_88)

    override fun initOnce(): Boolean {
        return true
    }

    override val targetComponentTypes = arrayOf("com.tencent.mobileqq.aio.msglist.holder.component.markdown.AIOMarkdownContentComponent")
    override fun onGetMenuNt(msg: Any, componentType: String, param: XC_MethodHook.MethodHookParam) {
        if (!isEnabled) return
        val item = CustomMenu.createItemIconNt(msg, "复制内容", R.drawable.ic_item_copy_72dp, R.id.item_copy_code) {
            val ctx = ContextUtils.getCurrentActivity()
            val msgRecord = XposedHelpers.callMethod(msg, "getMsgRecord") as MsgRecord
            msgRecord.elements.forEach { element ->
                element.markdownElement?.let { markdownElement ->
                    val content = markdownElement.content
                    copyToClipboard(ctx, content)
                    Toasts.success(ctx, "复制成功")
                }
            }
        }
        param.result = listOf(item) + param.result as List<*>
    }
}