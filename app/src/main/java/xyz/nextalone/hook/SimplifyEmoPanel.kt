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

import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.TIMVersion
import io.github.qauxv.util.requireMinQQVersion
import io.github.qauxv.util.requireMinTimVersion
import xyz.nextalone.base.MultiItemDelayableHook
import xyz.nextalone.util.clazz
import xyz.nextalone.util.get
import xyz.nextalone.util.hookBefore
import xyz.nextalone.util.method
import xyz.nextalone.util.throwOrTrue
import cc.ioctl.util.hookBeforeIfEnabled
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.PlayQQVersion
import io.github.qauxv.util.requireRangePlayQQVersion
import top.linl.util.reflect.FieldUtils

@FunctionHookEntry
@UiItemAgentEntry
object SimplifyEmoPanel : MultiItemDelayableHook("na_simplify_emo_panel") {
    override val preferenceTitle = "精简表情菜单"

    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.CHAT_EMOTICON

    private val allItemsDict = mapOf(
        13 to "加号菜单",
        7 to "默认表情",
        4 to "收藏表情",
        12 to "热门表情",
        15 to "厘米秀",
        11 to "DIY表情",
        9 to "魔法表情",
        17 to "超级QQ秀",
        -1 to "表情包",
        18 to "搜索表情",
        19 to "动画贴纸",
    )
    override val allItems: Set<String> = allItemsDict.values.toSet()
    override val enableCustom = false

    override fun initOnce() = throwOrTrue {
        val clazz = "com.tencent.mobileqq.emoticonview.BasePanelView".clazz ?: "com.tencent.mobileqq.emoticonview.EmoticonPanelController".clazz
        val method = clazz?.method("initTabView")
        method?.hookBefore(this) {
            val mutableList = if ("com.tencent.mobileqq.emoticonview.BasePanelModel".clazz != null) {
                it.thisObject.get("mPanelController").get("mBasePanelModel").get("panelDataList")
            } else {
                it.thisObject.get("panelDataList")
            } as MutableList<*>
            mutableList.removeAll { item ->
                if (item == null) return@removeAll false
                val i = item.javaClass.getDeclaredField("type").get(item) as Int
                allItemsDict[i] in activeItems || i !in allItemsDict.keys && "表情包" in activeItems
            }
            // fixme unable to locate the slide method
//                "Lcom/tencent/mobileqq/emoticonview/EmoticonTabAdapter;->getView(ILandroid/view/View;Landroid/view/ViewGroup;)Landroid/view/View;".method.hookAfter(
//                    this@SimplifyEmoPanel
//                ) { it2 ->
//                    val view: View = it2.result as View
//                    val layoutParams: ViewGroup.LayoutParams = view.layoutParams
//                    layoutParams.width = hostInfo.application.resources.displayMetrics.widthPixels / mutableList.size
//                    view.layoutParams = layoutParams
//                    it2.result = view
//                }
        }
        if (requireRangePlayQQVersion(PlayQQVersion.PlayQQ_8_2_11, PlayQQVersion.PlayQQ_8_2_11)) {
            hookBeforeIfEnabled(Initiator.loadClass("com.tencent.mobileqq.emoticonview.EmoticonMainPanel").getDeclaredMethod("c", Int::class.java)) { param ->
                val mutableList = FieldUtils.getField(param.thisObject, "a", java.util.List::class.java) as MutableList<*>
                mutableList.removeAll { item ->
                    if (item == null) return@removeAll false
                    val i = FieldUtils.getField(item, "a", Int::class.java) as Int
                    allItemsDict[i] in activeItems || i !in allItemsDict.keys && "表情包" in activeItems
                }
            }
        }
    }

    override val isAvailable: Boolean get() = requireMinQQVersion(QQVersion.QQ_8_5_5) || requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA) || requireRangePlayQQVersion(PlayQQVersion.PlayQQ_8_2_11, PlayQQVersion.PlayQQ_8_2_11)
}
