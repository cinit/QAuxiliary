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

package xyz.nextalone.hook

import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.dexkit.DexKit
import xyz.nextalone.util.hookBefore
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object TrimMessage : CommonSwitchFunctionHook(
    dexDeobfIndexes = intArrayOf(DexKit.N_ChatActivityFacade_sendMsgButton)
) {

    override val name = "移除消息前后的空格"

    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.CHAT_OTHER

    override fun initOnce(): Boolean = throwOrTrue {
        DexKit.getMethodFromCache(DexKit.N_ChatActivityFacade_sendMsgButton)!!.hookBefore(this) {
            it.args[3] = trimMessage(it.args[3] as String)
        }
    }

    private fun trimMessage(message: String): String {
        val charArray: CharArray = message.toCharArray()
        val arrayList = ArrayList<String>()
        var i = 0
        while (i < charArray.size) {
            val c = charArray[i]
            if (c.code == 20 && i < charArray.size - 1) {
                val c2 = charArray[i + 1]
                if (c2.code in 1..254) {
                    // 2-byte small emoticon, 254 in total
                    arrayList.add(message.substring(i, i + 2))
                    i += 2
                } else if (c2.code == 255 && i < charArray.size - 4) {
                    // 5-byte small emoticon
                    arrayList.add(message.substring(i, i + 5))
                    i += 5
                } else {
                    // possible broken emoticon, leave it as is
                    arrayList.add(c.toString())
                    i++
                }
            } else {
                arrayList.add(c.toString())
                i++
            }
        }
        // remove first and last space
        while (arrayList.size > 0 && arrayList[0] == " ") {
            arrayList.removeFirst()
        }
        while (arrayList.size > 0 && arrayList[arrayList.size - 1] == " ") {
            arrayList.removeLast()
        }
        return arrayList.joinToString("")
    }
}
