/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2025 QAuxiliary developers
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

package io.github.relimus.hook

import android.content.Context
import android.view.View
import com.github.kyuubiran.ezxhelper.utils.findConstructorOrNull
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.hook.CommonSwitchFunctionHook
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.TIMVersion
import io.github.qauxv.util.requireMinTimVersion
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object HideQZoneAD : CommonSwitchFunctionHook() {
    override val name = "隐藏QQ空间广告"
    override val description = "隐藏QQ空间和好友动态里的广告"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.MAIN_UI_MISC
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_8_9_0) || requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA)

    override fun initOnce() = throwOrTrue {
        if (requireMinQQVersion(QQVersion.QQ_8_9_0)) {
            listOf(
                "com.qzone.reborn.feedpro.widget.comment.QZoneFeedProDetailBottomAdBlockView", // 说说详情页广告
                "com.qzone.reborn.feedpro.itemview.ad.card.QZoneCardAdFeedProItemView",
                "com.qzone.reborn.feedpro.itemview.ad.card.QZoneCardMultiPicAdFeedProItemView",
                "com.qzone.reborn.feedpro.itemview.ad.carousel.QZoneCarouselCardVideoAdFeedProItemView",
                "com.qzone.reborn.feedpro.itemview.ad.contract.QZoneContractCardAdFeedProItemView",
                "com.qzone.reborn.feedpro.itemview.ad.contract.QZoneContractFullFrameAdFeedProItemView",
                "com.qzone.reborn.feedx.itemview.ad.QZoneAdVideoFeedItemView", // 9.2.5 样式(好友动态)
                "com.qzone.reborn.feedx.itemview.ad.QZoneAdRewardFeedItemView",
                "com.qzone.reborn.feedx.itemview.ad.QZoneAdPictureFeedItemView",
                "com.qzone.reborn.feedx.itemview.ad.QZoneAdMDPAFeedItemView"
            ).forEach { className ->
                Initiator.load(className)?.findConstructorOrNull {
                    parameterTypes[0] == Context::class.java
                }?.hookAfter { param ->
                    val view = param.thisObject as View
                    view.visibility = View.GONE
                    view.layoutParams.height = 0
                    view.layoutParams.width = 0
                }
            }
        } else {
            Initiator.loadClass("com.qzone.proxy.feedcomponent.model.gdt.QZoneAdFeedDataExtKt").findMethod {
                name == "isShowingRecommendAd"
            }.hookBefore {
                it.result = true
            }
        }
    }
}
