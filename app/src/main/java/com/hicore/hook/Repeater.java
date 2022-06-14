package com.hicore.hook;

import com.hicore.ReflectUtil.MClass;
import com.hicore.ReflectUtil.MField;
import com.hicore.ReflectUtil.MMethod;
import com.hicore.messageUtils.QQMessageUtils;
import com.hicore.messageUtils.QQMsgBuilder;
import com.hicore.messageUtils.QQMsgSender;
import io.github.qauxv.util.Toasts;
import java.util.ArrayList;
import org.json.JSONObject;

public class Repeater {

    public static void Repeat(Object Session, Object chatMsg) throws Exception {
        String Name = chatMsg.getClass().getSimpleName();
        switch (Name) {
            case "MessageForText":
            case "MessageForLongTextMsg":
            case "MessageForFoldMsg": {
                ArrayList AtList1 = MField.GetField(chatMsg, "atInfoTempList", ArrayList.class);
                ArrayList AtList2 = MField.GetField(chatMsg, "atInfoList", ArrayList.class);
                String mStr = MField.GetField(chatMsg, "extStr", String.class);
                JSONObject mJson = new JSONObject(mStr);
                mStr = mJson.optString("troop_at_info_list");
                ArrayList AtList3 = MMethod
                        .CallMethod(null, MClass.loadClass("com.tencent.mobileqq.data.MessageForText"), "getTroopMemberInfoFromExtrJson", ArrayList.class,
                                new Class[]{String.class}, mStr);
                if (AtList1 == null) {
                    AtList1 = AtList2;
                }
                if (AtList1 == null) {
                    AtList1 = AtList3;
                }
                String nowMsg = MField.GetField(chatMsg, "msg", String.class);
                QQMsgSender.sendText(Session, nowMsg, AtList1);
                break;
            }
            case "MessageForPic": {
                QQMsgSender.sendPic(Session, chatMsg);
                break;
            }
            case "MessageForPtt": {
                String pttPath = MMethod.CallMethodNoParam(chatMsg, "getLocalFilePath", String.class);
                QQMsgSender.sendVoice(Session, pttPath);
                break;
            }
            case "MessageForMixedMsg": {
                QQMsgSender.sendMix(Session, chatMsg);
                break;
            }
            case "MessageForReplyText": {
                QQMsgSender.sendReply(Session, chatMsg);
                break;
            }
            case "MessageForScribble": {
                QQMessageUtils.AddAndSendMsg(QQMsgBuilder.CopyToTYMessage(chatMsg));
                break;
            }
            case "MessageForMarketFace": {
                QQMessageUtils.AddAndSendMsg(QQMsgBuilder.CopyToMacketFaceMessage(chatMsg));
                break;
            }
            case "MessageForArkApp": {
                QQMsgSender.sendArkApp(Session, MField.GetField(chatMsg, "ark_app_message"));
                break;
            }
            case "MessageForStructing":
            case "MessageForTroopPobing": {
                QQMsgSender.sendStruct(Session, MField.GetField(chatMsg, "structingMsg"));
                break;
            }
            case "MessageForAniSticker": {
                int servID = QQMessageUtils.DecodeAntEmoCode(MField.GetField(chatMsg, "sevrId", int.class));
                QQMsgSender.sendAnimation(Session, servID);
                break;
            }
            case "MessageForArkFlashChat": {
                QQMessageUtils.AddAndSendMsg(QQMsgBuilder.Copy_NewFlashChat(chatMsg));
                break;
            }
            case "MessageForShortVideo": {
                QQMsgSender.QQ_Forward_ShortVideo(Session, chatMsg);
                break;
            }
            case "MessageForPokeEmo": {
                QQMessageUtils.AddAndSendMsg(QQMsgBuilder.Copy_PokeMsg(chatMsg));
                break;
            }
            default: {
                Toasts.error(null, "不支持的消息类型:" + chatMsg.getClass().getSimpleName());
            }
        }
    }
}
