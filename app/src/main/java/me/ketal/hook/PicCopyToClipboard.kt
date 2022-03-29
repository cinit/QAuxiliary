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

import android.content.Context
import android.view.View
import cc.ioctl.util.Reflex
import com.github.kyuubiran.ezxhelper.utils.tryOrFalse
import io.github.qauxv.R
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.CustomMenu
import io.github.qauxv.util.Initiator._ChatMessage
import io.github.qauxv.util.Initiator._PicItemBuilder
import xyz.nextalone.util.SystemServiceUtils.copyToClipboard
import xyz.nextalone.util.clazz
import xyz.nextalone.util.get
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.hookBefore
import xyz.nextalone.util.invoke
import xyz.nextalone.util.method
import java.io.File

@[FunctionHookEntry UiItemAgentEntry Suppress("UNCHECKED_CAST")]
object PicCopyToClipboard : CommonSwitchFunctionHook() {
    override val name: String = "复制图片到剪贴板"

    override val description: String = "复制图片到剪贴板，可以在聊天窗口中粘贴使用"

    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY

    override fun initOnce() = tryOrFalse {
        val clsPicItemBuilder = _PicItemBuilder()
        val clsMixedMsgItemBuilder = "com.tencent.mobileqq.activity.aio.item.MixedMsgItemBuilder".clazz
        val clsStructingMsgItemBuilder = "com.tencent.mobileqq.activity.aio.item.StructingMsgItemBuilder".clazz
        val clazz = arrayOf(clsPicItemBuilder, clsPicItemBuilder.superclass, clsMixedMsgItemBuilder, clsStructingMsgItemBuilder)
        // copy pic
        clazz.filterNotNull().forEach {
            it.method { m ->
                m.name == "a"
                    && m.parameterTypes.contentEquals(arrayOf(Int::class.java, Context::class.java, _ChatMessage()))
            }?.hookBefore(this) { m ->
                val id = m.args[0]
                val context = m.args[1] as Context
                val message = m.args[2]
                if (id != R.id.item_copyToClipboard) return@hookBefore
                m.result = null
                copyToClipboard(context, File(getPicPath(message)))
            }
            it.method { m ->
                m.returnType.isArray
                    && m.parameterTypes.contentEquals(arrayOf(View::class.java))
            }?.hookAfter(this) { param ->
                val view = param.args[0] as View
                val message = getMessage(view)
                if (!hasPic(message)) return@hookAfter
                param.result = param.result.run {
                    this as Array<Any>
                    val clQQCustomMenuItem = javaClass.componentType
                    val itemCopy = CustomMenu.createItem(clQQCustomMenuItem, R.id.item_copyToClipboard, "复制图片")
                    plus(itemCopy)
                }
            }
        }
    }

    // find chatMessage from view
    private tailrec fun getMessage(view: View): Any {
        return if (view.parent.javaClass.simpleName.endsWith("ListView")) {
            val viewHolder = view.tag
            viewHolder.get(_ChatMessage())!!
        } else getMessage(view.parent as View)
    }

    private fun hasPic(message: Any): Boolean {
        return when (Reflex.getShortClassName(message)) {
            "MessageForPic" -> true
            "MessageForLongMsg" -> {
                val list = message.get("longMsgFragmentList") as List<Any>
                list.any { hasPic(it) }
            }
            "MessageForMixedMsg" -> {
                val list = message.get("msgElemList") as List<Any>
                list.any { hasPic(it) }
            }
            "MessageForStructing" -> {
                val text = message.get("structingMsg").invoke("getXml") as String
                // Log.d("structingMsg: $text")
                // todo parse structingmsg
                false
            }
            else -> false
        }
    }

    private fun getPicPath(message: Any): String {
        return when (Reflex.getShortClassName(message)) {
            "MessageForPic" -> getFilePath(message)
            "MessageForLongMsg" -> {
                val list = message.get("longMsgFragmentList") as List<Any>
                // todo Alert users when multiple images are included
                val msg = list.first { Reflex.getShortClassName(it) == "MessageForPic" }
                getFilePath(msg)
            }
            "MessageForMixedMsg" -> {
                val list = message.get("msgElemList") as List<Any>
                // todo Alert users when multiple images are included
                val msg = list.first { Reflex.getShortClassName(it) == "MessageForPic" }
                getFilePath(msg)
            }
            "MessageForStructing" -> {
                val text = message.get("structingMsg").invoke("getXml") as String
                // todo parse structingmsg
                ""
            }
            else -> throw UnsupportedOperationException("get pic from ${Reflex.getShortClassName(message)}")
        }
    }

    private fun getFilePath(message: Any): String {
        val path = arrayOf("chatraw", "chatimg", "chatthumb").map { str ->
            message.invoke("getFilePath", str, String::class.java) as String
        }.first { path ->
            // chosen the first exist file aka the biggest one
            File(path).exists()
        }
        return path
    }
}
