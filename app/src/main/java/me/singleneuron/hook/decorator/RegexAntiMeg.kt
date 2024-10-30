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

import android.app.Activity
import android.view.View
import android.widget.EditText
import cc.ioctl.util.Reflex
import cc.ioctl.hook.notification.MessageInterception
import cc.ioctl.util.msg.MessageReceiver
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.qauxv.util.xpcompat.XposedHelpers
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.RuntimeErrorTracer
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.Initiator
import kotlinx.coroutines.flow.MutableStateFlow
import io.github.qauxv.config.ConfigManager.getExFriendCfg
import me.singleneuron.data.MsgRecordData

@UiItemAgentEntry
@FunctionHookEntry
object RegexAntiMeg : CommonConfigFunctionHook(), MessageReceiver {

    override val name = "万象屏蔽卡片消息"
    override val description = "使用强大的正则表达式自由屏蔽卡片消息"
    override val valueState: MutableStateFlow<String?>? = null
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.NOTIFICATION_CATEGORY
    override val runtimeErrorDependentComponents: List<RuntimeErrorTracer> = listOf(MessageInterception)

    private var regexCache: Regex? = null
    private var regexStringCache: String = ""

    override fun onReceive(data: MsgRecordData?): Boolean {
        try {
            if (data == null) return false
            val regexString = getExFriendCfg()?.getStringOrDefault(RegexAntiMeg::class.simpleName!!, "")
            if (regexString.isNullOrBlank()) return false
            return when {
                Initiator.loadClass("com.tencent.mobileqq.data.MessageForStructing")
                        .isAssignableFrom(data.javaClass) -> {
                    val text = Reflex.invokeVirtual(
                            Reflex.getInstanceObjectOrNull(data, "structingMsg"),
                            "getXml", *arrayOfNulls(0)
                    ) as String
                    processMsg(data, text, regexString)
                }
                Initiator.loadClass("com.tencent.mobileqq.data.MessageForArkApp")
                        .isAssignableFrom(data.javaClass) -> {
                    val text = Reflex.invokeVirtual(
                            Reflex.getInstanceObjectOrNull(data, "ark_app_message"),
                            "toAppXml", *arrayOfNulls(0)
                    ) as String
                    processMsg(data, text, regexString)
                }
                else -> false
            }
        } catch (e: Exception) {
            traceError(e)
            return false
        }
    }

    override fun initOnce(): Boolean {
        return MessageInterception.initialize()
    }

    private fun processMsg(data: MsgRecordData, text: String, regexString: String): Boolean {
        if (regexStringCache != regexString) {
            regexCache = regexString.toRegex()
            regexStringCache = regexString
        }
        return if (regexCache?.matches(text) == true) {
            XposedHelpers.setBooleanField(data.msgRecord, "isread", true)
            true
        } else false
    }

    override val onUiItemClickListener: (IUiItemAgent, Activity, View) -> Unit = { _, activity, _ ->
        val dialogContext = CommonContextWrapper.createMaterialDesignContext(activity)
        MaterialAlertDialogBuilder(dialogContext).apply {
            setTitle("设置正则表达式")
            setMessage("请输入正则表达式，以便屏蔽指定的卡片消息")
            val editTextPreference: EditText = EditText(dialogContext).apply {
                setText(getExFriendCfg()?.getStringOrDefault(RegexAntiMeg::class.simpleName!!, ""))
                hint = "留空以禁用"
            }
            setView(editTextPreference)
            setPositiveButton("确定") { _, _ ->
                val regexString = editTextPreference.text.toString()
                getExFriendCfg()?.putString(RegexAntiMeg::class.simpleName!!, regexString)
            }
            setNegativeButton("取消") { _, _ -> }
        }.show()
    }
}
