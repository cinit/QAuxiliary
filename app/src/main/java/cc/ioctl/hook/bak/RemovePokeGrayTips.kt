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
package cc.ioctl.hook.bak

import cc.ioctl.util.HookUtils
import com.github.kyuubiran.ezxhelper.utils.getObjectAs
import io.github.qauxv.util.Initiator.loadClass
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import xyz.nextalone.util.throwOrTrue

//屏蔽戳一戳灰字提示
object RemovePokeGrayTips : CommonSwitchFunctionHook("kr_remove_poke_tips") {
    val keys = listOf("拍了拍", "戳了戳", "亲了亲", "抱了抱", "揉了揉", "喷了喷", "踢了踢", "舔了舔", "捏了捏", "摸了摸")

    override val name = "屏蔽戳一戳灰字提示"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.MAIN_UI_MSG

    override fun initOnce() = throwOrTrue {
        val Msg = loadClass("com.tencent.imcore.message.QQMessageFacade\$Message")
        val MsgRecord = loadClass("com.tencent.mobileqq.data.MessageRecord")
        for (m in Initiator._QQMessageFacade().declaredMethods) {
            val argt = m.parameterTypes
            if (m.name == "a" && argt.size == 1 && argt[0] == Msg::class.java) {
                HookUtils.hookBeforeIfEnabled(this, m) { param ->
                    val msg = param.args[0].getObjectAs<String>("msg", String::class.java)
                }
            }
        }
    }
}
