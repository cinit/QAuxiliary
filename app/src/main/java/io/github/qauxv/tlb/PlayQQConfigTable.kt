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

import cc.ioctl.hook.ReplyNoAtHook
import cc.ioctl.hook.VasProfileAntiCrash
import io.github.qauxv.util.PlayQQVersion.*
import me.kyuubiran.hook.AutoMosaicName

class PlayQQConfigTable : ConfigTableInterface {

    override val configs: Map<String?, Map<Long, Any>> = mapOf(

            VasProfileAntiCrash::class.java.simpleName to mapOf(
                    PlayQQ_8_2_9 to "ause",
                    PlayQQ_8_2_9_1 to "ause",
                    PlayQQ_8_2_10 to "ausa",
                    PlayQQ_8_2_11 to "ausf",
            ),

            )

    override val rangingConfigs: Map<String?, Map<Long, Any>> = mapOf(
            ReplyNoAtHook::class.java.simpleName to mapOf(
                    PlayQQ_8_2_9 to "m",
            ),

            AutoMosaicName::class.java.simpleName to mapOf(
                    PlayQQ_8_2_9 to "r",
            ),
    )

}
