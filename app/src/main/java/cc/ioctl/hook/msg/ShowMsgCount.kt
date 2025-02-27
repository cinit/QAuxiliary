/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2024 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is an opensource software: you can redistribute it
 * and/or modify it under the terms of the General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the General Public License for more details.
 *
 * You should have received a copy of the General Public License
 * along with this software.
 * If not, see
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package cc.ioctl.hook.msg

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import cc.ioctl.util.HookUtils.BeforeAndAfterHookedMethod
import cc.ioctl.util.HookUtils.hookBeforeAndAfterIfEnabled
import cc.ioctl.util.LayoutHelper
import cc.ioctl.util.hookBeforeIfEnabled
import com.github.kyuubiran.ezxhelper.utils.findFieldObjectAs
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.TIMVersion
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.dexkit.AIOTitleVB_updateLeftTopBack_NT
import io.github.qauxv.util.dexkit.CCustomWidgetUtil_updateCustomNoteTxt_NT
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.NCustomWidgetUtil_updateCustomNoteTxt
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.requireMinTimVersion
import io.github.qauxv.util.xpcompat.XC_MethodHook.MethodHookParam
import me.ketal.util.findViewByType
import xyz.nextalone.util.get
import xyz.nextalone.util.throwOrTrue

/**
 * 显示具体消息数量
 * <p>
 * Peak frequency: ~172 invocations per second
 */
@FunctionHookEntry
@UiItemAgentEntry
object ShowMsgCount : CommonSwitchFunctionHook(
    targets = arrayOf(
        CCustomWidgetUtil_updateCustomNoteTxt_NT,
        AIOTitleVB_updateLeftTopBack_NT,
        NCustomWidgetUtil_updateCustomNoteTxt,
    ),
    targetProc = SyncUtils.PROC_ANY
) {

    override val name = "显示具体消息数量"
    override val description = """
        1. 小程序(菜单键)
        2. 隐藏会话(右上角+悬浮消息列表)
        3. 群消息数量(消息列表+左上角返回)
        4. 总消息数量
    """.trimIndent()
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.MESSAGE_CATEGORY

    override fun initOnce() = throwOrTrue {

        if (requireMinQQVersion(QQVersion.QQ_8_9_63_BETA_11345)) {
            // 小程序菜单键
            Initiator.loadClass("com.tencent.qqmini.sdk.core.utils.CustomWidgetUtil")
                .getDeclaredMethod("updateCustomNoteTxt", TextView::class.java, Int::class.java)
                .hookAfter { param ->
                    (param.args[0] as TextView).text = "${param.args[1] as Int}"
                }

            // 隐藏会话(右上角+悬浮消息列表)
            val (floatViewManagerClass, msgUnreadCallbackClass) = when {
                requireMinQQVersion(QQVersion.QQ_9_1_50) -> Pair(// 9.1.50+
                    "com.tencent.mobileqq.activity.miniaio.e",
                    "com.tencent.mobileqq.activity.miniaio.d"
                )

                requireMinQQVersion(QQVersion.QQ_9_0_90) -> Pair(// 9.0.90 ~ 9.1.30
                    "com.tencent.mobileqq.activity.miniaio.i",
                    "com.tencent.mobileqq.activity.miniaio.h"
                )

                requireMinQQVersion(QQVersion.QQ_9_0_60) -> Pair(// 9.0.60 ~ 9.0.70
                    "com.tencent.mobileqq.activity.miniaio.h",
                    "com.tencent.mobileqq.activity.miniaio.g"
                )

                requireMinQQVersion(QQVersion.QQ_9_0_55) -> Pair(// 9.0.55 ~ 9.0.56
                    "com.tencent.mobileqq.activity.miniaio.f",
                    "com.tencent.mobileqq.activity.miniaio.e"
                )

                else -> Pair(// 8.9.70 ~ 9.0.50
                    "com.tencent.mobileqq.activity.miniaio.i",
                    "com.tencent.mobileqq.activity.miniaio.h"
                )
            }
            // 隐藏会话右上角
            Initiator.loadClass(floatViewManagerClass)
                .getDeclaredMethod("updateUnreadCount", Int::class.java, Boolean::class.java)
                .hookAfter { param ->
                    (param.thisObject.findFieldObjectAs<ViewGroup> {
                        type == View::class.java
                    }.findViewByType(TextView::class.java) as TextView).text = "${param.args[0] as Int}"
                }
            // 隐藏会话悬浮消息列表
            Initiator.loadClass(msgUnreadCallbackClass)
                .getDeclaredMethod("updateUnreadCount", Int::class.java, Boolean::class.java)
                .hookAfter { param ->
                    param.thisObject.findFieldObjectAs<TextView> {
                        type == TextView::class.java
                    }.text = "${param.args[0] as Int}"
                }
        }

        if (requireMinQQVersion(QQVersion.QQ_9_0_8) || requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA)) {
            // 群聊消息数量 + 群聊左上角返回
            val clz = Initiator.loadClass("com.tencent.mobileqq.quibadge.QUIBadge")
            val (updateNumName, mNumName, mTextName) = if (requireMinQQVersion(QQVersion.QQ_9_0_15) || requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA)) {
                Triple("updateNum", "mNum", "mText")
            } else {
                Triple("w", "j", "n")
            }
            val updateNum = clz.getDeclaredMethod(updateNumName, Int::class.java)
            val mNum = clz.getDeclaredField(mNumName).apply { isAccessible = true }
            val mText = clz.getDeclaredField(mTextName).apply { isAccessible = true }
            hookBeforeIfEnabled(updateNum) { param ->
                val value = param.args[0] as Int
                mNum.set(param.thisObject, value)
                mText.set(param.thisObject, value.toString())
                param.result = null
            }
        } else {
            if (requireMinQQVersion(QQVersion.QQ_8_9_63_BETA_11345)) {
                // 群聊消息数量
                val clz = DexKit.requireClassFromCache(CCustomWidgetUtil_updateCustomNoteTxt_NT)
                val updateNum = clz.declaredMethods.single { method ->
                    val params = method.parameterTypes
                    params.size == 6 && params[0] == TextView::class.java
                        && params[1] == Int::class.java && params[2] == Int::class.java
                        && params[3] == Int::class.java && params[4] == Int::class.java
                        && params[5] == String::class.java
                }
                hookBeforeAndAfterIfEnabled(this, updateNum, 50, object : BeforeAndAfterHookedMethod {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.args[4] = Int.MAX_VALUE
                    }

                    override fun afterHookedMethod(param: MethodHookParam) {
                        val tv = param.args[0] as TextView
                        val count = param.args[2] as Int
                        val str = count.toString()
                        val lp = tv.layoutParams
                        lp.width = LayoutHelper.dip2px(tv.context, (9 + 7 * str.length).toFloat())
                        tv.layoutParams = lp
                    }
                })
                // 群聊左上角返回
                DexKit.requireMethodFromCache(AIOTitleVB_updateLeftTopBack_NT).hookAfter {
                    if (it.args[0] is Int) {
                        val count = it.args[0] as Int
                        if (count > 0) {
                            val (mTitleBinding, unreadTv) = when {
                                requireMinQQVersion(QQVersion.QQ_9_0_0) -> Pair("e", "v")
                                requireMinQQVersion(QQVersion.QQ_8_9_80) -> Pair("e", "s")
                                requireMinQQVersion(QQVersion.QQ_8_9_70) -> Pair("e", "t")
                                requireMinQQVersion(QQVersion.QQ_8_9_63_BETA_11345) -> Pair("e", "s")
                                else -> Pair("", "")
                            }
                            if (mTitleBinding.isNotEmpty() && unreadTv.isNotEmpty()) {
                                (it.thisObject.get(mTitleBinding).get(unreadTv) as TextView).text = "$count"
                            }
                        }
                    }
                }
            }
        }

        if (requireMinQQVersion(QQVersion.QQ_9_0_8) || requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA)) {
            // 总消息数量
            val clz = DexKit.requireClassFromCache(NCustomWidgetUtil_updateCustomNoteTxt)
            val method = clz.declaredMethods.single { method ->
                val params = method.parameterTypes
                params.size == 5 && params[0] == Initiator.loadClass("com.tencent.mobileqq.quibadge.QUIBadge")
                    && params[1] == Int::class.java && params[2] == Int::class.java && params[3] == Int::class.java
                    && params[4] == String::class.java
            }
            hookBeforeIfEnabled(method) { param ->
                param.args[3] = Int.MAX_VALUE
            }
        } else {
            // 总消息数量(QQ[9.0.8]之前) + 群消息数量(QQNT[8.9.63]之前)
            val clz = DexKit.requireClassFromCache(NCustomWidgetUtil_updateCustomNoteTxt)
            val method = clz.declaredMethods.single { method ->
                val params = method.parameterTypes
                if (requireMinQQVersion(QQVersion.QQ_9_0_0)) {
                    params.size == 7 && params[0] == TextView::class.java
                        && params[1] == Int::class.java && params[2] == Int::class.java
                        && params[3] == Int::class.java && params[4] == Int::class.java
                        && params[5] == String::class.java && params[6] == Boolean::class.java
                } else {
                    params.size == 6 && params[0] == TextView::class.java
                        && params[1] == Int::class.java && params[2] == Int::class.java
                        && params[3] == Int::class.java && params[4] == Int::class.java
                        && params[5] == String::class.java
                }
            }
            hookBeforeAndAfterIfEnabled(this, method, 50, object : BeforeAndAfterHookedMethod {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.args[4] = Int.MAX_VALUE
                }

                override fun afterHookedMethod(param: MethodHookParam) {
                    (param.args[0] as TextView).apply {
                        maxWidth = Int.MAX_VALUE
                        layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                        setPadding(0, 0, 0, 0)
                    }
                }
            })
        }

    }
}
