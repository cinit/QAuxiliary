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

package com.hicore.message.common;

import android.text.TextUtils;
import com.hicore.QApp.QAppUtils;
import com.hicore.message.bridge.Chat_facade_bridge;
import com.hicore.message.bridge.Nt_kernel_bridge;
import com.hicore.message.chat.CommonChat;
import com.hicore.message.chat.SessionBuilder;
import com.tencent.qqnt.kernel.nativeinterface.Contact;
import com.tencent.qqnt.kernel.nativeinterface.MsgElement;
import java.util.ArrayList;

public class MsgSender {
    public static void send_text(CommonChat chat,String text){
        if (QAppUtils.isQQnt()){
            Contact contact = new Contact();
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

            ArrayList<MsgElement> newMsgArr = new ArrayList<>();
            newMsgArr.add(MsgBuilder.nt_build_text(text));
            Nt_kernel_bridge.send_msg(contact,newMsgArr);
        }else {
            Chat_facade_bridge.sendText(SessionBuilder.buildSession(chat),text,new ArrayList<>());
        }
    }
    public static void send_pic(CommonChat chat,String pic){

    }
    public static void send_voice(CommonChat chat,String voicePath){

    }
    public static void send_reply(CommonChat chat,Object source){

    }
}
