/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2024 QAuxiliary developers
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

package cc.hicore.hook

import android.text.Spanned
import com.github.kyuubiran.ezxhelper.utils.hookReplace
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.UnlockTroopNameLimitClass
import io.github.qauxv.util.requireMaxQQVersion
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object UnlockTroopNameLimit : CommonSwitchFunctionHook(
    targets = arrayOf(UnlockTroopNameLimitClass)
) {
    override val name = "允许群名带表情"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.EXPERIMENTAL_CATEGORY
    override val isAvailable = requireMaxQQVersion(QQVersion.QQ_9_0_90)

    override fun initOnce() = throwOrTrue {
        DexKit.requireClassFromCache(UnlockTroopNameLimitClass)
            .getDeclaredMethod("filter", CharSequence::class.java, Int::class.java, Int::class.java, Spanned::class.java, Int::class.java, Int::class.java)
            .hookReplace { null }
    }
}