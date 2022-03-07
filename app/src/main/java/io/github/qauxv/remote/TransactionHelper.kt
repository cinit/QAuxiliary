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

package io.github.qauxv.remote

import io.github.qauxv.util.LicenseStatus
import io.github.qauxv.util.Log
import io.github.qauxv.util.data.UserStatusConst
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object TransactionHelper {

    const val apiAddress = "https://api.qwq2333.top"

    @Throws(IOException::class)
    private fun convertInputStreamToString(inputStream: InputStream): String? {
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        var line: String? = ""
        var result: String? = ""
        while (bufferedReader.readLine().also { line = it } != null) {
            result += line
        }
        inputStream.close()
        return result
    }

    @JvmStatic
    fun getUserStatus(uin: Long): Int {
        return try {
            val url = URL("$apiAddress/user/query")
            val conn = url.openConnection() as HttpsURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; utf-8")
            conn.setRequestProperty("Accept", "application/json")
            val os = DataOutputStream(conn.outputStream)
            os.writeBytes("{\"uin\":$uin}")
            os.flush()
            os.close()
            val resp = JSONObject(convertInputStreamToString(conn.inputStream)!!)
            if (resp.getInt("code") == 200) {
                resp.getInt("status")
            } else {
                UserStatusConst.notExist
            }
        } catch (e: Exception) {
            Log.e(e)
            UserStatusConst.notExist
        }
    }

    /**
     * 记录发送卡片信息
     *
     * @param uin 发送者qq号
     * @param msg 卡片内容
     * @return 是否上报成功 成功返回null 失败返回理由 成功返回null 失败返回理由
     */
    @JvmStatic
    fun postCardMsg(uin: Long, msg: String): String? {
        try {
            if (LicenseStatus.isWhitelisted()) {
                return null
            }
            val url = URL("$apiAddress/statistics/card/send")
            val conn = url.openConnection() as HttpsURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; utf-8")
            conn.setRequestProperty("Accept", "application/json")
            val os = DataOutputStream(conn.outputStream)
            val request = JSONObject()
            request.put("uin", uin)
            request.put("msg", msg)
            os.writeBytes(request.toString())
            os.flush()
            os.close()
            val resp = JSONObject(convertInputStreamToString(conn.inputStream)!!)
            if (resp.getInt("code") == 200) {
                return null
            } else {
                return resp.getString("reason")
            }
        } catch (e: Exception) {
            Log.e(e)
            return "server is offline"
        }
    }
}
