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
package me.singleneuron.hook

import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.util.xpcompat.XposedBridge
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.step.DexDeobfStep
import io.github.qauxv.step.Step
import io.github.qauxv.util.LicenseStatus
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.dexkit.CAvatarUtil
import io.github.qauxv.util.dexkit.CFaceManager
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.hostInfo
import io.github.qauxv.util.isTim
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object NewRoundHead : CommonSwitchFunctionHook() {

    override val name = "新版简洁模式圆头像"
    override val description = "From 花Q，支持8.3.6及更高，重启后生效"

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY

    override val isAvailable: Boolean get() = !isTim()

    override fun makePreparationSteps(): Array<Step> {
        //特征字符串："FaceManager"/"AvatarUtil"
        return if (requireMinQQVersion(QQVersion.QQ_8_5_0)) {
            arrayOf(DexDeobfStep(CAvatarUtil))
        } else {
            arrayOf(DexDeobfStep(CFaceManager))
        }
    }

    override val isPreparationRequired: Boolean
        get() {
            return if (requireMinQQVersion(QQVersion.QQ_8_5_0)) {
                DexKit.isRunDexDeobfuscationRequired(CAvatarUtil)
            } else {
                DexKit.isRunDexDeobfuscationRequired(CFaceManager)
            }
        }

    override fun initOnce() = throwOrTrue {
        var method = "a"
        if (hostInfo.versionCode == QQVersion.QQ_8_5_0) {
            method = "adjustFaceShape"
        }
        //参数和值都是byte类型
        // com/tencent/mobileqq/avatar/utils/AvatarUtil
        // public static byte adjustFaceShape(byte b);
        //这个方法在QQ主界面初始化时会调用200+次，因此需要极高的性能
        if (requireMinQQVersion(QQVersion.QQ_8_5_0)) {
            for (m in DexKit.requireClassFromCache(CAvatarUtil).declaredMethods) {
                val argt = m.parameterTypes
                if (argt.isNotEmpty() && method == m.name && argt[0] == Byte::class.javaPrimitiveType && m.returnType == Byte::class.javaPrimitiveType) {
                    XposedBridge.hookMethod(m, object : XC_MethodHook() {
                        @Throws(Throwable::class)
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            if (LicenseStatus.sDisableCommonHooks) {
                                return
                            }
                            if (!isEnabled) {
                                return
                            }
                            param.result = param.args[0]
                        }
                    })
                }
            }
        } else {
            for (m in DexKit.requireClassFromCache(CFaceManager).declaredMethods) {
                val argt = m.parameterTypes
                if (argt.isNotEmpty() && method == m.name && argt[0] == Byte::class.javaPrimitiveType && m.returnType == Byte::class.javaPrimitiveType) {
                    XposedBridge.hookMethod(m, object : XC_MethodHook() {
                        @Throws(Throwable::class)
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            if (LicenseStatus.sDisableCommonHooks) {
                                return
                            }
                            if (!isEnabled) {
                                return
                            }
                            param.result = param.args[0]
                        }
                    })
                }
            }
        }
    }
}
