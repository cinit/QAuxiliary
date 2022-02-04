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
package me.kyuubiran.hook

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import cc.ioctl.util.HookUtils
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import me.kyuubiran.util.getMethods
import me.kyuubiran.util.putObject
import xyz.nextalone.util.throwOrTrue
import java.lang.reflect.Method

//移除小红点
@UiItemAgentEntry
@FunctionHookEntry
object RemoveRedDot : CommonSwitchFunctionHook("kr_remove_red_dot") {

    override val name = "移除小红点"

    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.UI_MISC

    private val TRANSPARENT_PNG = byteArrayOf(
        0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x0D.toByte(), 0x0A.toByte(),
        0x1A.toByte(), 0x0A.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0D.toByte(),
        0x49.toByte(), 0x48.toByte(), 0x44.toByte(), 0x52.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(),
        0x08.toByte(), 0x06.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x1F.toByte(),
        0x15.toByte(), 0xC4.toByte(), 0x89.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x0B.toByte(), 0x49.toByte(), 0x44.toByte(), 0x41.toByte(), 0x54.toByte(), 0x08.toByte(),
        0xD7.toByte(), 0x63.toByte(), 0x60.toByte(), 0x00.toByte(), 0x02.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x05.toByte(), 0x00.toByte(), 0x01.toByte(), 0xE2.toByte(), 0x26.toByte(),
        0x05.toByte(), 0x9B.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x49.toByte(), 0x45.toByte(), 0x4E.toByte(), 0x44.toByte(), 0xAE.toByte(), 0x42.toByte(),
        0x60.toByte(), 0x82.toByte()
    )

    override fun initOnce() = throwOrTrue {
        for (m: Method in getMethods("com.tencent.theme.ResourcesFactory")) {
            val argt = m.parameterTypes
            if ((m.name == "createImageFromResourceStream" || m.name == "a") && argt.size == 7) {
                HookUtils.hookAfterIfEnabled(this, m) { param ->
                    if (!param.args[3].toString().contains("skin_tips_dot")) return@hookAfterIfEnabled
                    putObject(
                        param.result,
                        "a",
                        BitmapFactory.decodeByteArray(
                            TRANSPARENT_PNG,
                            0,
                            TRANSPARENT_PNG.size
                        ),
                        Bitmap::class.java
                    )
                }
            }
        }
    }
}
