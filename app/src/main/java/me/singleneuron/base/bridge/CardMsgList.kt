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
package me.singleneuron.base.bridge

import io.github.qauxv.SyncUtils
import io.github.qauxv.config.ConfigManager
import io.github.qauxv.util.LicenseStatus
import io.github.qauxv.util.encodeToJson
import java.net.URL

const val apiAddress = "https://2fa.qwq2333.top/card/BlackList"
const val cacheKey = "cardRuleCache"
const val lastUpdateTimeKey = "cardRuleCacheLastUpdateTime"

abstract class CardMsgList {

    companion object {

        @JvmStatic
        fun getInstance(): () -> String {
            //Todo
            return ::getBlackList
        }

    }
}

fun getBuiltInRule(): String {
    val map = mapOf(
        "禁止引流" to """(jq\.qq\.com)|(mqqapi.*?forward)""",
        "禁止发送回执消息" to "viewReceiptMessage",
        "禁止干扰性卡片" to """com\.tencent\.mobileqq\.reading""",
        "禁止干扰性消息" to """serviceID[\s]*?=[\s]*?('|")(13|60|76|83)('|")""",
        "禁止音视频通话" to """ti\.qq\.com""",
        "禁止自动回复类卡片" to """com\.tencent\.autoreply"""
    )
    return map.encodeToJson()
}

fun getBlackList(): String {
    if (LicenseStatus.isWhitelisted())
        return "{}"
    val cfg = ConfigManager.getDefaultConfig()
    val cache: String = cfg.getStringOrDefault(cacheKey, "null")
    if (cache != "null") {
        val lastUpdateTime = cfg.getLongOrDefault(lastUpdateTimeKey, System.currentTimeMillis())
        if (lastUpdateTime >= lastUpdateTime + 360 * 60 * 1000) {
            SyncUtils.async {
                val onlineRule = URL(apiAddress).readText()
                cfg.putString(cacheKey, onlineRule)
                cfg.putLong(lastUpdateTimeKey, System.currentTimeMillis())
                cfg.save()
            }
        }
        return cache
    } else {
        SyncUtils.async {
            val onlineRule = URL(apiAddress).readText()
            cfg.putString(cacheKey, onlineRule)
            cfg.putLong(lastUpdateTimeKey, System.currentTimeMillis())
            cfg.save()
        }
        return getBuiltInRule()
    }
}
