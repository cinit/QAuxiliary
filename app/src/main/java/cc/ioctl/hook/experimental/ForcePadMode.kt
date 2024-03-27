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

package cc.ioctl.hook.experimental

import cc.ioctl.util.HookUtils
import cc.ioctl.util.Reflex
import com.github.kyuubiran.ezxhelper.utils.getStaticObject
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.SyncUtils
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.NPadUtil_initDeviceType
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object ForcePadMode : CommonSwitchFunctionHook(targetProc = SyncUtils.PROC_ANY, arrayOf(NPadUtil_initDeviceType)) {

    override val name = "强制平板模式"
    override val description = "支持 QQ 8.9.15及以上，未经测试，谨慎使用"
    override val extraSearchKeywords: Array<String> = arrayOf("pad")
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY
    override val isApplicationRestartRequired = true

    override val isAvailable: Boolean
        get() = requireMinQQVersion(QQVersion.QQ_8_9_15)

    override fun initOnce() = throwOrTrue {
        check(isAvailable) { "ForcePadMode is not available" }
        HookUtils.hookAfterIfEnabled(this, DexKit.requireMethodFromCache(NPadUtil_initDeviceType)) {
            val type = Initiator._DeviceType().getStaticObject("TABLET")
            Reflex.setStaticObject(DexKit.requireClassFromCache(NPadUtil_initDeviceType), "b", type)
        }
//        val k = Initiator.loadClass("com.tencent.mobileqq.injector.a");
//        val getAppId = k.getDeclaredMethod("getAppId")
//        HookUtils.hookAfterAlways(this, getAppId) {
//            val result = it.result as Int
//            Log.i("ForcePadMode getAppId: $result")
//        }
    }

}
