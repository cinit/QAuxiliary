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

import de.robv.android.xposed.XC_MethodHook
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.hostInfo
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.base.MultiItemDelayableHook
import xyz.nextalone.util.hookBefore
import xyz.nextalone.util.method
import xyz.nextalone.util.throwOrTrue

@FunctionHookEntry
@UiItemAgentEntry
object SimplifyPlusPanel : MultiItemDelayableHook("na_simplify_plus_panel_multi") {

    override val preferenceTitle = "精简加号菜单"
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.CHAT_OTHER

    override val allItems = setOf(
        "图片",
        "拍摄",
        "语音通话",
        "视频通话",
        "一起派对",
        "戳一戳",
        "视频包厢",
        "红包",
        "位置",
        "文件",
        "一起听歌",
        "分享屏幕",
        "收藏",
        "热图",
        "一起玩",
        "涂鸦",
        "转账",
        "名片",
        "送礼物",
        "腾讯文档",
        "厘米秀",
        "一起K歌",
        "礼物",
        "直播间",
        "签到",
        "匿名",
        "群课堂",
        "健康收集",
        "一起看",
        "投票",
        "收钱",
        "坦白说",
        "超级粉丝团"
    )
    override val defaultItems = setOf<String>()

    override fun initOnce() = throwOrTrue {
        val callback: (XC_MethodHook.MethodHookParam) -> Unit = {
            val list = (it.args[0] as MutableList<*>).listIterator()
            while (list.hasNext()) {
                val item = list.next()
                if (item != null) {
                    val str = try {
                        (item.javaClass.getDeclaredField("name").get(item) as String).toString()
                    } catch (t: Throwable) {
                        try {
                            (item.javaClass.getDeclaredField("a").get(item) as String).toString()
                        } catch (t: Throwable) {
                            (item.javaClass.getDeclaredField("d").get(item) as String).toString()
                        }
                    }
                    if (activeItems.any { string ->
                            string.isNotEmpty() && string in str
                        }) {
                        list.remove()
                    }
                }
            }
        }
        when {
            hostInfo.versionCode >= QQVersion.QQ_8_5_5 -> {
                "Lcom/tencent/mobileqq/activity/aio/pluspanel/PlusPanelViewBinder;->a(Ljava/util/ArrayList;Lcom/tencent/mobileqq/activity/aio/coreui/pluspanel/PanelAdapter;Lcom/tencent/mobileqq/emoticonview/EmoticonPagerRadioGroup;)V".method.hookBefore(
                    this,
                    callback
                )
                "Lcom/tencent/mobileqq/activity/aio/pluspanel/PlusPanelViewBinder;->b(Ljava/util/ArrayList;Lcom/tencent/mobileqq/activity/aio/coreui/pluspanel/PanelAdapter;Lcom/tencent/mobileqq/emoticonview/EmoticonPagerRadioGroup;)V".method.hookBefore(
                    this,
                    callback
                )
            }
            hostInfo.versionCode == QQVersion.QQ_8_5_0 -> {
                "Lcom/tencent/mobileqq/activity/aio/pluspanel/PlusPanelViewBinder;->a(Ljava/util/ArrayList;Lcom/tencent/mobileqq/activity/aio/PanelAdapter;Lcom/tencent/mobileqq/emoticonview/EmoticonPagerRadioGroup;)V".method.hookBefore(
                    this,
                    callback
                )
                "Lcom/tencent/mobileqq/activity/aio/pluspanel/PlusPanelViewBinder;->b(Ljava/util/ArrayList;Lcom/tencent/mobileqq/activity/aio/PanelAdapter;Lcom/tencent/mobileqq/emoticonview/EmoticonPagerRadioGroup;)V".method.hookBefore(
                    this,
                    callback
                )
            }
            hostInfo.versionCode >= QQVersion.QQ_8_4_8 -> {
                "Lcom/tencent/mobileqq/activity/aio/PlusPanel;->a(Ljava/util/ArrayList;)V".method.hookBefore(
                    this,
                    callback
                )
                "Lcom/tencent/mobileqq/activity/aio/PlusPanel;->b(Ljava/util/ArrayList;)V".method.hookBefore(
                    this,
                    callback
                )
            }
            else -> {
                "Lcom/tencent/mobileqq/activity/aio/PlusPanel;->a(Ljava/util/List;)V".method.hookBefore(
                    this,
                    callback
                )
            }
        }
    }

    override val isAvailable: Boolean get() = requireMinQQVersion(QQVersion.QQ_8_0_0)
}
