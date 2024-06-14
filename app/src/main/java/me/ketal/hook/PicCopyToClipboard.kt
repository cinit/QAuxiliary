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
import android.os.Build
import android.view.View
import cc.hicore.QApp.QAppUtils
import cc.hicore.QQDecodeUtils.DecodeForEncPic
import cc.ioctl.util.Reflex
import cc.ioctl.util.ui.FaultyDialog
import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.findMethodOrNull
import com.github.kyuubiran.ezxhelper.utils.tryOrLogFalse
import com.xiaoniu.dispatcher.OnMenuBuilder
import de.robv.android.xposed.XC_MethodHook
import io.github.qauxv.R
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.CustomMenu
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Initiator._ChatMessage
import io.github.qauxv.util.Initiator._MarketFaceItemBuilder
import io.github.qauxv.util.Initiator._MixedMsgItemBuilder
import io.github.qauxv.util.Initiator._PicItemBuilder
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.dexkit.AbstractQQCustomMenuItem
import io.github.qauxv.util.isAndroidxFileProviderAvailable
import xyz.nextalone.util.SystemServiceUtils.copyToClipboard
import xyz.nextalone.util.clazz
import xyz.nextalone.util.get
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.hookBefore
import xyz.nextalone.util.invoke
import java.io.File
import kotlin.concurrent.thread

@[FunctionHookEntry UiItemAgentEntry Suppress("UNCHECKED_CAST")]
object PicCopyToClipboard : CommonSwitchFunctionHook(
    arrayOf(
        AbstractQQCustomMenuItem
    )
), OnMenuBuilder {
    override val name: String = "复制图片到剪贴板"

    override val description: String = "复制图片到剪贴板，可以在聊天窗口中粘贴使用"

    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY

    override val isAvailable: Boolean = isAndroidxFileProviderAvailable

    override fun initOnce() = tryOrLogFalse {
        if (QAppUtils.isQQnt()) {
            return@tryOrLogFalse
        }
        val clsPicItemBuilder = _PicItemBuilder()
        val clazz = arrayOf(
            clsPicItemBuilder,
            clsPicItemBuilder.superclass,
            _MixedMsgItemBuilder(),
            "com.tencent.mobileqq.activity.aio.item.StructingMsgItemBuilder".clazz,
            _MarketFaceItemBuilder()
        )
        // copy pic
        clazz.filterNotNull().forEach {
            it.findMethod {
                name == "a"
                    && parameterTypes.contentEquals(arrayOf(Int::class.java, Context::class.java, _ChatMessage()))
            }.hookBefore(this) { m ->
                val (id, context, chatMessage) = m.args
                context as Context
                if (id != R.id.item_copyToClipboard) return@hookBefore
                m.result = null
                val path = getPicPath(chatMessage)
                if (path.size > 1) {
                    // todo Let the user select one of the items to copy
                }
                onClick(context, File(path.first()))
            }
            it.findMethodOrNull {
                returnType.isArray
                    && parameterTypes.contentEquals(arrayOf(View::class.java))
            }?.hookAfter(this) { param ->
                val view = param.args[0] as View
                val message = getMessage(view)
                val path = getPicPath(message)
                if (path.isEmpty()) return@hookAfter

                param.result = param.result.run {
                    this as Array<Any>
                    val clQQCustomMenuItem = javaClass.componentType
                    val itemCopy = CustomMenu.createItem(clQQCustomMenuItem, R.id.item_copyToClipboard, "复制图片")
                    plus(itemCopy)
                }
            }
        }
    }

    private fun onClick(context: Context, file: File) {
        if (!file.exists()) {
            Toasts.info(context, "请查看原图后复制")
            return
        }
        try {
            thread {
                copyToClipboard(context, file)
                // on Android 13+, the system will show something like "Copied to clipboard".
                // We only need to show a hint on Android 12 and below.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    SyncUtils.runOnUiThread {
                        Toasts.success(context, "已复制图片")
                    }
                }
            }
        } catch (e: IllegalAccessException) {
            FaultyDialog.show(context, e)
        }
    }

    // find chatMessage from view
    private tailrec fun getMessage(view: View): Any {
        return if (view.parent.javaClass.simpleName.endsWith("ListView")) {
            val viewHolder = view.tag
            viewHolder.get(_ChatMessage())!!
        } else getMessage(view.parent as View)
    }

    private fun getPicPath(message: Any): Array<String> {
        return when (Reflex.getShortClassName(message)) {
            "MessageForPic" -> arrayOf(getFilePath(message))
            "MessageForLongMsg" -> {
                val list = message.get("longMsgFragmentList") as List<Any>
                return list.filter { Reflex.getShortClassName(it) == "MessageForPic" }
                    .map { getFilePath(it) }
                    .toTypedArray()
            }

            "MessageForMixedMsg" -> {
                val list = message.get("msgElemList") as List<Any>
                return list.filter { Reflex.getShortClassName(it) == "MessageForPic" }
                    .map { getFilePath(it) }
                    .toTypedArray()
            }

            "MessageForStructing" -> {
                val text = message.get("structingMsg")?.invoke("getXml") as String
                // todo parse structingmsg
                emptyArray()
            }

            "MessageForMarketFace" -> {
                val tmpPath = DecodeForEncPic.decodeGifForLocalPath(
                    message.get("mMarkFaceMessage").get("dwTabID") as Int,
                    message.get("mMarkFaceMessage").get("sbufID") as ByteArray?
                )
                if (tmpPath.isEmpty()) return emptyArray()
                arrayOf(tmpPath)
            }

            else -> emptyArray()
        }
    }

    private fun getFilePathNt(message: Any): String {
        val msgClass = Initiator.loadClass("com.tencent.mobileqq.aio.msg.AIOMsgItem")
        val clazz = Initiator.load("com.tencent.qqnt.aio.msg.api.impl.AIOMsgItemApiImpl")!!
        return clazz.newInstance().invoke("getLocalPath", message, msgClass) as String
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

    override val targetComponentTypes = arrayOf("com.tencent.mobileqq.aio.msglist.holder.component.pic.AIOPicContentComponent")

    override fun onGetMenuNt(msg: Any, componentType: String, param: XC_MethodHook.MethodHookParam) {
        if (!isEnabled) return
        val list = param.result as MutableList<Any>
        val context = param.thisObject.invoke("getMContext")!!
        val item = CustomMenu.createItemIconNt(msg, "复制图片", R.drawable.ic_item_copy_72dp, R.id.item_copyToClipboard) {
            runCatching {
                val file = File(getFilePathNt(msg))
                onClick(context as Context, file)
            }.onFailure { t ->
                Log.e(t)
            }
        }
        list.add(item)
    }
}
