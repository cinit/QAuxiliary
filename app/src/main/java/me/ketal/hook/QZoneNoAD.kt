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

package me.ketal.hook

import cc.ioctl.util.Reflex
import io.github.qauxv.SyncUtils
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.TIMVersion
import io.github.qauxv.util.requireMinVersion
import me.ketal.base.PluginDelayableHook
import me.ketal.util.getField
import me.ketal.util.getMethod
import xyz.nextalone.util.hookBefore
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object QZoneNoAD : PluginDelayableHook("ketal_qzone_hook") {
    override val preference = uiSwitchPreference {
        title = "隐藏空间好友热播和广告"
    }

    override val targetProcesses = SyncUtils.PROC_QZONE
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.MAIN_UI_OPERATION_LOG

    override val isAvailable = requireMinVersion(QQVersion.QQ_8_0_0, TIMVersion.TIM_1_0_0)

    override val pluginID = "qzone_plugin.apk"

    override fun startHook(classLoader: ClassLoader) = throwOrTrue {
        "Lcom/qzone/module/feedcomponent/ui/FeedViewBuilder;->setFeedViewData(Landroid/content/Context;Lcom/qzone/proxy/feedcomponent/ui/AbsFeedView;Lcom/qzone/proxy/feedcomponent/model/BusinessFeedData;ZZ)V"
            .getMethod(classLoader)
            ?.hookBefore(this) {
                val obj =
                    "Lcom/qzone/proxy/feedcomponent/model/BusinessFeedData;->cellOperationInfo:Lcom/qzone/proxy/feedcomponent/model/CellOperationInfo;"
                        .getField(classLoader)
                        ?.get(it.args[2])!!
                val hashMap = Reflex.getInstanceObjectOrNull(obj, "busiParam", Map::class.java)
                for (num in hashMap.keys) {
                    if (num == 194) {
                        it.result = null
                    }
                    if (num == 101 && (hashMap[num] as String).contains("v.gdt.qq.com")) {
                        it.result = null
                    }
                }

            }
    }
}
