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

import cc.ioctl.hook.sideswipe.SimplifyQQSettingMe
import cc.ioctl.hook.troop.RemovePlayTogether
import cc.ioctl.hook.ui.chat.ReplyNoAtHook
import io.github.qauxv.bridge.QQMessageFacade
import io.github.qauxv.util.QQVersion.QQ_8_2_0
import io.github.qauxv.util.QQVersion.QQ_8_2_6
import io.github.qauxv.util.QQVersion.QQ_8_3_6
import io.github.qauxv.util.QQVersion.QQ_8_4_1
import io.github.qauxv.util.QQVersion.QQ_8_4_8
import io.github.qauxv.util.QQVersion.QQ_8_5_0
import io.github.qauxv.util.QQVersion.QQ_8_5_5
import io.github.qauxv.util.QQVersion.QQ_8_6_0
import io.github.qauxv.util.QQVersion.QQ_8_6_5
import io.github.qauxv.util.QQVersion.QQ_8_7_0
import io.github.qauxv.util.QQVersion.QQ_8_7_5
import io.github.qauxv.util.QQVersion.QQ_8_8_0
import io.github.qauxv.util.QQVersion.QQ_8_8_11
import io.github.qauxv.util.QQVersion.QQ_8_8_17
import io.github.qauxv.util.QQVersion.QQ_8_8_20
import io.github.qauxv.util.QQVersion.QQ_8_8_23
import io.github.qauxv.util.QQVersion.QQ_8_8_3
import io.github.qauxv.util.QQVersion.QQ_8_8_33
import io.github.qauxv.util.QQVersion.QQ_8_8_35
import io.github.qauxv.util.QQVersion.QQ_8_8_38
import io.github.qauxv.util.QQVersion.QQ_8_8_50
import io.github.qauxv.util.QQVersion.QQ_8_8_68
import io.github.qauxv.util.QQVersion.QQ_8_8_80
import io.github.qauxv.util.QQVersion.QQ_8_8_83
import io.github.qauxv.util.QQVersion.QQ_8_8_93
import io.github.qauxv.util.QQVersion.QQ_8_8_98
import io.github.qauxv.util.QQVersion.QQ_8_9_0
import io.github.qauxv.util.QQVersion.QQ_8_9_13
import io.github.qauxv.util.QQVersion.QQ_8_9_18
import io.github.qauxv.util.QQVersion.QQ_8_9_2
import io.github.qauxv.util.QQVersion.QQ_8_9_25
import io.github.qauxv.util.QQVersion.QQ_8_9_28
import io.github.qauxv.util.QQVersion.QQ_8_9_3
import io.github.qauxv.util.QQVersion.QQ_8_9_63_BETA_11345
import io.github.qauxv.util.QQVersion.QQ_8_9_68
import io.github.qauxv.util.QQVersion.QQ_8_9_70
import io.github.qauxv.util.QQVersion.QQ_8_9_8
import io.github.qauxv.util.QQVersion.QQ_8_9_90
import io.github.qauxv.util.QQVersion.QQ_9_0_0
import io.github.qauxv.util.QQVersion.QQ_9_0_20
import io.github.qauxv.util.QQVersion.QQ_9_0_35
import io.github.qauxv.util.QQVersion.QQ_9_0_85
import io.github.qauxv.util.QQVersion.QQ_9_0_90
import me.ketal.hook.SortAtPanel
import xyz.nextalone.hook.ChatWordsCount

class QQConfigTable : ConfigTableInterface {

    override val configs: Map<String, Map<Long, Any>> = mapOf(
    )

    override val rangingConfigs: Map<String, Map<Long, Any>> = mapOf(
        ReplyNoAtHook::class.java.simpleName to mapOf(
            QQ_8_2_0 to "l",
            QQ_8_2_6 to "m",
            QQ_8_3_6 to "n",
            QQ_8_4_8 to "createAtMsg",
            QQ_8_5_5 to "l",
            QQ_8_6_0 to "com/tencent/mobileqq/activity/aio/rebuild/input/InputUIUtils",
            QQ_8_9_0 to "com/tencent/mobileqq/activity/aio/rebuild/input/b",
            QQ_8_9_8 to "com/tencent/mobileqq/activity/aio/rebuild/input/d",
        ),

        ChatWordsCount::class.java.simpleName to mapOf(
            QQ_8_5_0 to "ivc",
            QQ_8_6_5 to "mvm",
            QQ_8_7_0 to "mxh",
            QQ_8_7_5 to "mxn",
            QQ_8_8_0 to "mxz",
            QQ_8_8_3 to "myn",
            QQ_8_8_20 to "n87",
            QQ_8_8_23 to "n_d",
            QQ_8_8_33 to "nbn",
            QQ_8_8_35 to "nci",
            QQ_8_8_38 to "ncy",
            QQ_8_8_50 to "nf4",
            QQ_8_8_68 to "nm7",
            QQ_8_8_80 to "nmx",
            QQ_8_8_83 to "nnl",
            // NT begin
            QQ_8_9_63_BETA_11345 to "nxj",
            QQ_8_9_68 to "nyb",
            QQ_8_9_70 to "nyn"
        ),

        //中间部分(QQ会员 我的钱包等)
        SimplifyQQSettingMe.MidContentName to mapOf(
            QQ_8_4_1 to "k",
            QQ_8_6_0 to "n",
            QQ_8_6_5 to "c",
            QQ_8_7_0 to "b",
            QQ_8_8_11 to "R",
            QQ_8_8_17 to "S",
            QQ_8_8_93 to "Y0",
            QQ_8_8_98 to "S0",
            QQ_8_9_2 to "R0",
            QQ_8_9_3 to "X0",
            QQ_8_9_13 to "h0",
            // gap
            QQ_8_9_25 to "l0",
            QQ_8_9_28 to "m0",
            QQ_8_9_68 to "l0",
            QQ_8_9_90 to "i0",
            QQ_9_0_0 to "f0",
            QQ_9_0_20 to "e0",
            QQ_9_0_35 to "g0",
            QQ_9_0_85 to "h0",
            QQ_9_0_90 to "f0",
        ),

        SortAtPanel.sessionInfoTroopUin to mapOf(
            QQ_8_4_1 to "a",
            QQ_8_8_11 to "b",
            QQ_8_8_93 to "f",
        ),

        // 打卡
        RemovePlayTogether.ClockInEntryHelper to mapOf(
            QQ_8_4_8 to "d",
            QQ_8_8_20 to "f",
            QQ_8_8_93 to "n",
        ),

        // 一起嗨
        RemovePlayTogether.TogetherControlHelper to mapOf(
            QQ_8_4_1 to "h",
            QQ_8_4_8 to "g",
            QQ_8_8_20 to "n",
            QQ_8_8_93 to "q",
        ),

        QQMessageFacade::class.java.simpleName to mapOf(
            QQ_8_2_0 to "b",
            QQ_8_8_93 to "z2",
            QQ_8_9_18 to "B2",
        ),
    )
}
