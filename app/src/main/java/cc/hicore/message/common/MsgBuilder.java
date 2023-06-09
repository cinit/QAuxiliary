/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2023 QAuxiliary developers
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

package cc.hicore.message.common;

import cc.hicore.QApp.QAppUtils;
import cc.hicore.ReflectUtil.MClass;
import cc.hicore.ReflectUtil.MField;
import cc.hicore.ReflectUtil.MMethod;
import cc.hicore.Utils.DataUtils;
import cc.ioctl.util.HostInfo;
import cc.hicore.Utils.XLog;
import com.tencent.qqnt.kernel.nativeinterface.MsgElement;
import com.tencent.qqnt.kernel.nativeinterface.PttElement;
import com.tencent.qqnt.kernel.nativeinterface.TextElement;
import io.github.qauxv.bridge.AppRuntimeHelper;
import io.github.qauxv.util.QQVersion;
import java.io.File;
import java.lang.reflect.Method;
import java.util.UUID;

public class MsgBuilder {
    public static MsgElement nt_build_text(String text){
        TextElement textElement = new TextElement();
        textElement.setContent(text);

        MsgElement msgElement = new MsgElement();
        msgElement.setElementType(1);
        msgElement.setTextElement(textElement);
        return msgElement;
    }
    public static MsgElement nt_build_voice(String origin,int time){
        PttElement pttElement = new PttElement();
        File f = new File(origin);
        pttElement.setFileName(f.getName());
        pttElement.setFilePath(f.getAbsolutePath());
        pttElement.setMd5HexStr(DataUtils.getFileMD5(f));
        pttElement.setFileSize(f.length());
        pttElement.setDuration(time);
        pttElement.setFormatType(1);
        pttElement.setVoiceType(2);
        pttElement.setVoiceChangeType(0);
        pttElement.setCanConvert2Text(false);
        pttElement.setFileId(0);
        pttElement.setFileUuid("");
        pttElement.setText("");
        MsgElement msgElement = new MsgElement();
        msgElement.setElementType(4);
        msgElement.setPttElement(pttElement);
        return msgElement;
    }
    public static MsgElement nt_build_pic(String path){
        try {
            Object helper = MClass.NewInstance(MClass.loadClass("com.tencent.qqnt.msg.api.impl.MsgUtilApiImpl"));
            return MMethod.CallMethod(helper,"createPicElement",MsgElement.class,new Class[]{String.class,boolean.class,int.class},path,true,0);
        } catch (Exception e) {
            XLog.e("MsgBuilder.nt_build_pic",e);
            throw new RuntimeException(e);
        }
    }
    public static Object build_pic(Object _SessionInfo,String path){
        try {
            Method CallMethod = MMethod.FindMethod("com.tencent.mobileqq.activity.ChatActivityFacade", null, MClass.loadClass("com.tencent.mobileqq.data.ChatMessage"), new Class[]{
                    MClass.loadClass("com.tencent.mobileqq.app.QQAppInterface"),
                    MClass.loadClass("com.tencent.mobileqq.activity.aio.SessionInfo"),
                    String.class
            });
            Object PICMsg = CallMethod.invoke(null,
                    AppRuntimeHelper.getQQAppInterface(), _SessionInfo, path
            );
            MField.SetField(PICMsg, "md5", DataUtils.getFileMD5(new File(path)));
            MField.SetField(PICMsg, "uuid", DataUtils.getFileMD5(new File(path)) + ".jpg");
            MField.SetField(PICMsg, "localUUID", UUID.randomUUID().toString());
            MMethod.CallMethodNoParam(PICMsg, "prewrite", void.class);
            return PICMsg;
        }catch (Exception e){
            XLog.e("MsgBuilder.build_pic", e);
            return null;
        }
    }
    public static Object copy_new_flash_chat(Object source){
        try {
            Method ArkChatObj;
            if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_8_90)){
                ArkChatObj = MMethod.FindMethod("com.tencent.mobileqq.service.h.r", null,
                        MClass.loadClass("com.tencent.mobileqq.data.MessageForArkFlashChat"),
                        new Class[]{
                                MClass.loadClass("com.tencent.mobileqq.app.QQAppInterface"),
                                String.class, String.class, int.class,
                                MClass.loadClass("com.tencent.mobileqq.data.ArkFlashChatMessage")
                        }
                );
            }else {
                ArkChatObj = MMethod.FindMethod("com.tencent.mobileqq.service.message.MessageRecordFactory", null,
                        MClass.loadClass("com.tencent.mobileqq.data.MessageForArkFlashChat"),
                        new Class[]{
                                MClass.loadClass("com.tencent.mobileqq.app.QQAppInterface"),
                                String.class, String.class, int.class,
                                MClass.loadClass("com.tencent.mobileqq.data.ArkFlashChatMessage")
                        }
                );
            }

            Object sArk = MField.GetField(source, "ark_app_message");
            int isTroop = MField.GetField(source, "istroop", int.class);
            String FriendUin = MField.GetField(source, "frienduin", String.class);
            Object NewChat = ArkChatObj.invoke(null, QAppUtils.getAppRuntime(), FriendUin, QAppUtils.getCurrentUin(), isTroop, sArk);
            return NewChat;
        } catch (Exception e) {
            return null;
        }
    }
    public static Object copy_market_face_msg(Object source){
        try {
            Object mMessageRecord = build_common_message_record(-2007);
            MMethod.CallMethod(mMessageRecord, mMessageRecord.getClass().getSuperclass().getSuperclass(), "initInner", void.class,
                    new Class[]{String.class, String.class, String.class, String.class, long.class, int.class, int.class, long.class},
                    QAppUtils.getCurrentUin(), MField.GetField(source, "frienduin"), QAppUtils.getCurrentUin(), "[原创表情]", System.currentTimeMillis() / 1000,
                    -2007,
                    MField.GetField(source, "istroop"), System.currentTimeMillis() / 1000
            );
            MField.SetField(mMessageRecord, "msgData", MField.GetField(source, source.getClass(), "msgData", byte[].class));
            String strName = MField.GetField(source, "sendFaceName");
            if (strName != null) {
                MField.SetField(mMessageRecord, "sendFaceName", strName);
            }
            MMethod.CallMethodNoParam(mMessageRecord, "doParse", void.class);
            return rebuild_message(mMessageRecord);
        }catch (Exception e){
            XLog.e("MsgBuilder.copy_market_face_msg", e);
            return null;
        }
    }
    public static Object copy_poke_msg(Object source){
        try {
            Object PokeEmo = MClass.NewInstance(MClass.loadClass("com.tencent.mobileqq.data.MessageForPokeEmo"));
            MField.SetField(PokeEmo, "msgtype", -5018);
            MField.SetField(PokeEmo, "pokeemoId", 13);
            MField.SetField(PokeEmo, "pokeemoPressCount", MField.GetField(source, "pokeemoPressCount"));
            MField.SetField(PokeEmo, "emoIndex", MField.GetField(source, "emoIndex"));
            MField.SetField(PokeEmo, "summary", MField.GetField(source, "summary"));
            MField.SetField(PokeEmo, "emoString", MField.GetField(source, "emoString"));
            MField.SetField(PokeEmo, "emoCompat", MField.GetField(source, "emoCompat"));
            MMethod.CallMethod(PokeEmo, "initMsg", void.class, new Class[0]);
            String friendInfo = MField.GetField(source, "frienduin", String.class);
            int istroop = MField.GetField(source, "istroop", int.class);
            MMethod.CallStaticMethod(HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_0) ? MClass.loadClass("com.tencent.mobileqq.service.h.r"):
                            MClass.loadClass("com.tencent.mobileqq.service.message.MessageRecordFactory"),

                    null, void.class, new Class[]{MClass.loadClass("com.tencent.common.app.AppInterface"), MClass.loadClass("com.tencent.mobileqq.data.MessageRecord"), String.class, String.class, int.class},
                    QAppUtils.getAppRuntime(), PokeEmo, friendInfo, QAppUtils.getCurrentUin(), istroop);
            return PokeEmo;
        } catch (Exception e) {
            XLog.e("MsgBuilder.copy_poke_msg", e);
            return null;
        }
    }
    public static Object rebuild_message(Object record){
        try{
            if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_0)){
                return MMethod.CallStaticMethod(MClass.loadClass("com.tencent.mobileqq.service.h.r"),null,MClass.loadClass("com.tencent.mobileqq.data.MessageRecord"),record);
            }else {
                return MMethod.CallStaticMethod(MClass.loadClass("com.tencent.mobileqq.service.message.MessageRecordFactory"),null,MClass.loadClass("com.tencent.mobileqq.data.MessageRecord"),record);
            }

        }catch (Exception e){
            return null;
        }
    }
    public static Object build_common_message_record(int type){
        try{
            Method CallMethod = null;
            if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_0)){
                CallMethod = MMethod.FindMethod("com.tencent.mobileqq.service.h.r","d", MClass.loadClass("com.tencent.mobileqq.data.MessageRecord"),new Class[]{
                        int.class
                });
            } else if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_8_93)){
                CallMethod = MMethod.FindMethod("com.tencent.mobileqq.service.message.MessageRecordFactory","d", MClass.loadClass("com.tencent.mobileqq.data.MessageRecord"),new Class[]{
                        int.class
                });
            }else {
                CallMethod = MMethod.FindMethod("com.tencent.mobileqq.service.message.MessageRecordFactory","a", MClass.loadClass("com.tencent.mobileqq.data.MessageRecord"),new Class[]{
                        int.class
                });
            }
            return CallMethod.invoke(null,type);
        }catch (Exception e){
            return null;
        }
    }
}
