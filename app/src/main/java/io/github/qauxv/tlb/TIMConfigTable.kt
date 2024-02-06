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
package io.github.qauxv.tlb

import cc.ioctl.hook.entertainment.AutoMosaicName
import cc.ioctl.hook.ui.chat.ReplyNoAtHook
import io.github.qauxv.bridge.QQMessageFacade
import io.github.qauxv.util.TIMVersion.TIM_1_0_0
import io.github.qauxv.util.TIMVersion.TIM_3_0_0
import io.github.qauxv.util.TIMVersion.TIM_3_1_1
import io.github.qauxv.util.TIMVersion.TIM_3_3_0
import io.github.qauxv.util.TIMVersion.TIM_3_3_1
import io.github.qauxv.util.TIMVersion.TIM_3_5_0
import io.github.qauxv.util.TIMVersion.TIM_3_5_6

class TIMConfigTable : ConfigTableInterface {

    override val configs: Map<String, Map<Long, Any>> = mapOf(
    )

    override val rangingConfigs: Map<String, Map<Long, Any>> = mapOf(

        //key:public \S* \(boolean
        QQMessageFacade::class.java.simpleName to mapOf(
            TIM_1_0_0 to "b",
            TIM_3_0_0 to "wa",
            TIM_3_1_1 to "PK",
            TIM_3_3_0 to "PO",
            TIM_3_5_0 to "PB",
        ),

        ReplyNoAtHook::class.java.simpleName to mapOf(
            TIM_3_1_1 to "wg",
            TIM_3_3_0 to "wk",
            TIM_3_3_1 to "wf",
            TIM_3_5_6 to "n",
        ),

        AutoMosaicName::class.java.simpleName to mapOf(
            TIM_3_0_0 to "jU",
            TIM_3_1_1 to "wm",
            TIM_3_3_0 to "wq",
            TIM_3_5_0 to "wl",
        ),
    )

}
