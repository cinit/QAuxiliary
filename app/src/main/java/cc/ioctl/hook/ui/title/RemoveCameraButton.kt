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

package cc.ioctl.hook.ui.title

import android.view.View
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.isTim
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.util.throwOrTrue
import top.linl.util.reflect.FieldUtils
import android.widget.ImageView
import cc.ioctl.util.hookAfterIfEnabled
import io.github.qauxv.util.PlayQQVersion
import io.github.qauxv.util.requireRangePlayQQVersion

@FunctionHookEntry
@UiItemAgentEntry
object RemoveCameraButton : CommonSwitchFunctionHook("kr_disable_camera_button") {

    override val name: String = "屏蔽消息界面标题栏相机/小世界图标"

    override val isAvailable: Boolean get() = !isTim() && !requireMinQQVersion(QQVersion.QQ_9_0_8)

    override fun initOnce() = throwOrTrue {
        findMethod(Initiator._ConversationTitleBtnCtrl()) {
            val methodName = when {
                requireMinQQVersion(QQVersion.QQ_8_9_63_BETA_11345) -> "D"
                requireMinQQVersion(QQVersion.QQ_8_9_10) -> "C"
                requireMinQQVersion(QQVersion.QQ_8_8_93) -> "G"
                else -> "a"
            }
            name == methodName && returnType == Void.TYPE && parameterTypes.contentEquals(arrayOf(View::class.java))
        }.hookBefore {
            if (!isEnabled) return@hookBefore; it.result = null
        }
        if (requireRangePlayQQVersion(PlayQQVersion.PlayQQ_8_2_11, PlayQQVersion.PlayQQ_8_2_11))
            hookAfterIfEnabled(Initiator.loadClass("aawg").getDeclaredMethod("a")) { param ->
                val view = FieldUtils.getField(param.thisObject, "a", android.widget.ImageView::class.java) as ImageView
                view.visibility = View.GONE
                FieldUtils.setField(param.thisObject, "a", android.widget.ImageView::class.java, view)
            }
        else
            findMethod(Initiator._ConversationTitleBtnCtrl()) {
                val methodName = when {
                    requireMinQQVersion(QQVersion.QQ_8_9_63_BETA_11345) -> "C"
                    requireMinQQVersion(QQVersion.QQ_8_9_10) -> "B"
                    requireMinQQVersion(QQVersion.QQ_8_9_5) -> "E"
                    requireMinQQVersion(QQVersion.QQ_8_8_93) -> "F"
                    else -> "a"
                }
                name == methodName && returnType == Void.TYPE && parameterTypes.isEmpty()
            }.hookBefore {
                if (!isEnabled) return@hookBefore; it.result = null
            }
    }

    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Simplify.MAIN_UI_TITLE
}
