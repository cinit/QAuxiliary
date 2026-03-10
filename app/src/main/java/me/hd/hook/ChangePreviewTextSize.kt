/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2026 QAuxiliary developers
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

package me.hd.hook

import android.os.Bundle
import android.widget.TextView
import cc.ioctl.util.hookAfterIfEnabled
import com.github.kyuubiran.ezxhelper.utils.getObjectByType
import com.github.kyuubiran.ezxhelper.utils.invokeMethod
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion

@FunctionHookEntry
@UiItemAgentEntry
object ChangePreviewTextSize : CommonSwitchFunctionHook() {

    override val name = "篡改预览字体大小"
    override val description = "双击文本消息启动的预览界面, 缩小字体大小方便预览"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.UI_CHAT_MSG
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_9_2_30)

    override fun initOnce(): Boolean {
        val previewActivityClass = Initiator.loadClass("com.tencent.mobileqq.activity.TextPreviewActivity")
        val containerViewClass = Initiator.loadClass("com.tencent.qqnt.textpreview.PreviewTextContainerView")
        hookAfterIfEnabled(previewActivityClass.getDeclaredMethod("onCreate", Bundle::class.java)) { param ->
            val containerView = param.thisObject.getObjectByType(containerViewClass)
            val textView = containerView.invokeMethod { returnType == TextView::class.java && parameterCount == 0 } as TextView
            textView.textSize = 14f
        }
        return true
    }
}
