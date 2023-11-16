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

package com.xiaoniu.hook

import com.github.kyuubiran.ezxhelper.utils.hookBefore
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import xyz.nextalone.util.method

@FunctionHookEntry
@UiItemAgentEntry
object DisableDocOnlinePreview : CommonSwitchFunctionHook() {
    override val name = "禁用文档在线预览"

    override fun initOnce(): Boolean {
        "Lcom/tencent/mobileqq/filemanager/fileviewer/docs/TroopDocsPreviewConfigProcessor;->parse([B)Lcom/tencent/freesia/IConfigData;".method.hookBefore {
            it.result =
                "Lcom/tencent/mobileqq/filemanager/fileviewer/docs/TroopDocsPreviewConfigProcessor;->defaultConfig()Lcom/tencent/freesia/IConfigData;".method.invoke(
                    it.thisObject
                )
        }
        return true
    }

    override val uiItemLocation = FunctionEntryRouter.Locations.Auxiliary.FILE_CATEGORY
}
