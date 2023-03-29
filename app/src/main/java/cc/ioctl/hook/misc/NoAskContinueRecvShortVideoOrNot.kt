/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
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

package cc.ioctl.hook.misc

import android.content.Intent
import de.robv.android.xposed.XC_MethodHook
import io.github.qauxv.base.IDynamicHook
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.router.decorator.BaseSwitchFunctionDecorator
import io.github.qauxv.router.decorator.IStartActivityHookDecorator
import io.github.qauxv.router.dispacher.StartActivityHook
import io.github.qauxv.util.hostInfo


@UiItemAgentEntry
@FunctionHookEntry
object NoAskContinueRecvShortVideoOrNot : BaseSwitchFunctionDecorator(), IStartActivityHookDecorator {

    override val name = "去除是否继续接收弹窗"

    override val description = "去除\"WiFi已断开，是否继续使用移动流量下载视频？\"弹窗(自动停止)"

    override val dispatcher: IDynamicHook = StartActivityHook

    override val uiItemLocation: Array<String> = FunctionEntryRouter.Locations.Simplify.UI_MISC

    override fun onStartActivityIntent(intent: Intent, param: XC_MethodHook.MethodHookParam): Boolean {
        val pkgName = hostInfo.packageName
        if (intent.`package` == pkgName) {
            val target = intent.component?.className
            if (target == "com.tencent.mobileqq.activity.DialogActivity") {
                // The code is really messy in QQAppInterface and DialogActivity.
                // I can hardly understand what they are freaking doing.
                val isInterceptRequired = intent.hasExtra("continue_receive_short_video_or_not") ||
                        (intent.getIntExtra("key_dialog_type", 0) == 0)
                if (isInterceptRequired) {
                    param.result = null
                    return true
                }
            }
        }
        return true
    }

}
