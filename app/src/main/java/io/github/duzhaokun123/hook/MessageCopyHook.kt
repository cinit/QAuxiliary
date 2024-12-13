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
import android.util.Log
import android.view.View
import android.widget.TextView
import cc.hicore.QApp.QAppUtils
import cc.ioctl.util.Reflex
import cc.ioctl.util.afterHookIfEnabled
import com.xiaoniu.dispatcher.OnMenuBuilder
import com.xiaoniu.util.ContextUtils
import io.github.qauxv.R
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.step.Step
import io.github.qauxv.ui.CommonContextWrapper
import io.github.qauxv.util.CustomMenu
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.TIMVersion
import io.github.qauxv.util.dexkit.DexDeobfsProvider
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.DexKitFinder
import io.github.qauxv.util.dexkit.TextMsgItem_getText
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.requireMinTimVersion
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedBridge
import io.github.qauxv.util.xpcompat.XposedHelpers
import java.lang.reflect.Modifier

@FunctionHookEntry
@UiItemAgentEntry
object MessageCopyHook : CommonSwitchFunctionHook(), DexKitFinder, OnMenuBuilder {
    const val TAG = "MessageCopyHook"
    override val name: String
        get() = "文本消息自由复制"

    override fun initOnce(): Boolean {
        if (QAppUtils.isQQnt()) {
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
                showDialog(wc, Reflex.getInstanceObjectOrNull(chatMessage, "msg")?.toString() ?: "获取消息失败")
            }
        }
    }

    override val uiItemLocation: Array<String>
        get() = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY

    fun showDialog(context: Context, text: CharSequence) {
        AlertDialog.Builder(context)
            .setMessage(text)
            .setNegativeButton(android.R.string.cancel, null)
            .show()
            .findViewById<TextView>(android.R.id.message)
            .setTextIsSelectable(true)
    }

    override fun makePreparationSteps(): Array<Step> {
        return arrayOf(object : Step {
            override fun step(): Boolean {
                return doFind()
            }

            override fun isDone(): Boolean {
                return !isNeedFind
            }

            override fun getPriority(): Int {
                return 0
            }

            override fun getDescription(): String {
                return "文本消息自由复制相关类查找中"
            }
        })
    }

    override val isNeedFind: Boolean
        get() = TextMsgItem_getText.descCache == null && (requireMinQQVersion(QQVersion.QQ_8_9_63_BETA_11345) || requireMinTimVersion(TIMVersion.TIM_4_0_95))

    override fun doFind(): Boolean {
        DexDeobfsProvider.getCurrentBackend().use { backend ->
            val dexKit = backend.getDexKitBridge()
            Log.d(TAG, "doFind: doFind")
            val getText = dexKit.findMethod {
                searchPackages("com.tencent.mobileqq.aio.msg")
                matcher {
                    modifiers = Modifier.PRIVATE
                    returnType = "java.lang.CharSequence"
                    paramCount = 0
                    usingNumbers(24)
                    usingStrings("biz_src_jc_aio")
//                    addCall {
//                        name = "getQQText"
//                    }
                }
            }.firstOrNull() ?: return false
            Log.d(TAG, "doFind: $getText")
            TextMsgItem_getText.descCache = getText.descriptor
        }
        return true
    }

    override val targetComponentTypes = arrayOf("com.tencent.mobileqq.aio.msglist.holder.component.text.AIOTextContentComponent")

    override fun onGetMenuNt(msg: Any, componentType: String, param: XC_MethodHook.MethodHookParam) {
        if (!isEnabled) return
        // TODO: support ark message
        val item = CustomMenu.createItemIconNt(msg, "自由复制", R.drawable.ic_item_copy_72dp, R.id.item_free_copy) {
            val text = try {
                DexKit.requireMethodFromCache(TextMsgItem_getText).also {
                    it.isAccessible = true
                }.invoke(msg) as CharSequence
            } catch (e: Exception) {
                "${e.javaClass.name}: ${e.message}\n" + (e.stackTrace.joinToString("\n"))
            }
            showDialog(CommonContextWrapper.createAppCompatContext(ContextUtils.getCurrentActivity()), text)
        }
        param.result = (param.result as List<*>) + item
    }
}
