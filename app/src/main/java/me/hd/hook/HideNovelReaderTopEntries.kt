/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2026 QAuxiliary developers
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

package me.hd.hook

import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.dsl.uiSwitchPreference
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import me.hd.base.KuiklyDelayableHook
import me.hd.util.hookBeforeIfEnabled
import me.hd.util.parameters
import me.hd.util.singleConstructor
import org.luckypray.dexkit.DexKitBridge
import java.lang.reflect.Method
import java.lang.reflect.Modifier

@FunctionHookEntry
@UiItemAgentEntry
object HideNovelReaderTopEntries : KuiklyDelayableHook() {
    override val kuiklyName: String
        get() = "vas_qqvip_novel_reader.apk"

    override val uiItemAgent = uiSwitchPreference {
        title = "隐藏小说阅读页右上角会员/金币入口"
    }

    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.UI_MISC
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_9_0_0)

    override fun startHook(classLoader: ClassLoader): Boolean {
        DexKitBridge.create(classLoader, true).use { bridge ->
            bridge.findMethod {
                matcher {
                    modifiers = Modifier.STATIC
                    usingNumbers(30.0f, 100.0f, 92.0f, 85.0f)
                }
            }.single().getMethodInstance(classLoader)
                .hookBeforeIfEnabled(this) { param ->
                    val pairType = (param.method as Method).returnType
                    param.result = pairType.singleConstructor {
                        parameters(Any::class.java, Any::class.java)
                    }.newInstance(false, false)
                }
        }
        return true
    }
}
