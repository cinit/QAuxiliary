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

package cc.hicore.hook;

import cc.hicore.ReflectUtil.MField;
import cc.hicore.ReflectUtil.MMethod;
import cc.hicore.ReflectUtil.XField;
import cc.hicore.ReflectUtil.XMethod;
import cc.hicore.message.bridge.Chat_facade_bridge;
import cc.hicore.message.common.MsgBuilder;
import cc.hicore.message.common.MsgUtils;
import io.github.qauxv.util.Initiator;
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
                ArrayList AtList1 = XField.obj(chatMsg).name("atInfoTempList").type(ArrayList.class).get();
                ArrayList AtList2 = XField.obj(chatMsg).name("atInfoList").type(ArrayList.class).get();
                String mStr = XField.obj(chatMsg).name("extStr").type(String.class).get();
                JSONObject mJson = new JSONObject(mStr);
                mStr = mJson.optString("troop_at_info_list");
                ArrayList AtList3 = XMethod.clz("com.tencent.mobileqq.data.MessageForText").name("getTroopMemberInfoFromExtrJson").ret(ArrayList.class).param(String.class).invoke(mStr);
                if (AtList1 == null) {
                    AtList1 = AtList2;
                }
                if (AtList1 == null) {
                    AtList1 = AtList3;
                }
                String nowMsg = XField.obj(chatMsg).name("msg").type(String.class).get();
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
                Chat_facade_bridge.sendArkApp(Session,XField.obj(chatMsg).name("ark_app_message").get());
                break;
            }
            case "MessageForStructing":
            case "MessageForTroopPobing": {
                Chat_facade_bridge.sendStruct(Session, XField.obj(chatMsg).name("structingMsg").get());
                break;
            }
            case "MessageForAniSticker": {
                int servID = MsgUtils.DecodeAntEmoCode(XField.obj(chatMsg).name("sevrId").type(int.class).get());
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
