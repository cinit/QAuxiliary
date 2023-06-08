package com.hicore.hook;

import com.hicore.ReflectUtil.MClass;
import com.hicore.ReflectUtil.MField;
import com.hicore.ReflectUtil.MMethod;
import com.hicore.message.common.MsgBuilder;
import com.hicore.message.common.MsgUtils;
import com.hicore.message.bridge.Chat_facade_bridge;
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
                Chat_facade_bridge.sendText(Session, nowMsg, AtList1);
                break;
            }
            case "MessageForPic": {
                Chat_facade_bridge.sendPic(Session, chatMsg);
                break;
            }
            case "MessageForPtt": {
                String pttPath = MMethod.CallMethodNoParam(chatMsg, "getLocalFilePath", String.class);
                Chat_facade_bridge.sendVoice(Session, pttPath);
                break;
            }
            case "MessageForMixedMsg": {
                Chat_facade_bridge.sendMix(Session, chatMsg);
                break;
            }
            case "MessageForReplyText": {
                Chat_facade_bridge.sendReply(Session, chatMsg);
                break;
            }
            case "MessageForMarketFace": {
                Chat_facade_bridge.AddAndSendMsg(MsgBuilder.copy_market_face_msg(chatMsg));
                break;
            }
            case "MessageForArkApp": {
                Chat_facade_bridge.sendArkApp(Session, MField.GetField(chatMsg, "ark_app_message"));
                break;
            }
            case "MessageForStructing":
            case "MessageForTroopPobing": {
                Chat_facade_bridge.sendStruct(Session, MField.GetField(chatMsg, "structingMsg"));
                break;
            }
            case "MessageForAniSticker": {
                int servID = MsgUtils.DecodeAntEmoCode(MField.GetField(chatMsg, "sevrId", int.class));
                Chat_facade_bridge.sendAnimation(Session, servID);
                break;
            }
            case "MessageForArkFlashChat": {
                Chat_facade_bridge.AddAndSendMsg(MsgBuilder.copy_new_flash_chat(chatMsg));
                break;
            }
            case "MessageForShortVideo": {
                Chat_facade_bridge.QQ_Forward_ShortVideo(Session, chatMsg);
                break;
            }
            case "MessageForPokeEmo": {
                Chat_facade_bridge.AddAndSendMsg(MsgBuilder.copy_poke_msg(chatMsg));
                break;
            }
            default: {
                Toasts.error(null, "不支持的消息类型:" + chatMsg.getClass().getSimpleName());
            }
        }
    }
}
