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

import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.forEach
import cc.ioctl.hook.OpenProfileCard
import de.robv.android.xposed.XC_MethodHook
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Log
import io.github.qauxv.util.isTim
import me.ketal.dispacher.OnBubbleBuilder
import me.singleneuron.data.MsgRecordData
import xyz.nextalone.util.clazz
import xyz.nextalone.util.findHostView
import xyz.nextalone.util.get
import xyz.nextalone.util.invoke
import xyz.nextalone.util.method

@UiItemAgentEntry
object ShowMsgAt : CommonSwitchFunctionHook(), OnBubbleBuilder {

    override val name = "消息显示At对象"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY
    override val extraSearchKeywords: Array<String> = arrayOf("@", "艾特")

    override fun initOnce() = !isTim()

    override fun onGetView(
        rootView: ViewGroup,
        chatMessage: MsgRecordData,
        param: XC_MethodHook.MethodHookParam
    ) {
        if (!isEnabled || 1 != chatMessage.isTroop) return
        val textMsgType = "com.tencent.mobileqq.data.MessageForText".clazz!!
        val extStr = chatMessage.msgRecord.invoke(
            "getExtInfoFromExtStr",
            "troop_at_info_list", String::class.java
        ) ?: return
        if ("" == extStr) return
        val atList = (textMsgType.method("getTroopMemberInfoFromExtrJson")
            ?.invoke(null, extStr) ?: return) as List<*>
        when (val content = rootView.findHostView<View>("chat_item_content_layout")) {
            is TextView -> {
                copeAtInfo(content, atList)
            }
            is ViewGroup -> {
                content.forEach {
                    if (it is TextView)
                        copeAtInfo(it, atList)
                }
            }
            else -> {
                Log.d("暂不支持的控件类型--->$content")
                return
            }
        }
    }

    private fun copeAtInfo(textView: TextView, atList: List<*>) {
        val spannableString = SpannableString(textView.text)
        atList.forEach {
            val uin = it.get("uin") as Long
            val start = (it.get("startPos") as Short).toInt()
            val length = it.get("textLen") as Short
            if (start < spannableString.length) {
                if (spannableString[start] == '@') {
                    spannableString.setSpan(ProfileCardSpan(uin), start, start + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            } else if (start == spannableString.length) {
                // workaround for host bug: start index is at the end of the string
                // there is a space at the end of the at string
                val possibleStart = start - length - 1
                if (possibleStart >= 0 && spannableString[possibleStart] == '@') {
                    spannableString.setSpan(ProfileCardSpan(uin), possibleStart, possibleStart + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            } else {
                Log.e("艾特信息超出范围")
                Log.e("start:$start, length:$length")
                Log.e("text:'${textView.text}', length:${textView.text.length}")
            }
        }
        textView.text = spannableString
        textView.movementMethod = LinkMovementMethod.getInstance()
    }
}

class ProfileCardSpan(val qq: Long) : ClickableSpan() {
    override fun onClick(v: View) {
        // 0 for @all
        if (qq > 10000) {
            OpenProfileCard.openUserProfileCard(v.context, qq)
        }
    }
}
