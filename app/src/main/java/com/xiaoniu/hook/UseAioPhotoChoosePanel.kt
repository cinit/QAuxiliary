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

package com.xiaoniu.hook

import com.github.kyuubiran.ezxhelper.utils.hookBefore
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.requireRangeQQVersion
import xyz.nextalone.util.clazz
import xyz.nextalone.util.hookBeforeAllConstructors
import xyz.nextalone.util.method
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object UseAioPhotoChoosePanel : CommonSwitchFunctionHook() {

    override val name = "还原图片选择面板"
    override val description = "从QQ9.0.20开始，普通模式半屏的图片选择面板可能被移除\n9.0.65起官方已提供开关，通用-发图方式"
    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.CHAT_CATEGORY
    override val isAvailable = requireRangeQQVersion(QQVersion.QQ_9_0_20, QQVersion.QQ_9_0_60)

    override fun initOnce() = throwOrTrue {
        if (requireMinQQVersion(QQVersion.QQ_9_0_55)) {
            // 1003改其他, 触发半屏加号面板
            "Lcom/tencent/mobileqq/aio/shortcurtbar/AIOShortcutBarVM\$c;".clazz!!.hookBeforeAllConstructors {
                it.args.forEachIndexed { index, arg ->
                    if ((arg is Int) && (arg == 1003)) it.args[index] = 114514
                }
            }
            // 还原1003，加号面板显示半屏选图面板
            "Lcom/tencent/input/base/panelcontainer/h\$l;".clazz!!.hookBeforeAllConstructors {
                if (it.args[0] == "AIOShortcutBarVM" && it.args[1] == 114514) it.args[1] = 1003
            }
        } else {
            "Lcom/tencent/mobileqq/aio/api/impl/QQTabApiImpl\$b;->isExperiment(Ljava/lang/String;)Z".method.hookBefore {
                if (it.args[0] == "exp_QQ_aio_photo_choose_B") it.result = false
            }
        }
    }

}