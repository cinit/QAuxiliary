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

import androidx.appcompat.app.AlertDialog
import io.github.qauxv.base.annotation.FunctionHookEntry
import io.github.qauxv.base.annotation.UiItemAgentEntry
import io.github.qauxv.dsl.FunctionEntryRouter
import io.github.qauxv.dsl.uiClickableItem
import io.github.qauxv.util.Log
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.requireMinQQVersion
import me.hd.base.KuiklyDelayableHook
import me.hd.util.hookAfterIfEnabled
import me.ketal.data.ConfigData
import org.luckypray.dexkit.DexKitBridge
import java.io.File

@FunctionHookEntry
@UiItemAgentEntry
object CustomPetHomeBg : KuiklyDelayableHook() {
    private val bgMap = mapOf(
        18 to "黄色爪爪 [免费]",
        19 to "蓝色雪花 [免费]",
        23 to "薄荷清新 [600点券]",
        24 to "沉稳灰蓝 [10000金币]",
        25 to "暖阳午后 [会员免费]",
        28 to "绿色童话 [会员免费]",
        29 to "蓝色童话 [10000金币]",
        30 to "粉色童话 [600点券]",
        38 to "简约星阁 [600点券]",
        40 to "撞色几何 [600点券]",
        41 to "意式海岸 [600点券]",
        44 to "朝朝落霞 [600点券]",
        45 to "夕夕星河 [600点券]",
        46 to "浪花泡泡鱼 [98钻石]",
        47 to "绮梦毕业礼 [学业获得]",
        48 to "暖阳小厨 [职业获得·掌勺主厨]",
        // 0 to "绘梦画苑 [职业获得·调色师]",
        51 to "星月魔堡 [职业获得·星语巫师]",
        52 to "雾夜公馆 [职业获得·破案高手]",
        53 to "浮梦云屋 [职业获得·织梦者]",
        54 to "星光舞台 [职业获得·人气偶像]",
        // 55 to "???? [职业获得·内门武师]",
        56 to "蓝调画阁 [限时获得·名画之旅]",
        57 to "台夫特之晨 [限时获得·名画之旅]",
        58 to "向阳花厅 [限时获得·名画之旅]",
        59 to "阿尔勒之夏 [限时获得·名画之旅]",
        // 60 to "月儿圆圆 [10000金币]",
        // 61 to "花田诗意 [98钻石]",
        // 72 to "梦幻糖果 [10000金币]",
        // 73 to "木屋奇遇 [600点券]",
    )

    private val bgIdKey = ConfigData<Int>("hd_CustomPetHomeBg_bgIdKey")
    private var bgId: Int
        get() = bgIdKey.getOrDefault(0)
        set(value) {
            bgIdKey.value = value
        }

    override val kuiklyName: String
        get() = "pet_home.apk"

    override val uiItemAgent = uiClickableItem {
        title = "自定义宠物首页背景"
        onClickListener = { _, activity, _ ->
            val ids = bgMap.keys.toList()
            AlertDialog.Builder(activity)
                .setTitle("更换背景")
                .setSingleChoiceItems(bgMap.values.toTypedArray(), ids.indexOf(bgId)) { dialog, which ->
                    bgId = ids[which]
                    isEnabled = true
                    if (!isInitialized) initialize()
                    dialog.dismiss()
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    override val uiItemLocation = FunctionEntryRouter.Locations.Entertainment.ENTERTAIN_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_9_3_25)

    override fun startHook(classLoader: ClassLoader): Boolean {
        DexKitBridge.create(classLoader, true).use { bridge ->
            bridge.findMethod {
                matcher {
                    usingStrings("AIFileModule", "UpdateSystemData")
                }
            }.single().getMethodInstance(classLoader)
                .hookAfterIfEnabled(this) { param ->
                    val element = param.args[0] as String // jpg or ktx
                    if (bgId != 0 && element.endsWith(".jpg")) {
                        // normal_bg.jpg -> /data/user/0/com.tencent.mobileqq/files/files/vas_material_folder/pet_353_download/petHomeBackground.28.zip/normal_bg.jpg
                        // TODO 待优化 自定义选取本地资源
                        val path = param.result as String
                        if (!path.contains("petHomeBackground")) return@hookAfterIfEnabled
                        val replacePath = path.replace(Regex("petHomeBackground\\.\\d+\\.zip"), "petHomeBackground.${bgId}.zip")
                        if (!File(replacePath).exists()) throw RuntimeException("本地不存在 '${bgMap[bgId]}' 背景资源")
                        param.result = replacePath
                    }
                }
        }
        return true
    }
}
