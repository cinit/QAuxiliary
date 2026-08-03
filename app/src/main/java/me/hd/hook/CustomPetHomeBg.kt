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
import io.github.qauxv.util.QQVersion
import io.github.qauxv.util.dexkit.Hd_CustomPetHomeBg_Method
import io.github.qauxv.util.requireMinQQVersion
import me.hd.base.KuiklyDelayableHook
import me.hd.util.hookAfterIfEnabled
import me.hd.util.name
import me.hd.util.parameters
import me.hd.util.returnType
import me.hd.util.singleMethod
import me.hd.util.toClass
import me.ketal.data.ConfigData

@FunctionHookEntry
@UiItemAgentEntry
object CustomPetHomeBg : KuiklyDelayableHook(
    targets = arrayOf(Hd_CustomPetHomeBg_Method)
) {
    private val bgMap = mapOf(
        "黄色爪爪[免费]" to 18,
        "蓝色雪花[免费]" to 19,
        "薄荷清新[600点券]" to 23,
        "沉稳灰蓝[1w金币]" to 24,
        "暖阳午后[会员免费]" to 25,
        "绿色童话[会员免费]" to 28,
        "蓝色童话[1w金币]" to 29,
        "粉色童话[600点券]" to 30,
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
            AlertDialog.Builder(activity)
                .setTitle("更换背景")
                .setItems(bgMap.keys.toTypedArray()) { dialog, which ->
                    val key = bgMap.keys.elementAt(which)
                    val value = bgMap[key] ?: 0
                    if (value != 0) {
                        bgId = value
                    }
                }
                .show()
        }
    }

    override val uiItemLocation = FunctionEntryRouter.Locations.Entertainment.ENTERTAIN_CATEGORY
    override val isAvailable = requireMinQQVersion(QQVersion.QQ_9_3_25)

    override fun startHook(classLoader: ClassLoader): Boolean {
        // val getElementMethod = DexKit.loadMethodFromCache(Hd_CustomPetHomeBg_Method)
        // TODO 待优化 非 DexKitBridge.create(hostInfo.application.applicationInfo.sourceDir) 搜索
        "y6.e".toClass(classLoader).singleMethod {
            returnType(String::class.java)
                && name("a")
                && parameters(String::class.java)
        }.hookAfterIfEnabled(this) { param ->
            // normal_bg.jpg -> /data/user/0/com.tencent.mobileqq/files/files/vas_material_folder/pet_353_download/petHomeBackground.28.zip/normal_bg.jpg
            val element = param.args[0] as String
            val path = param.result as String
            if (path.contains("petHomeBackground") && path.endsWith(".jpg") && bgId != 0) {
                // TODO 待优化 自定义选取本地图片
                param.result = path.replace(Regex("petHomeBackground\\.\\d+\\.zip"), "petHomeBackground.${bgId}.zip")
            }
        }
        return true
    }
}
