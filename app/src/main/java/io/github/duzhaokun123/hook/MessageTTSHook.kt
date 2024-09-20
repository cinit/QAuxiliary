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
import android.content.Context
import android.speech.tts.TextToSpeech
import android.view.View
import cc.hicore.QApp.QAppUtils
import cc.ioctl.util.HostInfo
import cc.ioctl.util.Reflex
import cc.ioctl.util.afterHookIfEnabled
import com.xiaoniu.dispatcher.OnMenuBuilder
import com.xiaoniu.util.ContextUtils
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedBridge
import io.github.qauxv.util.xpcompat.XposedHelpers
import io.github.duzhaokun123.hook.MessageCopyHook.TAG
import io.github.duzhaokun123.util.TTS
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
import io.github.qauxv.util.Toasts
import io.github.qauxv.util.dexkit.DexDeobfsProvider
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.DexKitFinder
import io.github.qauxv.util.dexkit.TextMsgItem_getText
import java.lang.reflect.Modifier

@FunctionHookEntry
@UiItemAgentEntry
object MessageTTSHook : CommonSwitchFunctionHook(), OnMenuBuilder, DexKitFinder {
    override val name: String
        get() = "文字消息转语音 (使用系统 TTS)"

    override val description: String
        get() = "提示失败多半是没设置系统 TTS 引擎"

    override fun initOnce(): Boolean {
        if (QAppUtils.isQQnt()) return true

        val cl_ArkAppItemBuilder = Initiator._TextItemBuilder()
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
        TTS.addInitCallback {
            if (it == TextToSpeech.ERROR) {
                Toasts.error(HostInfo.getApplication(), "TTS 初始化失败")
                traceError(RuntimeException("TTS init failed"))
            }
        }
        TTS.init(HostInfo.getApplication())
        return true
    }

    override val uiItemLocation: Array<String>
        get() = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY

    private val getMenuItemCallBack = afterHookIfEnabled(60) { param ->
        try {
            val original: Array<Any> = param.result as Array<Any>
            val originLength = original.size
            val itemClass = original.javaClass.componentType!!
            val ret: Array<Any?> = java.lang.reflect.Array.newInstance(itemClass, originLength + 2) as Array<Any?>
            System.arraycopy(original, 0, ret, 0, originLength)
            ret[originLength] = CustomMenu.createItem(itemClass, R.id.item_tts, "TTS")
            ret[originLength + 1] = CustomMenu.createItem(itemClass, R.id.item_tts2, "TTS+")
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
            R.id.item_tts -> {
                TTS.speak(wc, Reflex.getInstanceObjectOrNull(chatMessage, "msg")?.toString() ?: "")
            }

            R.id.item_tts2 -> {
                val msg = Reflex.getInstanceObjectOrNull(chatMessage, "msg")?.toString() ?: ""
                TTS.showConfigDialog(wc, msg)
            }
        }
    }
    override val targetComponentTypes: Array<String>
        get() = arrayOf("com.tencent.mobileqq.aio.msglist.holder.component.text.AIOTextContentComponent")

    override fun onGetMenuNt(msg: Any, componentType: String, param: XC_MethodHook.MethodHookParam) {
        if (!isEnabled) return
        val item = CustomMenu.createItemIconNt(msg, "TTS", R.drawable.ic_item_tts_72dp, R.id.item_tts) {
            val ctx = ContextUtils.getCurrentActivity()
            val wc = CommonContextWrapper.createAppCompatContext(ctx)
            val text = try {
                DexKit.requireMethodFromCache(TextMsgItem_getText).also {
                    it.isAccessible = true
                }.invoke(msg) as CharSequence
            } catch (e: Exception) {
                "${e.javaClass.name}: ${e.message}\n" + (e.stackTrace.joinToString("\n"))
            }
            TTS.speak(wc, text.toString())
        }
        val item2 = CustomMenu.createItemIconNt(msg, "TTS+", R.drawable.ic_item_tts_plus_72dp, R.id.item_tts2) {
            val ctx = ContextUtils.getCurrentActivity()
            val wc = CommonContextWrapper.createAppCompatContext(ctx)
            val text = try {
                DexKit.requireMethodFromCache(TextMsgItem_getText).also {
                    it.isAccessible = true
                }.invoke(msg) as CharSequence
            } catch (e: Exception) {
                "${e.javaClass.name}: ${e.message}\n" + (e.stackTrace.joinToString("\n"))
            }
            TTS.showConfigDialog(wc, text.toString())
        }
        param.result = (param.result as List<*>) + item + item2
    }


    /**
     * see MessageCopyHook
     */
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
                return "文本消息相关方法查找中"
            }
        })
    }

    override val isNeedFind: Boolean
        get() = HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_63_BETA_11345) && TextMsgItem_getText.descCache == null

    override fun doFind(): Boolean {
        DexDeobfsProvider.getCurrentBackend().use { backend ->
            val dexKit = backend.getDexKitBridge()
            android.util.Log.d(TAG, "doFind: doFind")
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
            android.util.Log.d(TAG, "doFind: $getText")
            TextMsgItem_getText.descCache = getText.descriptor
        }
        return true
    }

}
