/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the qwq233 Universal License
 * as published on https://github.com/qwq233/license; either
 * version 2 of the License, or any later version and our EULA as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the qwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package me.qcuncle.hook

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.widget.TextView
import cc.hicore.QApp.QAppUtils
import cc.ioctl.util.Reflex
import cc.ioctl.util.afterHookIfEnabled
import com.xiaoniu.dispatcher.OnMenuBuilder
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedBridge
import io.github.qauxv.util.xpcompat.XposedHelpers
import io.github.qauxv.R
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.bridge.AppRuntimeHelper
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.CustomMenu
import io.github.qauxv.util.Initiator
import xyz.nextalone.util.clazz
import xyz.nextalone.util.invoke

@FunctionHookEntry
@UiItemAgentEntry
object TranslateTextMsg : CommonSwitchFunctionHook(), OnMenuBuilder {
    override val name: String = "翻译文本消息"

    override val description: String = "在聊天窗口中，长按一个文本消息出现翻译按钮，点击翻译"

    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY

    private var isHook: Boolean = false

    override fun initOnce(): Boolean {
        if (QAppUtils.isQQnt()) return true

        val _TextItemBuilder = Initiator._TextItemBuilder()
        XposedHelpers.findAndHookMethod(
            _TextItemBuilder, "a", Int::class.javaPrimitiveType, Context::class.java,
            Initiator._ChatMessage(), menuItemClickCallback
        )
        for (m in _TextItemBuilder!!.declaredMethods) {
            if (!m.returnType.isArray) {
                continue
            }
            val ps = m.parameterTypes
            if (ps.size == 1 && ps[0] == View::class.java) {
                XposedBridge.hookMethod(m, getMenuItemCallBack)
                break
            }
        }
        return true
    }

    private val getMenuItemCallBack = afterHookIfEnabled(60) { param ->
        try {
            val original: Array<Any> = param.result as Array<Any>
            val originLength = original.size
            val itemClass = original.javaClass.componentType!!
            val ret: Array<Any?> = java.lang.reflect.Array.newInstance(itemClass, originLength + 1) as Array<Any?>
            System.arraycopy(original, 0, ret, 0, originLength)
            ret[originLength] = CustomMenu.createItem(itemClass, R.id.item_translate, "翻译")
            CustomMenu.checkArrayElementNonNull(ret)
            param.result = ret
        } catch (e: Throwable) {
            traceError(e)
            throw e
        }
    }

    private val menuItemClickCallback = afterHookIfEnabled(60) { param ->
        val id = param.args[0] as Int
        val ctx = param.args[1] as Activity
        val chatMessage = param.args[2]
        val wc = CommonContextWrapper.createAppCompatContext(ctx)
        when (id) {
            R.id.item_translate -> {
                translateMessageContent(Reflex.getInstanceObjectOrNull(chatMessage, "msg")?.toString() ?: "")
            }
        }

        if (!isHook) {
            isHook = true
            val _TranslateResult = "com.tencent.mobileqq.ocr.data.TranslateResult".clazz!!
            val _C88380b = "com.tencent.mobileqq.ocr.b".clazz!!
            XposedHelpers.findAndHookMethod(_C88380b, "c", Boolean::class.java, Int::class.java, _TranslateResult,
                afterHookIfEnabled {
                    val translateResult = it.args[2]
                    val dstContent = XposedHelpers.findMethodExactIfExists(_TranslateResult, "f")
                        .invoke(translateResult)?.toString() ?: "翻译失败"
                    AlertDialog.Builder(wc)
                        .setMessage(dstContent)
                        .show()
                        .findViewById<TextView>(android.R.id.message)
                        .setTextIsSelectable(true)
                })
        }
    }

    private fun translateMessageContent(content: String) {
        val runtime = AppRuntimeHelper.getAppRuntime()
        val ocrHandler = runtime?.invoke("getRuntimeService", "com.tencent.mobileqq.ocr.api.IOCRHandler".clazz!!, Class::class.java)
        val src = if (m245294i3(content)) "zh" else "en"
        val dst = if (m245294i3(content)) "en" else "zh"
        XposedHelpers.findMethodExactIfExists(
            "com.tencent.mobileqq.ocr.api.IOCRHandler".clazz!!, "batchTranslate", String::class.java, String::class.java, String::class.java
        ).invoke(ocrHandler, content, src, dst)
    }

    private fun m245294i3(str: String): Boolean {
        for (element in str) {
            if (element.code in 19968..40868) {
                return true
            }
        }
        return false
    }

    override val targetComponentTypes: Array<String>
        get() = arrayOf("com.tencent.mobileqq.aio.msglist.holder.component.text.AIOTextContentComponent")

    override fun onGetMenuNt(msg: Any, componentType: String, param: XC_MethodHook.MethodHookParam) {
        if (!isEnabled) return
        val item = CustomMenu.createItemIconNt(msg, "翻译文本", R.drawable.ic_item_translate_72dp, R.id.item_translate) {
            //TODO: 待开发
        }
        param.result = (param.result as List<*>) + item
    }
}