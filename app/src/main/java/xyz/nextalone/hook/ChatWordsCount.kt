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
package xyz.nextalone.hook

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.*
import cc.ioctl.util.LayoutHelper
import io.github.qauxv.base.IUiItemAgent
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonConfigFunctionHook
import io.github.qauxv.ui.CustomDialog
import io.github.qauxv.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import me.kyuubiran.util.getExFriendCfg
import me.kyuubiran.util.showToastByTencent
import me.singleneuron.tlb.ConfigTable.getConfig
import xyz.nextalone.util.*
import java.util.*

@FunctionHookEntry
@UiItemAgentEntry
object ChatWordsCount : CommonConfigFunctionHook("na_chat_words_count_kt", intArrayOf(DexKit.N_QQSettingMe_onResume)) {

    override val name = "聊天字数统计"
    override val valueState: MutableStateFlow<String?>? = null

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY

    override val onUiItemClickListener: (IUiItemAgent, Activity, View) -> Unit = { _, activity, _ ->
        showChatWordsCountDialog(activity)
    }

    private const val UIN_CONFIG_ERROR_MESSAGE = "未登录或无法获取当前账号信息"

    private const val msgCfg = "na_chat_words_count_kt_msg"
    private const val wordsCfg = "na_chat_words_count_kt_words"
    private const val emoCfg = "na_chat_words_count_kt_emo"
    private const val timeCfg = "na_chat_words_count_kt_time"
    private const val colorCfg = "na_chat_words_count_kt_color"
    private const val strCfg = "na_chat_words_count_kt_str"
    private fun getChatWords(): String {
        return getExFriendCfg()?.let {
            val isToday = Date().today == it.getStringOrDefault(timeCfg, "")
            val str = it.getStringOrDefault(strCfg, "今日已发送 %1 条消息，共 %2 字，表情包 %3 个")
            val msg = if (isToday) it.getIntOrDefault(msgCfg, 0) else 0
            val words = if (isToday) it.getIntOrDefault(wordsCfg, 0) else 0
            val emo = if (isToday) it.getIntOrDefault(emoCfg, 0) else 0
            str.replace("%1", msg.toString()).replace("%2", words.toString())
                .replace("%3", emo.toString())
        } ?: UIN_CONFIG_ERROR_MESSAGE
    }

    override fun initOnce() = throwOrTrue {
        "com.tencent.mobileqq.activity.QQSettingMe".clazz?.hookBeforeAllConstructors {
            val viewGroup = it.args[2] as ViewGroup
            DexKit.doFindMethod(DexKit.N_QQSettingMe_onResume)?.hookAfter(this) {
                val relativeLayout = viewGroup.findHostView<RelativeLayout>(getConfig(ChatWordsCount::class.java.simpleName))
                val textView = (relativeLayout?.parent as FrameLayout).findViewById<TextView>(io.github.qauxv.R.id.chat_words_count)
                textView.text = getChatWords()
            }
        }
        "com.tencent.mobileqq.activity.QQSettingMe".clazz?.hookAfterAllConstructors {
            val activity = it.args[0] as Activity
            val relativeLayout = (it.args[2] as ViewGroup).findHostView<RelativeLayout>(getConfig(ChatWordsCount::class.java.simpleName))
            relativeLayout?.visibility = View.GONE
            val textView = TextView(activity)
            textView.text = getChatWords()
            textView.setTextColor(
                Color.parseColor(
                    getExFriendCfg()?.getString(
                        colorCfg
                    ) ?: "#FF000000"
                )
            )
            textView.id = io.github.qauxv.R.id.chat_words_count
            textView.textSize = 15.0f
            textView.setOnClickListener {
                val dialog = CustomDialog.createFailsafe(activity)
                val ctx = dialog.context
                val editText = EditText(ctx)
                editText.setText(getExFriendCfg()?.getString(colorCfg) ?: "#ff000000")
                editText.textSize = 16f
                val _5 = LayoutHelper.dip2px(activity, 5f)
                editText.setPadding(_5, _5, _5, _5 * 2)
                val linearLayout = LinearLayout(ctx)
                linearLayout.addView(
                    editText,
                    LayoutHelper.newLinearLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        _5 * 2
                    )
                )
                val alertDialog = dialog
                    .setTitle("输入聊天字数统计颜色")
                    .setView(linearLayout)
                    .setPositiveButton("确认") { _, _ ->
                    }
                    .setNegativeButton("取消", null)
                    .setNeutralButton("使用默认值") { _, _ ->
                        putExFriend(colorCfg, "#FF000000")
                        Toasts.showToast(
                            activity,
                            Toasts.TYPE_INFO,
                            "重启以应用设置",
                            Toast.LENGTH_SHORT
                        )
                    }
                    .create() as AlertDialog
                alertDialog.show()
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val color = editText.text.toString()
                    try {
                        Color.parseColor(color)
                        putExFriend(colorCfg, color)
                        alertDialog.cancel()
                        Toasts.showToast(
                            activity,
                            Toasts.TYPE_INFO,
                            "重启以应用设置",
                            Toast.LENGTH_SHORT
                        )
                    } catch (e: IllegalArgumentException) {
                        Toasts.showToast(
                            activity,
                            Toasts.TYPE_ERROR,
                            "颜色格式不正确",
                            Toast.LENGTH_SHORT
                        )
                    }
                }
                textView.setOnLongClickListener {
                    CustomDialog.createFailsafe(activity).setTitle("聊天字数统计设置").setMessage("是否要重置统计记录").setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                        putExFriend(timeCfg, Date().today)
                        putExFriend(msgCfg, 0)
                        putExFriend(wordsCfg, 0)
                        putExFriend(emoCfg, 0)
                        activity.showToastByTencent("已清空聊天字数统计")
                    }.setNegativeButton(android.R.string.cancel, null).show()
                    true
                }
            }
            (relativeLayout?.parent as FrameLayout).addView(textView)
        }
        Initiator._ChatActivityFacade().method(
            "a",
            6,
            LongArray::class.java
        )?.hookAfter(this)
        {
            val isToday = Date().today == getExFriendCfg()!!.getStringOrDefault(timeCfg, "")
            if (isToday) {
                putExFriend(msgCfg, getExFriendCfg()!!.getIntOrDefault(msgCfg, 0) + 1)
                putExFriend(
                    wordsCfg,
                    getExFriendCfg()!!.getIntOrDefault(wordsCfg, 0) + (it.args[3] as String).length
                )
            } else {
                putExFriend(timeCfg, Date().today)
                putExFriend(msgCfg, 0)
                putExFriend(wordsCfg, 0)
                putExFriend(emoCfg, 0)
            }
        }
        val sendEmoMethod =
            if ("com.tencent.mobileqq.emoticonview.sender.CustomEmotionSenderUtil".clazz != null)
                "com.tencent.mobileqq.emoticonview.sender.CustomEmotionSenderUtil".clazz?.method("sendCustomEmotion", 11, Void.TYPE)
            else Initiator._ChatActivityFacade().method(
                "a", 11, Void.TYPE
            ) {
                it.parameterTypes.contains(Initiator._StickerInfo())
            }
        sendEmoMethod?.hookAfter(
            this
        ) {
            val isToday = Date().today == getExFriendCfg()!!.getStringOrDefault(timeCfg, "")
            if (isToday) {
                putExFriend(emoCfg, getExFriendCfg()!!.getIntOrDefault(emoCfg, 0) + 1)
            } else {
                putExFriend(timeCfg, Date().today)
                putExFriend(msgCfg, 0)
                putExFriend(wordsCfg, 0)
                putExFriend(emoCfg, 0)
            }
        }
    }

    override val isAvailable: Boolean get() = requireMinQQVersion(QQVersion.QQ_8_5_0)

    private fun showChatWordsCountDialog(activity: Context) {
        val dialog = CustomDialog.createFailsafe(activity)
        val ctx = dialog.context
        val editText = EditText(ctx)
        editText.textSize = 16f
        val _5 = LayoutHelper.dip2px(activity, 5f)
        editText.setPadding(_5, _5, _5, _5 * 2)
        editText.setText(
            getExFriendCfg()?.getStringOrDefault(
                strCfg,
                "今日已发送 %1 条消息，共 %2 字，表情包 %3 个"
            ) ?: UIN_CONFIG_ERROR_MESSAGE
        )
        val checkBox = CheckBox(ctx)
        checkBox.text = "开启聊天字数统计"
        checkBox.isChecked = isEnabled
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            isEnabled = isChecked
            when (isChecked) {
                true -> Toasts.showToast(ctx, Toasts.TYPE_INFO, "已开启聊天字数统计", Toasts.LENGTH_SHORT)
                false -> Toasts.showToast(ctx, Toasts.TYPE_INFO, "已关闭聊天字数统计", Toasts.LENGTH_SHORT)
            }
        }
        val textView = TextView(ctx)
        textView.text = "替换侧滑栏个性签名为聊天字数统计，点击可更换字体颜色。\n%1表示发送消息总数，%2表示发送字数，%3表示发送表情包个数。"
        textView.setPadding(_5 * 2, _5, _5 * 2, _5)
        val linearLayout = LinearLayout(ctx)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.addView(
            textView,
            LayoutHelper.newLinearLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                _5,
                0,
                _5,
                0
            )
        )
        linearLayout.addView(
            checkBox,
            LayoutHelper.newLinearLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                _5 * 2
            )
        )
        linearLayout.addView(
            editText,
            LayoutHelper.newLinearLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                _5 * 2
            )
        )
        val alertDialog = dialog.setTitle("输入聊天字数统计样式")
            .setView(linearLayout)
            .setCancelable(true)
            .setPositiveButton("确认") { _, _ ->
            }.setNeutralButton("使用默认值") { _, _ ->
                putExFriend(strCfg, "今日已发送 %1 条消息，共 %2 字，表情包 %3 个")
            }
            .setNegativeButton("取消", null)
            .create() as AlertDialog
        alertDialog.show()
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val text = editText.text.toString()
            if (text == "") {
                Toasts.showToast(
                    activity,
                    Toasts.TYPE_ERROR,
                    "请输入聊天字数统计样式",
                    Toast.LENGTH_SHORT
                )
            } else {
                putExFriend(strCfg, text)
                Toasts.showToast(activity, Toasts.TYPE_INFO, "设置已保存", Toast.LENGTH_SHORT)
                alertDialog.cancel()
            }
        }
    }
}
