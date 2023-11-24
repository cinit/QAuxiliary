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
package cc.ioctl.hook.ui.title

import android.widget.LinearLayout
import cc.ioctl.util.HookUtils
import cc.ioctl.util.HostInfo
import com.github.kyuubiran.ezxhelper.utils.findField
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.getObjectAs
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.setViewZeroSize
import de.robv.android.xposed.XposedBridge
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.Initiator.loadClass
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.hostInfo
import io.github.qauxv.util.isTim
import io.github.qauxv.util.requireMinQQVersion
import java.lang.reflect.Field

@FunctionHookEntry
@UiItemAgentEntry
object RemoveDailySign : CommonSwitchFunctionHook("kr_remove_daily_sign") {

    override val name = "移除侧滑栏左上角打卡"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.SLIDING_UI

    override val isAvailable: Boolean get() = !isTim()

    override fun initOnce(): Boolean {
        if (hostInfo.packageName != HostInfo.PACKAGE_NAME_QQ) return false
        val callback = HookUtils.afterIfEnabled(this) { param ->
            // em_drawer_sign_up
            val dailySignName = when {
                requireMinQQVersion(QQVersion.QQ_8_9_70) -> "e0"
                requireMinQQVersion(QQVersion.QQ_8_9_68) -> "h0"
                requireMinQQVersion(QQVersion.QQ_8_9_28) -> "i0"
                requireMinQQVersion(QQVersion.QQ_8_9_25) -> "h0"
                // gap
                requireMinQQVersion(QQVersion.QQ_8_9_3) -> "d0"
                requireMinQQVersion(QQVersion.QQ_8_8_93) -> "c0"
                requireMinQQVersion(QQVersion.QQ_8_8_17) -> "O"
                requireMinQQVersion(QQVersion.QQ_8_8_11) -> "N"
                hostInfo.versionCode == QQVersion.QQ_8_6_0 -> "b"
                else -> "a"
            }
            param.thisObject.getObjectAs<LinearLayout>(dailySignName, LinearLayout::class.java).setViewZeroSize()
        }
        XposedBridge.hookAllConstructors(
            loadClass(
                if (requireMinQQVersion(QQVersion.QQ_8_9_90)) {
                    "com.tencent.mobileqq.QQSettingMeView"
                } else if (requireMinQQVersion(QQVersion.QQ_8_9_25)) {
                    "com.tencent.mobileqq.activity.QQSettingMeView"
                } else {
                    "com.tencent.mobileqq.activity.QQSettingMe"
                }
            ), callback
        )
        // for NT QQ 8.9.68.11450
        val clazz = Initiator.load(
            if (requireMinQQVersion(QQVersion.QQ_8_9_90))
                "com.tencent.mobileqq.QQSettingMeViewV9"
            else "com.tencent.mobileqq.activity.QQSettingMeViewV9"
        )
        if (clazz != null) {
            clazz.findField {
                val cz = type
                if (cz.name.contains("com.tencent.mobileqq.activity.qqsettingme.bizParts")) {
                    var i = 0
                    for (f in cz.declaredFields) {
                        if (f.type == LinearLayout::class.java) {
                            i++
                        }
                    }
                    i > 2
                } else {
                    false
                }
            }
                .type
                ?.findMethod { name == "onInitView" }?.hookAfter {
                    val fields = arrayListOf<Field>()
                    for (f in it.thisObject.javaClass.declaredFields) {
                        if (f.type == LinearLayout::class.java) {
                            fields.add(f)
                        }
                    }
                    it.thisObject.getObjectAs<LinearLayout>(fields[1].name, LinearLayout::class.java).setViewZeroSize()
                }
        }
        return true
    }
}
