package com.hicore.messageUtils;

import cc.ioctl.util.HostInfo;
import com.hicore.ReflectUtil.MClass;
import com.hicore.ReflectUtil.MField;
import com.hicore.ReflectUtil.MMethod;
import io.github.qauxv.util.QQVersion;
import java.lang.reflect.Method;

public class QQMsgBuilder {

    public static Object Copy_NewFlashChat(Object SourceChat) {
        try {
            Method ArkChatObj = MMethod.FindMethod("com.tencent.mobileqq.service.message.MessageRecordFactory", null,
                    MClass.loadClass("com.tencent.mobileqq.data.MessageForArkFlashChat"),
                    new Class[]{
                            MClass.loadClass("com.tencent.mobileqq.app.QQAppInterface"),
                            String.class, String.class, int.class,
                            MClass.loadClass("com.tencent.mobileqq.data.ArkFlashChatMessage")
                    }
            );
            Object sArk = MField.GetField(SourceChat, "ark_app_message");
            int isTroop = MField.GetField(SourceChat, "istroop", int.class);
            String FriendUin = MField.GetField(SourceChat, "frienduin", String.class);
            Object NewChat = ArkChatObj.invoke(null, QQEnvUtils.getAppRuntime(), FriendUin, QQEnvUtils.getCurrentUin(), isTroop, sArk);
            return NewChat;
        } catch (Exception e) {
            return null;
        }
    }

    public static Object CopyToTYMessage(Object SourceObj) throws Exception {
        Object mMessageRecord = build_common_message_record(-7001);
        MMethod.CallMethod(mMessageRecord, mMessageRecord.getClass().getSuperclass().getSuperclass(), "initInner", void.class,
                new Class[]{String.class, String.class, String.class, String.class, long.class, int.class, int.class, long.class},
                QQEnvUtils.getCurrentUin(), MField.GetField(SourceObj, "frienduin"), QQEnvUtils.getCurrentUin(), "[涂鸦]", System.currentTimeMillis() / 1000,
                -7001,
                MField.GetField(SourceObj, "istroop"), System.currentTimeMillis() / 1000
        );

        MField.SetField(mMessageRecord, "combineFileUrl", MField.GetField(SourceObj, SourceObj.getClass(), "combineFileUrl", String.class));
        MField.SetField(mMessageRecord, "combineFileMd5", MField.GetField(SourceObj, SourceObj.getClass(), "combineFileMd5", String.class));

        MField.SetField(mMessageRecord, "gifId", MField.GetField(SourceObj, SourceObj.getClass(), "gifId", int.class));

        MField.SetField(mMessageRecord, "offSet", MField.GetField(SourceObj, SourceObj.getClass(), "offSet", int.class));
        MField.SetField(mMessageRecord, "fileUploadStatus", MField.GetField(SourceObj, SourceObj.getClass(), "fileUploadStatus", int.class));
        MField.SetField(mMessageRecord, "fileDownloadStatus", MField.GetField(SourceObj, SourceObj.getClass(), "fileDownloadStatus", int.class));
        String mPath = MField.GetField(SourceObj, "localFildPath");
        MField.SetField(mMessageRecord, "localFildPath", mPath);
        MField.SetField(mMessageRecord, "extStr", MField.GetField(SourceObj, SourceObj.getClass(), "extStr", String.class));
        MField.SetField(mMessageRecord, "msg", "[涂鸦]");
        MMethod.CallMethodNoParam(mMessageRecord, "prewrite", void.class);
        MMethod.CallMethodNoParam(mMessageRecord, "parse", void.class);
        return rebuild_message(mMessageRecord);
    }

    public static Object CopyToMacketFaceMessage(Object SourceObj) throws Exception {
        Object mMessageRecord = build_common_message_record(-2007);
        MMethod.CallMethod(mMessageRecord, mMessageRecord.getClass().getSuperclass().getSuperclass(), "initInner", void.class,
                new Class[]{String.class, String.class, String.class, String.class, long.class, int.class, int.class, long.class},
                QQEnvUtils.getCurrentUin(), MField.GetField(SourceObj, "frienduin"), QQEnvUtils.getCurrentUin(), "[原创表情]", System.currentTimeMillis() / 1000,
                -2007,
                MField.GetField(SourceObj, "istroop"), System.currentTimeMillis() / 1000
        );
        MField.SetField(mMessageRecord, "msgData", MField.GetField(SourceObj, SourceObj.getClass(), "msgData", byte[].class));
        String strName = MField.GetField(SourceObj, "sendFaceName");
        if (strName != null) {
            MField.SetField(mMessageRecord, "sendFaceName", strName);
        }
        MMethod.CallMethodNoParam(mMessageRecord, "doParse", void.class);
        return rebuild_message(mMessageRecord);
    }

    public static Object Copy_PokeMsg(Object raw) {
        try {
            Object PokeEmo = MClass.NewInstance(MClass.loadClass("com.tencent.mobileqq.data.MessageForPokeEmo"));
            MField.SetField(PokeEmo, "msgtype", -5018);
            MField.SetField(PokeEmo, "pokeemoId", 13);
            MField.SetField(PokeEmo, "pokeemoPressCount", MField.GetField(raw, "pokeemoPressCount"));
            MField.SetField(PokeEmo, "emoIndex", MField.GetField(raw, "emoIndex"));
            MField.SetField(PokeEmo, "summary", MField.GetField(raw, "summary"));
            MField.SetField(PokeEmo, "emoString", MField.GetField(raw, "emoString"));
            MField.SetField(PokeEmo, "emoCompat", MField.GetField(raw, "emoCompat"));
            MMethod.CallMethod(PokeEmo, "initMsg", void.class, new Class[0]);
            String friendInfo = MField.GetField(raw, "frienduin", String.class);
            int istroop = MField.GetField(raw, "istroop", int.class);
            MMethod.CallStaticMethod(MClass.loadClass("com.tencent.mobileqq.service.message.MessageRecordFactory"),
                    null, void.class, new Class[]{MClass.loadClass("com.tencent.common.app.AppInterface"), MClass.loadClass("com.tencent.mobileqq.data.MessageRecord"), String.class, String.class, int.class},
                    QQEnvUtils.getAppRuntime(), PokeEmo, friendInfo, QQEnvUtils.getCurrentUin(), istroop);
            return PokeEmo;
        } catch (Exception e) {
            return null;
        }

    }
    public static Object build_common_message_record(int type){
        try{
            if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_8_93)){
                Method CallMethod = MMethod.FindMethod("com.tencent.mobileqq.service.message.MessageRecordFactory","d", MClass.loadClass("com.tencent.mobileqq.data.MessageRecord"),new Class[]{
                        int.class
                });
                return CallMethod.invoke(null,type);
            }else {
                Method CallMethod = MMethod.FindMethod("com.tencent.mobileqq.service.message.MessageRecordFactory","a", MClass.loadClass("com.tencent.mobileqq.data.MessageRecord"),new Class[]{
                        int.class
                });
                return CallMethod.invoke(null,type);
            }
        }catch (Exception e){
            return null;
        }
    }
    public static Object rebuild_message(Object record){
        try{
            return MMethod.CallStaticMethod(MClass.loadClass("com.tencent.mobileqq.service.message.MessageRecordFactory"),null,MClass.loadClass("com.tencent.mobileqq.data.MessageRecord"),record);
        }catch (Exception e){
            return null;
        }
    }
}
