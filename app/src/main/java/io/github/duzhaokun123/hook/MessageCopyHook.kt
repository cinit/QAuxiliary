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

package io.github.duzhaokun123.hook

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.widget.TextView
import cc.ioctl.util.HostInfo
import cc.ioctl.util.Reflex
import cc.ioctl.util.afterHookIfEnabled
import cc.ioctl.util.hookAfterIfEnabled
import com.github.kyuubiran.ezxhelper.utils.invokeMethodAutoAs
import com.github.kyuubiran.ezxhelper.utils.paramCount
import com.xiaoniu.util.ContextUtils
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.qauxv.R
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.CustomMenu
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Log
import io.github.qauxv.util.QQVersion
import xyz.nextalone.util.method

@FunctionHookEntry
@UiItemAgentEntry
object MessageCopyHook : CommonSwitchFunctionHook() {
    override val name: String
        get() = "文本消息自由复制"

    override fun initOnce(): Boolean {
        if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_63)) { // TODO: support ark message
            val class_AIOMsgItem = Initiator.loadClass("com.tencent.mobileqq.aio.msg.AIOMsgItem")
            val class_BaseContentComponent = Initiator.loadClass("com.tencent.mobileqq.aio.msglist.holder.component.BaseContentComponent")
            val method_getMsg = class_BaseContentComponent.method { it.returnType == class_AIOMsgItem && it.paramCount == 0 }!!
            method_getMsg.isAccessible = true
            val class_AIOTextContentComponent = Initiator.loadClass("com.tencent.mobileqq.aio.msglist.holder.component.text.AIOTextContentComponent")
            val method_list = class_AIOTextContentComponent.method { it.returnType == List::class.java && it.paramCount == 0 }!!
            method_list.isAccessible = true
            hookAfterIfEnabled(method_list) { param ->
                val msg = method_getMsg.invoke(param.thisObject)
                val item = CustomMenu.createItemNt(msg, "自由复制", R.id.item_free_copy) {
                    Log.d(msg.javaClass.name)
//                    val method_getElement = msg.javaClass.method { it.returnType == TextElement::class.java }!!
                    AlertDialog.Builder(CommonContextWrapper.createAppCompatContext(ContextUtils.getCurrentActivity()))
                        .setMessage(msg.invokeMethodAutoAs("y1"))
                        .show()
                        .findViewById<TextView>(android.R.id.message)
                        .setTextIsSelectable(true)
                }
                param.result = (param.result as List<*>) + item
            }
            return true
        }

        val class_ArkAppItemBuilder = Initiator._TextItemBuilder()
        XposedHelpers.findAndHookMethod(
            class_ArkAppItemBuilder, "a", Int::class.javaPrimitiveType, Context::class.java,
            Initiator.load("com/tencent/mobileqq/data/ChatMessage"), menuItemClickCallback
        )
        for (m in class_ArkAppItemBuilder!!.declaredMethods) {
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
            ret[originLength] = CustomMenu.createItem(itemClass, R.id.item_free_copy, "自由复制")
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
            R.id.item_free_copy -> {
                AlertDialog.Builder(wc)
                    .setMessage(Reflex.getInstanceObjectOrNull(chatMessage, "msg")?.toString() ?: "")
                    .show()
                    .findViewById<TextView>(android.R.id.message)
                    .setTextIsSelectable(true)
            }
        }
    }

    override val uiItemLocation: Array<String>
        get() = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY
}
