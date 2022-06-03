package com.hicore.messageUtils;

import cc.ioctl.util.HostInfo;
import com.hicore.ReflectUtil.MClass;
import com.hicore.ReflectUtil.MMethod;
import com.hicore.Utils.FileUtils;
import java.lang.reflect.Method;
import org.json.JSONArray;
import org.json.JSONObject;

public class QQMessageUtils {

    public static void AddAndSendMsg(Object MessageRecord) {
        try {
            Object MessageFacade = MMethod.CallMethodNoParam(QQEnvUtils.getAppRuntime(), "getMessageFacade",
                    MClass.loadClass("com.tencent.imcore.message.QQMessageFacade"));
            Method mMethod = MMethod.FindMethod("com.tencent.imcore.message.BaseQQMessageFacade", null, void.class, new Class[]{
                    MClass.loadClass("com.tencent.mobileqq.data.MessageRecord"),
                    MClass.loadClass("com.tencent.mobileqq.app.BusinessObserver"),boolean.class
            });
            mMethod.invoke(MessageFacade, MessageRecord, null,false);
        } catch (Exception e) {
        }

    }

    public static int DecodeAntEmoCode(int EmoCode) {
        try {
            String s = FileUtils.ReadFileString(HostInfo.getApplication().getFilesDir() + "/qq_emoticon_res/face_config.json");
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
