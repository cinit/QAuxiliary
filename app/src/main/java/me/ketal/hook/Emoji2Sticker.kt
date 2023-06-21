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

package me.ketal.hook

import cc.hicore.QApp.QAppUtils
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.EmoMsgUtils_isSingleLottie_QQNT
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.util.clazz
import xyz.nextalone.util.hookAfter
import xyz.nextalone.util.hookBefore
import xyz.nextalone.util.method
import xyz.nextalone.util.set
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object Emoji2Sticker : CommonSwitchFunctionHook(arrayOf(EmoMsgUtils_isSingleLottie_QQNT)) {

    override val name = "关闭大号Emoji"
    override val description = "禁用新版QQ输入单个Emoji后发送大表情"
    override val uiItemLocation = FunctionEntryRouter.Locations.Entertainment.ENTERTAIN_CATEGORY

    override val isAvailable: Boolean
        get() = requireMinQQVersion(QQVersion.QQ_8_7_5)

    override fun initOnce() = throwOrTrue {
        if (QAppUtils.isQQnt()) {
            DexKit.requireMethodFromCache(EmoMsgUtils_isSingleLottie_QQNT).hookBefore(this) {
                it.result = false
            }
        }
        "com.tencent.mobileqq.emoticonview.AniStickerSendMessageCallBack".clazz?.method("parseMsgForAniSticker")
            ?.hookAfter(this) {
                it.result.set("singleAniSticker", false)
            }
    }
}
