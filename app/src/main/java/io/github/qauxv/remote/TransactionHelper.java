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
package io.github.qauxv.remote;

import io.github.qauxv.util.Log;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;

public class TransactionHelper {

    private TransactionHelper() {
    }

    private static final String apiAddress = "https://api.qwq2333.top";

    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while ((line = bufferedReader.readLine()) != null) {
            result += line;
        }
        inputStream.close();
        return result;
    }

    public static int getUserStatus(long uin) {
        try {
            URL url = new URL(apiAddress + "/user/query");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            DataOutputStream os = new DataOutputStream(conn.getOutputStream());
            os.writeBytes("{\"uin\":" + uin + "}");

            os.flush();
            os.close();

            JSONObject resp = new JSONObject(convertInputStreamToString(conn.getInputStream()));
            if (resp.getInt("code") == 200) {
                return resp.getInt("status");
            } else {
                return -1;
            }
        } catch (Exception e) {
            Log.e(e);
            return -1;
        }
    }

    /**
     * 记录发送卡片信息
     *
     * @param uin 发送者qq号
     * @param msg 卡片内容
     * @return 是否上报成功 成功返回null 失败返回理由 成功返回null 失败返回理由
     */
    public static String postCardMsg(long uin, String msg) {
        try {
            URL url = new URL(apiAddress + "/statistics/card/send");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            DataOutputStream os = new DataOutputStream(conn.getOutputStream());

            JSONObject request = new JSONObject();
            request.put("uin", uin);
            request.put("msg", msg);

            os.writeBytes(request.toString());

            os.flush();
            os.close();

            JSONObject resp = new JSONObject(convertInputStreamToString(conn.getInputStream()));
            if (resp.getInt("code") == 200) {
                return null;
            } else {
                return resp.getString("reason");
            }
        } catch (Exception e) {
            Log.e(e);
            return "server is offline";
        }
    }

    /**
     * 记录群发文本
     *
     * @param uin   发送者qq号
     * @param msg   群发文本
     * @param count 数量
     * @return 是否上报成功 成功返回null 失败返回理由 失败有可能是服务端出现问题 也有可能是服务端认为非法
     */
    public static String postBatchMsg(long uin, String msg, int count) {
        try {
            if (msg == null) {
                msg = "msg is null";
            }
            URL url = new URL(apiAddress + "/statistics/batch");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            DataOutputStream os = new DataOutputStream(conn.getOutputStream());

            JSONObject request = new JSONObject();
            request.put("uin", uin);
            request.put("msg", msg);
            request.put("count", count);

            os.writeBytes(request.toString());

            os.flush();
            os.close();

            JSONObject resp = new JSONObject(convertInputStreamToString(conn.getInputStream()));
            if (resp.getInt("code") == 200) {
                return null;
            } else {
                return resp.getString("reason");
            }
        } catch (Exception e) {
            Log.e(e);
            return "server is offline";
        }
    }
}
