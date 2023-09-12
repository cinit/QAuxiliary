/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the qwq233 Universal License
 * as published on https://github.com/qwq233/license; either
 * version 2 of the License, or any later version and our EULA as published
 * by QAuxiliary contributors.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the qwq233 Universal License for more details.
 *
 * See
 * <https://github.com/qwq233/license>
 * <https://github.com/cinit/QAuxiliary/blob/master/LICENSE.md>.
 */

package cc.hicore.message.common;

import cc.hicore.Utils.FileUtils;
import cc.ioctl.util.HostInfo;
import org.json.JSONArray;
import org.json.JSONObject;

public class MsgUtils {
    public static int DecodeAntEmoCode(int EmoCode) {
        try {
            String s = FileUtils.readFileString(HostInfo.getApplication().getFilesDir() + "/qq_emoticon_res/face_config.json");
            JSONObject j = new JSONObject(s);
            JSONArray arr = j.getJSONArray("sysface");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (obj.has("AniStickerType")) {
                    if (obj.optString("QSid").equals(EmoCode + "")) {
                        String sId = obj.getString("AQLid");
                        return (Integer.parseInt(sId));
                    }
                }
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
