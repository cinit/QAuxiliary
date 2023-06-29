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

package cc.hicore.message.chat;

import android.text.TextUtils;
import cc.hicore.Env;
import cc.hicore.QApp.QAppUtils;
import cc.hicore.ReflectUtil.MField;
import cc.hicore.Utils.XLog;
import com.tencent.qqnt.kernel.nativeinterface.Contact;
import io.github.qauxv.bridge.SessionInfoImpl;
import io.github.qauxv.util.Initiator;

public class SessionUtils {
    public static CommonChat AIOParam2CommonChat(Object AIOParam){
        try {
            if (QAppUtils.isQQnt()){
                CommonChat chat = new CommonChat();
                Contact contact = AIOParam2Contact(AIOParam);
                if (contact.getChatType() == 1){
                    chat.type = 0;
                }else if (contact.getChatType() == 2){
                    chat.type = 1;
                }
                //TODO decode private session
                chat.contact = contact;
                return chat;
            }else {
                return null;
            }
        }catch (Exception e){
            XLog.e("SessionUtils.getCurrentSession",e);
            return null;
        }

    }
    public static Contact buildContact(CommonChat chat){
        Contact contact = new Contact();
        if (chat.contact == null){
            if (chat.type == 0) contact.setChatType(2);
            else if (chat.type == 1) contact.setChatType(1);
            else if (chat.type == 2) contact.setChatType(100);
            if (TextUtils.isEmpty(chat.uid)){
                if (chat.type == 0)contact.setPeerUid(chat.groupUin);
                else if (chat.type == 1)contact.setPeerUid(QAppUtils.UserUinToPeerID(chat.userUin));
                else if (chat.type == 2) throw new RuntimeException("Not support.");
            }else {
                contact.setPeerUid(chat.uid);
            }
        }else {
            contact = chat.contact;
        }
        return contact;
    }
    public static Contact AIOParam2Contact(Object AIOParam) throws Exception {
        Object AIOSession = MField.GetFirstField(AIOParam, Initiator.loadClass("com.tencent.aio.data.AIOSession"));
        Object AIOContact = MField.GetFirstField(AIOSession,Initiator.loadClass("com.tencent.aio.data.AIOContact"));
        Contact contact = new Contact();
        contact.setPeerUid(getCurrentPeerIDByAIOContact(AIOContact));

        int chatType = getCurrentChatTypeByAIOContact(AIOContact);
        contact.setChatType(chatType);

        if (chatType == 4){
            contact.setGuildId(getCurrentGuildIDByAIOContact(AIOContact));
        }
        return contact;
    }
    public static String getCurrentPeerIDByAIOContact(Object AIOContact) throws Exception {
        return MField.GetField(AIOContact,"f",String.class);
    }
    public static int getCurrentChatTypeByAIOContact(Object AIOContact) throws Exception{
        return MField.GetField(AIOContact,"e",int.class);
    }
    public static String getCurrentGuildIDByAIOContact(Object AIOContact) throws Exception{
        return MField.GetField(AIOContact,"g",String.class);
    }
    public static Object buildSession(CommonChat chat){
        if (chat.type == 0){
            return SessionInfoImpl.createSessionInfo(chat.groupUin,1);
        }else if (chat.type == 1){
            return SessionInfoImpl.createSessionInfo(chat.userUin,0);
        }else {
            throw new RuntimeException("Not support type");
        }

    }
}
