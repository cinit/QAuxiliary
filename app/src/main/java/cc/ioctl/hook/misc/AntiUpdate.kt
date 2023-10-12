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

package cc.ioctl.hook.misc

import android.os.Message
import cc.ioctl.util.Reflex
import cc.ioctl.util.hookBeforeIfEnabled
import com.github.kyuubiran.ezxhelper.utils.emptyParam
import com.github.kyuubiran.ezxhelper.utils.findAllMethods
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import java.lang.reflect.Method

// instead HookUpgrade
object AntiUpdate : CommonSwitchFunctionHook() {
    override val name: String = "屏蔽更新"

    override fun initOnce(): Boolean {
        val kUpgradeController = Initiator._UpgradeController()
            ?: throw ClassNotFoundException("UpgradeController")
        val kUpgradeDetailWrapper = Initiator.load("com.tencent.mobileqq.upgrade.UpgradeDetailWrapper")
            ?: Initiator.loadClass("com.tencent.mobileqq.app.upgrade.UpgradeDetailWrapper")
        // dialog
        val kConfigHandler = Initiator._ConfigHandler()
        val candidates = ArrayList<Method>(4)
        for (m in kConfigHandler.declaredMethods) {
            if (m.returnType == Void.TYPE && m.parameterTypes.size == 1 && m.parameterTypes[0] == kUpgradeDetailWrapper) {
                candidates.add(m)
            }
        }
        if (candidates.isEmpty() || candidates.size > 3) {
            throw IllegalStateException("ConfigHandler.showUpgradeIfNecessary candidates size is ${candidates.size}")
        }
        candidates.forEach { hookBeforeIfEnabled(it) { param -> param.result = null } }
        // yellow bar
        val kUpgradeBannerProcessor = Initiator.load("com.tencent.mobileqq.activity.recent.bannerprocessor.UpgradeBannerProcessor")
        if (kUpgradeBannerProcessor != null) {
            val method = Reflex.findSingleMethod(kUpgradeBannerProcessor, Void.TYPE, false, Message::class.java, Long::class.java, Boolean::class.java)
            hookBeforeIfEnabled(method) { param -> param.result = null }
        } else {
            // older versions
            kUpgradeController.findAllMethods {
                emptyParam && returnType == kUpgradeDetailWrapper && name.length == 1
            }.hookBefore { if (isEnabled) it.result = null }
        }
        for (m in kUpgradeController.getDeclaredMethods()) {
            if (m.returnType == Void.TYPE) {
                hookBeforeIfEnabled(m) { p -> p.setResult(null) }
            } else if (m.returnType == Boolean::class.javaPrimitiveType) {
                hookBeforeIfEnabled(m) { p -> p.result = false }
            }
        }
        return true
    }

    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Simplify.UI_MISC
}
