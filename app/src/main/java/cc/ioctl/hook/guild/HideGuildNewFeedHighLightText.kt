/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the qwq233 Universal License
 * as published on https://github.com/qwq233/license; either
 * version 2 of the License, or any later version and our EULA as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the qwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package cc.ioctl.hook.guild

import android.content.Context
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.core.text.buildSpannedString
import cc.hicore.QApp.QAppUtils
import cc.ioctl.util.hookBeforeIfEnabled
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion

@UiItemAgentEntry
@FunctionHookEntry
object HideGuildNewFeedHighLightText : CommonSwitchFunctionHook() {

    override val name = "隐藏有新帖红字"

    override val description: CharSequence
        get() = buildSpannedString {
            append("隐藏对话列表的")
            append("[有新帖]", ForegroundColorSpan(0xfff74c30.toInt()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            append("红字")
        }

    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Simplify.MAIN_UI_MSG_LIST

    override val isAvailable: Boolean get() = requireMinQQVersion(QQVersion.QQ_8_9_23) && !QAppUtils.isQQnt()

    override fun initOnce(): Boolean {
        // com.tencent.troopguild.api.impl.TroopGuildApiImpl->getHighLightStringByOptType(Context, int)String
        val kTroopGuildApiImpl = Initiator.loadClass("com.tencent.troopguild.api.impl.TroopGuildApiImpl")
        val getHighLightStringByOptType = kTroopGuildApiImpl.getDeclaredMethod(
            "getHighLightStringByOptType",
            Context::class.java,
            Int::class.javaPrimitiveType
        )
        // GuildGroupOptType.KNEWFEED.ordinal() is 1
        hookBeforeIfEnabled(getHighLightStringByOptType) { param ->
            if (param.args[1] == 1) {
                param.result = ""
            }
        }
        return true
    }

}
