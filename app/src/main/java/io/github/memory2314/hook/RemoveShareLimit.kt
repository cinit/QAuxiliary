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

package io.github.memory2314.hook

import com.github.kyuubiran.ezxhelper.utils.hookBefore
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.xpcompat.XposedHelpers
import xyz.nextalone.util.get
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object RemoveShareLimit : CommonSwitchFunctionHook() {
    override val name = "去除转发9名联系人限制"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY

    override fun initOnce() = throwOrTrue {
        Initiator.loadClass("com.tencent.mobileqq.activity.ForwardRecentActivity")
            .getDeclaredMethod(
                "add2ForwardTargetList",
                Initiator.loadClass("com.tencent.mobileqq.selectmember.ResultRecord")
            ).hookBefore { param ->
                val resultRecord = param.args[0] ?: return@hookBefore
                val forwardTargetKey = XposedHelpers.callMethod(
                    param.thisObject,
                    "getForwardTargetKey",
                    arrayOf(String::class.java, Int::class.java),
                    resultRecord.get("uin"), XposedHelpers.callMethod(resultRecord, "getUinType")
                )
                val mForwardTargetMap = param.thisObject.get("mForwardTargetMap") as MutableMap<Any, Any?>
                mForwardTargetMap[forwardTargetKey] = XposedHelpers.callMethod(resultRecord, "copyResultRecord", resultRecord)
                XposedHelpers.callMethod(param.thisObject, "refreshRightBtn")
                val mSelectedAndSearchBar = param.thisObject.get("mSelectedAndSearchBar")
                XposedHelpers.callMethod(mSelectedAndSearchBar, "z", ArrayList(mForwardTargetMap.values), true)
                val arrayList = mForwardTargetMap.values.map { it.get("uin") }
                val mSearchFragment = param.thisObject.get("mSearchFragment")
                if (mSearchFragment != null) {
                    XposedHelpers.callMethod(mSearchFragment, "setSelectedAndJoinedUins", arrayList, arrayList)
                }
                param.result = true
            }
    }
}