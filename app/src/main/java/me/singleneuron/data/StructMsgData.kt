/*
 * QNotified - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 dmca@ioctl.cc
 * https://github.com/ferredoxin/QNotified
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/ferredoxin/QNotified/blob/master/LICENSE.md>.
 */

package me.singleneuron.data

import com.google.gson.Gson
import io.github.qauxv.util.Log
import org.json.JSONObject

data class StructMsgData(
    var prompt: String,
    @Transient var news: StructMsgNewsData,
    var config: StructMsgConfigData,
    var extra: String
) {

    val app: String = "com.tencent.structmsg"
    val desc: String = "新闻"
    val view: String = "news"
    val ver: String = "0.0.0.1"
    val meta: Map<String, StructMsgNewsData> = mapOf(
        "news" to news
    )

    init {
        try {
            val appid = JSONObject(extra).optLong("appid", -1)
            news.appid = if (appid == -1L) null else appid.toString()
        } catch (e: Exception) {
            Log.e(e)
        }
    }

    companion object {
        @JvmStatic
        fun fromMiniApp(miniApp: MiniAppArkData): StructMsgData {
            val detail_1 = miniApp.meta["detail_1"]!!
            return StructMsgData(
                miniApp.prompt,
                StructMsgNewsData(
                    detail_1.desc,
                    detail_1.qqdocurl,
                    detail_1.preview,
                    detail_1.title,
                    detail_1.title
                ),
                StructMsgConfigData(
                    miniApp.config.ctime
                ),
                miniApp.extra
            )
        }
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }

}

data class StructMsgNewsData(
    var desc: String,
    var jumpUrl: String,
    var preview: String,
    var tag: String,
    var title: String
) {

    val app_type: Int = 1
    var appid: String? = null

}

data class StructMsgConfigData(
    var ctime: Long,

    ) {

    val autosize: Boolean = true
    val forward: Boolean = true
    val type: String = "normal"

}
