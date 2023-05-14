/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
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

package com.codepwn.hook

import cc.ioctl.util.hookBeforeIfEnabled
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion

@FunctionHookEntry
@UiItemAgentEntry
object ForceEnableQQSettingMeV9 : CommonSwitchFunctionHook() {

    override val name = "强制启用新样式侧滑栏"

    override val description = "与禁用新样式状态栏冲突\n(目前仅支持8.9.35及以后)"

    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Simplify.SLIDING_UI

    override val isAvailable: Boolean get() = requireMinQQVersion(QQVersion.QQ_8_9_35)

    override fun initOnce(): Boolean {
        //Lcom/tencent/mobileqq/activity/qqsettingme/utils/a;->e()Z 新样式侧滑
        //Lcom/tencent/mobileqq/activity/qqsettingme/utils/a;->f()Z 新样式侧滑下拉菜单（炒鸡QQ秀）
        val targetClazz = Initiator.loadClass("com.tencent.mobileqq.activity.qqsettingme.utils.a")
        val newSlider = targetClazz.getDeclaredMethod("e")
        val newSlideDownMenu = targetClazz.getDeclaredMethod("f")
        hookBeforeIfEnabled(newSlider) {
            it.result = true
        }
        hookBeforeIfEnabled(newSlideDownMenu) {
            it.result = true
        }
        return true
    }
}
