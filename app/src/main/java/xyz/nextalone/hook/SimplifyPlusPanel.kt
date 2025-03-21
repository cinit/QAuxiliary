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

import cc.hicore.QApp.QAppUtils
import com.github.kyuubiran.ezxhelper.utils.paramCount
import io.github.qauxv.util.xpcompat.XC_MethodHook
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.dexkit.DexKit
import io.github.qauxv.util.dexkit.PlusPanel_PanelAdapter
import io.github.qauxv.util.hostInfo
import io.github.qauxv.util.requireMinQQVersion
import xyz.nextalone.base.MultiItemDelayableHook
import xyz.nextalone.util.clazz
import xyz.nextalone.util.hookBefore
import xyz.nextalone.util.method
import xyz.nextalone.util.throwOrTrue
import java.lang.reflect.Method
import io.github.qauxv.util.PlayQQVersion
import io.github.qauxv.util.requireRangePlayQQVersion
import top.linl.util.reflect.FieldUtils
import xyz.nextalone.util.hookAfter

@FunctionHookEntry
@UiItemAgentEntry
object SimplifyPlusPanel : MultiItemDelayableHook("na_simplify_plus_panel_multi", arrayOf(PlusPanel_PanelAdapter)) {

    override val preferenceTitle = "精简加号菜单"
    override val extraSearchKeywords: Array<String> = arrayOf("+号菜单")
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.CHAT_OTHER

    override val allItems = setOf(
        "王者好礼",
        "超级粉丝团",
        "视频包厢",
        "一起K歌",
        "厘米秀",
        "一起派对",
        "一起听歌",
        "一起玩",
        "一起看",
        "热图",

        "礼物",
        "送礼物",
        "健康收集",
        "直播间",
        "坦白说",
        "群课堂",
        "腾讯文档",

        "签到",
        "拍摄",
        "文件",
        "戳一戳",
        "红包",
        "位置",
        "图片",
        "分享屏幕",
        "收藏",
        "语音通话",
        "涂鸦",
        "视频通话",
        "转账",
        "名片",
        "匿名",
        "投票",
        "收钱",
        "打卡",

        "元梦组队",
        "短视频"
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
                    } catch (e: Throwable) {
                        try {
                            (item.javaClass.getDeclaredField("a").get(item) as String).toString()
                        } catch (e: Throwable) {
                            (item.javaClass.getDeclaredField("d").apply {
                                isAccessible = true
                            }.get(item) as String).toString()
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
        val kPlusPanelViewBinder: Class<*>? = Initiator.load("com/tencent/mobileqq/activity/aio/pluspanel/PlusPanelViewBinder")
        if (kPlusPanelViewBinder != null) {
            // assert QQ.version >= QQVersion.QQ_8_5.0
            val methods = kPlusPanelViewBinder.declaredMethods
            val targetMethods = Array<Method?>(2) { null }
            var i = 0
            for (method in methods) {
                if (method.returnType == Void.TYPE) {
                    val params = method.parameterTypes
                    if (params.size == 3 && params[0] == java.util.ArrayList::class.java) {
                        targetMethods[i++] = method
                    }
                }
            }
            // if more than 2 methods, then IndexOutOfBoundsException, if less than 2 methods, then NullPointerException
            targetMethods[0]!!.hookBefore(this, callback)
            targetMethods[1]!!.hookBefore(this, callback)
        } else {
            // assert QQ.version <= QQVersion.QQ_8_4_8
            if (requireMinQQVersion(QQVersion.QQ_9_0_55)) {
                DexKit.requireClassFromCache(PlusPanel_PanelAdapter).declaredMethods.single { it.paramCount == 1 && it.parameterTypes[0] == ArrayList::class.java }
                    .hookBefore(
                        this,
                        callback
                    )
            } else if (QAppUtils.isQQnt()) {
                "Lcom/tencent/qqnt/pluspanel/adapter/PanelAdapter;".clazz!!.declaredMethods.single { it.paramCount == 1 && it.parameterTypes[0] == ArrayList::class.java }
                    .hookBefore(
                        this,
                        callback
                    )
            } else if (hostInfo.versionCode >= QQVersion.QQ_8_4_8) {
                "Lcom/tencent/mobileqq/activity/aio/PlusPanel;->a(Ljava/util/ArrayList;)V".method.hookBefore(
                    this,
                    callback
                )
                "Lcom/tencent/mobileqq/activity/aio/PlusPanel;->b(Ljava/util/ArrayList;)V".method.hookBefore(
                    this,
                    callback
                )
            } else {
                "Lcom/tencent/mobileqq/activity/aio/PlusPanel;->a(Ljava/util/List;)V".method.hookBefore(
                    this,
                    callback
                )
            }
        }
        if (requireRangePlayQQVersion(PlayQQVersion.PlayQQ_8_2_11, PlayQQVersion.PlayQQ_8_2_11)) {
            val hooker: (XC_MethodHook.MethodHookParam) -> Unit= {
                val pluspanel = it.thisObject as android.widget.RelativeLayout
                val group = pluspanel.getChildAt(0) as android.view.ViewGroup
                if (group.getChildAt(0) != null) {
                    val page = group.getChildAt(0) as android.widget.RelativeLayout
                    val pageline = page.getChildAt(0) as android.widget.LinearLayout
                    val pageline2 = page.getChildAt(1) as android.widget.LinearLayout
                    val page2 = group.getChildAt(1) as android.widget.RelativeLayout
                    val page2line = page2.getChildAt(0) as android.widget.LinearLayout
                    var allItemsMap = mapOf(
                        "语音通话" to pageline.getChildAt(0) as android.widget.RelativeLayout,
                        "视频通话" to pageline.getChildAt(1) as android.widget.RelativeLayout,
                        "位置" to pageline.getChildAt(2) as android.widget.RelativeLayout,
                        "热图" to pageline.getChildAt(3) as android.widget.RelativeLayout,
                        "文件" to pageline2.getChildAt(0) as android.widget.RelativeLayout,
                        "收藏" to pageline2.getChildAt(1) as android.widget.RelativeLayout,
                        "名片" to pageline2.getChildAt(2) as android.widget.RelativeLayout,
                        "一起听歌" to pageline2.getChildAt(3) as android.widget.RelativeLayout,
                        "腾讯文档" to page2line.getChildAt(0) as android.widget.RelativeLayout
                    )
                    val c = FieldUtils.getField(it.thisObject, "a", Initiator.loadClass("com.tencent.mobileqq.activity.BaseChatPie")) as Object
                    if (c.javaClass.toString().equals("class aemr")) {
                        allItemsMap = mapOf(
                            "语音通话" to pageline.getChildAt(0) as android.widget.RelativeLayout,
                            "视频通话" to pageline.getChildAt(1) as android.widget.RelativeLayout,
                            "戳一戳" to pageline.getChildAt(2) as android.widget.RelativeLayout,
                            "热图" to pageline.getChildAt(3) as android.widget.RelativeLayout,
                            "位置" to pageline2.getChildAt(0) as android.widget.RelativeLayout,
                            "文件" to pageline2.getChildAt(1) as android.widget.RelativeLayout,
                            "一起听歌" to pageline2.getChildAt(2) as android.widget.RelativeLayout,
                            "收藏" to pageline2.getChildAt(3) as android.widget.RelativeLayout,
                            "转账" to page2line.getChildAt(0) as android.widget.RelativeLayout,
                            "名片" to page2line.getChildAt(1) as android.widget.RelativeLayout,
                            "腾讯文档" to page2line.getChildAt(2) as android.widget.RelativeLayout
                        )
                    }
                    for (item in activeItems)
                        allItemsMap[item]?.visibility = android.view.View.GONE
                }
            }
            Initiator.loadClass("com.tencent.mobileqq.activity.aio.PlusPanel").getDeclaredMethod("a").hookAfter(this, hooker)
            Initiator.loadClass("com.tencent.mobileqq.activity.aio.PlusPanel").getDeclaredMethod("setVisibility", Int::class.java).hookBefore(this, hooker)
            Initiator.loadClass("com.tencent.mobileqq.activity.aio.PlusPanel").getDeclaredMethod("onInterceptTouchEvent", android.view.MotionEvent::class.java).hookBefore(this, hooker)
        }
    }

    override val isAvailable: Boolean get() = requireMinQQVersion(QQVersion.QQ_8_2_0) || requireRangePlayQQVersion(PlayQQVersion.PlayQQ_8_2_11, PlayQQVersion.PlayQQ_8_2_11)
}
