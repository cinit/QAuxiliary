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
package cc.ioctl.hook.msg

import android.app.Activity
import android.content.Context
import android.view.View
import cc.hicore.QApp.QAppUtils
import cc.ioctl.util.Reflex
import cc.ioctl.util.afterHookIfEnabled
import cc.ioctl.util.beforeHookIfEnabled
import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import com.xiaoniu.dispatcher.OnMenuBuilder
import com.xiaoniu.util.ContextUtils
import io.github.qauxv.util.xpcompat.XC_MethodHook.MethodHookParam
import io.github.qauxv.util.xpcompat.XposedBridge
import io.github.qauxv.util.xpcompat.XposedHelpers
import io.github.qauxv.R
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.CustomMenu
import io.github.qauxv.util.CustomMenu.createItemIconNt
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.dexkit.AbstractQQCustomMenuItem
import io.github.qauxv.util.dexkit.CArkAppItemBubbleBuilder
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.util.SystemServiceUtils.copyToClipboard
import xyz.nextalone.util.throwOrTrue
import java.lang.reflect.Array

@FunctionHookEntry
@UiItemAgentEntry
object CopyCardMsg : CommonSwitchFunctionHook("CopyCardMsg::BaseChatPie", arrayOf(CArkAppItemBubbleBuilder, AbstractQQCustomMenuItem)), OnMenuBuilder {

    override val name = "复制卡片消息"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY

    override fun initOnce() = throwOrTrue {
        if (QAppUtils.isQQnt()) {
            return@throwOrTrue
        }

        //Begin: ArkApp
        val cl_ArkAppItemBuilder = DexKit.loadClassFromCache(CArkAppItemBubbleBuilder)
        XposedHelpers.findAndHookMethod(
            cl_ArkAppItemBuilder, "a", Int::class.javaPrimitiveType, Context::class.java,
            Initiator.load("com/tencent/mobileqq/data/ChatMessage"), menuItemClickCallback
        )
        for (m in cl_ArkAppItemBuilder!!.declaredMethods) {
            if (!m.returnType.isArray) {
                continue
            }
            val ps = m.parameterTypes
            if (ps.size == 1 && ps[0] == View::class.java) {
                XposedBridge.hookMethod(m, getMenuItemCallBack)
                break
            }
        }
        //End: ArkApp
        //Begin: StructMsg
        val cl_StructingMsgItemBuilder = Initiator.loadClass(
            "com/tencent/mobileqq/activity/aio/item/StructingMsgItemBuilder"
        )
        XposedHelpers.findAndHookMethod(
            cl_StructingMsgItemBuilder, "a", Int::class.javaPrimitiveType, Context::class.java,
            Initiator.load("com/tencent/mobileqq/data/ChatMessage"), menuItemClickCallback
        )
        for (m in cl_StructingMsgItemBuilder.declaredMethods) {
            if (!m.returnType.isArray) {
                continue
            }
            val ps = m.parameterTypes
            if (ps.size == 1 && ps[0] == View::class.java) {
                XposedBridge.hookMethod(m, getMenuItemCallBack)
                break
            }
        }
        //End: StructMsg
//        for (m in Initiator.load("com.tencent.mobileqq.structmsg.StructMsgForGeneralShare").methods) {
//            if (m.name == "getView") {
//                XposedBridge.hookMethod(m, object : XC_MethodHook() {
//                    @Throws(Throwable::class)
//                    override fun afterHookedMethod(param: MethodHookParam) {
//                        val v = param.result as View
//                        val l: OnLongClickListener = getBubbleLongClickListener(param.args[0] as Activity)
//                        if (v != null && l != null) {
//                            //v.setOnLongClickListener(l);
//                        }
//                    }
//                })
//                break
//            }
//        }
    }

    private val getMenuItemCallBack = afterHookIfEnabled(60) { param ->
        val arr = param.result
        val clQQCustomMenuItem = arr.javaClass.componentType
        val item_copy = CustomMenu.createItem(clQQCustomMenuItem, R.id.item_copy_code, "复制代码")
        val ret = Array.newInstance(clQQCustomMenuItem, Array.getLength(arr) + 1)
        Array.set(ret, 0, Array.get(arr, 0))
        System.arraycopy(arr, 1, ret, 2, Array.getLength(arr) - 1)
        Array.set(ret, 1, item_copy)
        param.result = ret
    }

    private val menuItemClickCallback = beforeHookIfEnabled(60) { param ->
        val id = param.args[0] as Int
        val ctx = param.args[1] as Activity
        val chatMessage = param.args[2]
        if (id == R.id.item_copy_code) {
            param.result = null
            if (Initiator.loadClass("com.tencent.mobileqq.data.MessageForStructing")
                    .isAssignableFrom(chatMessage.javaClass)
            ) {
                val text = Reflex.invokeVirtual(
                    Reflex.getInstanceObjectOrNull(chatMessage, "structingMsg"), "getXml"
                ) as String
                copyToClipboard(ctx, text)
                Toasts.info(ctx, "复制成功")
            } else if (Initiator.loadClass("com.tencent.mobileqq.data.MessageForArkApp")
                    .isAssignableFrom(chatMessage.javaClass)
            ) {
                val text = Reflex.invokeVirtual(
                    Reflex.getInstanceObjectOrNull(chatMessage, "ark_app_message"), "toAppXml"
                ) as String
                copyToClipboard(ctx, text)
                Toasts.info(ctx, "复制成功")
            }
        }
    }
    override val targetComponentTypes = arrayOf(
        if (requireMinQQVersion(QQVersion.QQ_9_1_55)) {
            "com.tencent.mobileqq.aio.msglist.holder.component.template.AIOTemplateMsgComponent"
        } else {
            "com.tencent.mobileqq.aio.msglist.holder.component.ark.AIOArkContentComponent"
        },
    )

    override fun onGetMenuNt(msg: Any, componentType: String, param: MethodHookParam) {
        if (!isEnabled) return
        val ctx = ContextUtils.getCurrentActivity()
        val item = createItemIconNt(msg, "复制代码", R.drawable.ic_item_copy_72dp, R.id.item_copy_code) {
            val element = (msg.javaClass.declaredMethods.first {
                it.returnType == MsgElement::class.java && it.parameterTypes.isEmpty()
            }.apply { isAccessible = true }.invoke(msg) as MsgElement).arkElement
            copyToClipboard(ctx, element.bytesData)
            Toasts.info(ctx, "复制成功")
        }
        val list = param.result as MutableList<Any>
        list.add(item)
    }
}
