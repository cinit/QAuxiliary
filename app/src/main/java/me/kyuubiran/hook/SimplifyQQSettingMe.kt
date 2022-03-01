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
package me.kyuubiran.hook

import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.forEach
import androidx.core.view.forEachIndexed
import androidx.core.view.get
import androidx.core.view.size
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.util.Initiator
import io.github.qauxv.util.LicenseStatus
import io.github.qauxv.util.Log
import io.github.qauxv.util.QQVersion.*
import io.github.qauxv.util.requireMinQQVersion
import me.kyuubiran.util.setViewZeroSize
import io.github.qauxv.tlb.ConfigTable
import xyz.nextalone.base.MultiItemDelayableHook
import xyz.nextalone.util.get
import xyz.nextalone.util.hide
import xyz.nextalone.util.throwOrTrue
import java.util.*

//侧滑栏精简
@FunctionHookEntry
@UiItemAgentEntry
object SimplifyQQSettingMe : MultiItemDelayableHook("SimplifyQQSettingMe") {

    const val MidContentName = "SimplifyQQSettingMe::MidContentName"

    override val preferenceTitle: String = "侧滑栏精简"
    override val allItems = setOf<String>()
    override val uiItemLocation = FunctionEntryRouter.Locations.Simplify.SLIDING_UI
    override val isAvailable = requireMinQQVersion(QQ_8_4_1)
    override val enableCustom = false

    //Form 8.4.1
    //Body = [0,1,0,0,0,1,4] || [0,1,0,0,0,1,4,0]
    override var items: MutableList<String> = mutableListOf(
        "夜间模式",            //夜间模式 [0,1,0,0,0,1,6,1]
        "登录达人",          //登录达人 [0,1,0,0,0,1,6,2]
        "当前温度",         //当前温度 [0,1,0,0,0,1,6,3]
        "开播啦鹅",         //开播啦鹅 [0,1,0,0,0,1,4,0,1] || [0,1,0,0,0,1,4,0,1,1,1]
        "我的小世界",        //我的小世界 [0,1,0,0,0,1,4,0,2] || [0,1,0,0,0,1,4,0,1,2,1]
        "开通会员",         //开通会员 [0,1,0,0,0,1,4,0,3] || [0,1,0,0,0,1,4,0,1,3,1]
        "我的钱包",         //我的钱包 [0,1,0,0,0,1,4,0,4] || [0,1,0,0,0,1,4,0,1,4,1]
        "个性装扮",         //个性装扮 [0,1,0,0,0,1,4,0,5] || [0,1,0,0,0,1,4,0,1,5,1]
        "情侣空间",         //情侣空间 [0,1,0,0,0,1,4,0,6] || [0,1,0,0,0,1,4,0,1,6,1]
        "我的收藏",         //我的收藏 [0,1,0,0,0,1,4,0,7] || [0,1,0,0,0,1,4,0,1,7,1]
        "我的相册",         //我的相册 [0,1,0,0,0,1,4,0,8] || [0,1,0,0,0,1,4,0,1,8,1]
        "我的文件",         //我的文件 [0,1,0,0,0,1,4,0,9] || [0,1,0,0,0,1,4,0,1,9,1]
        "我的日程",         //我的日程 [0,1,0,0,0,1,4,0,10] || [0,1,0,0,0,1,4,0,1,10,1]
        "我的视频",         //我的视频 [0,1,0,0,0,1,4,0,11] || [0,1,0,0,0,1,4,0,1,11,1]
        "小游戏",          //小游戏 [0,1,0,0,0,1,4,0,12] || [0,1,0,0,0,1,4,0,1,12,1]
        "腾讯文档",         //腾讯文档 [0,1,0,0,0,1,4,0,13] || [0,1,0,0,0,1,4,0,1,13,1]
        "每日打卡",         //每日打卡 [0,1,0,0,0,1,4,0,14] || [0,1,0,0,0,1,4,0,1,14,1]
        "王卡免流量特权",   //开通王卡 [0,1,0,0,0,1,4,0,15] || [0,1,0,0,0,1,4,0,1,15,1]
        "厘米秀",
    )

    private val keyWords: SortedMap<String, String> = sortedMapOf(
        "间" to "夜间模式",
        "达" to "登录达人",
        "天" to "登录达人",
        "播" to "开播啦鹅",
        "世界" to "我的小世界",
        "会员" to "开通会员",
        "vip" to "开通会员",
        "VIP" to "开通会员",
        "钱包" to "我的钱包",
        "装扮" to "个性装扮",
        "情侣" to "情侣空间",
        "相册" to "我的相册",
        "收藏" to "我的收藏",
        "文件" to "我的文件",
        "日程" to "我的日程",
        "视频" to "我的视频",
        "游戏" to "小游戏",
        "文档" to "腾讯文档",
        "打卡" to "每日打卡",
        "王卡" to "王卡免流量特权",
        "流量" to "王卡免流量特权",
        "送12个月" to "王卡免流量特权",
        "厘米" to "厘米秀",
    )

    @Throws(Exception::class)
    override fun initOnce() = throwOrTrue {
        val clz = Initiator.load("com.tencent.mobileqq.activity.QQSettingMe")
        XposedBridge.hookAllConstructors(clz, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (LicenseStatus.sDisableCommonHooks) return
                if (!isEnabled) return
                try {
                    //中间部分(QQ会员 我的钱包等)
                    val midContentName = ConfigTable.getConfig<String>(MidContentName)
                    val midContentListLayout = if (requireMinQQVersion(QQ_8_6_5)) {
                        param.thisObject.get(midContentName, LinearLayout::class.java)
                    } else {
                        param.thisObject.get(midContentName, View::class.java) as LinearLayout
                    }
                    //底端部分 设置 夜间模式 达人 等
                    val underSettingsName = if (requireMinQQVersion(QQ_8_6_0)) "l" else "h"
                    val underSettingsLayout = if (requireMinQQVersion(QQ_8_6_5)) {
                        val parent = midContentListLayout?.parent?.parent as ViewGroup
                        var ret: LinearLayout? = null
                        parent.forEach {
                            if (it is LinearLayout && it[0] is LinearLayout) {
                                ret = it
                            }
                        }
                        ret
                    } else {
                        param.thisObject.get(underSettingsName, View::class.java) as LinearLayout
                    }
                    underSettingsLayout?.forEachIndexed { i, v ->
                        val tv = (v as LinearLayout)[1] as TextView
                        val text = tv.text
                        if (stringHit(text.toString()) || i == 3 && activeItems.contains("当前温度")) {
                            v.setViewZeroSize()
                        }
                    }
                    val midRemovedList: MutableList<Int> = mutableListOf()
                    midContentListLayout?.forEach {
                        val child = it as LinearLayout
                        val tv = if (child.size == 1) {
                            (child[0] as LinearLayout)[1]
                        } else {
                            child[1]
                        } as TextView
                        val text = tv.text.toString()
                        if (stringHit(text)) {
                            midRemovedList.add(midContentListLayout.indexOfChild(child))
                        }
                    }
                    midRemovedList.sorted().forEachIndexed { index, i ->
                        if (requireMinQQVersion(QQ_8_8_11)) {
                            midContentListLayout?.removeViewAt(i - index)
                        } else {
                            midContentListLayout?.getChildAt(i)?.hide()
                        }
                    }
                } catch (e: Throwable) {
                    traceError(e)
                }
            }
        })
        XposedBridge.hookAllMethods(ViewTreeObserver::class.java, "dispatchOnGlobalLayout", object : XC_MethodReplacement() {
            override fun replaceHookedMethod(param: MethodHookParam) {
                try {
                    XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                } catch (e: Exception) {
                    if (e.stackTraceToString().contains("QQSettingMe")) {
                        Log.d("SimplifyQQSettingMe: have prevented crash")
                    } else {
                        throw e
                    }
                }
            }
        })
    }

    private fun stringHit(string: String): Boolean {
        val mActiveItems = activeItems
        for (pair in keyWords) {
            if (string.contains(pair.key) && mActiveItems.contains(pair.value)) {
                return true
            }
        }
        return false
    }

    //以下三个方法是曾经辉煌的MultiConfigItem和BaseMultiConfigDelayableHook最后的墓碑

    fun hasConfig(name: String): Boolean {
        val cfg = ConfigManager.getDefaultConfig()
        return cfg.contains(this::class.java.simpleName + '$' + name)
    }

    fun setBooleanConfig(name: String, value: Boolean) {
        val cfg = ConfigManager.getDefaultConfig()
        cfg.putBoolean(this::class.java.simpleName + '$' + name, value)
    }

    fun getBooleanConfig(name: String): Boolean {
        val cfg = ConfigManager.getDefaultConfig()
        return cfg.getBooleanOrDefault(this::class.java.simpleName + '$' + name, false)
    }
}
