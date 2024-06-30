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

import cc.hicore.QApp.QAppUtils;
import cc.hicore.message.bridge.Chat_facade_bridge;
import cc.hicore.message.bridge.Nt_kernel_bridge;
import cc.hicore.message.chat.SessionUtils;
import com.tencent.qqnt.kernel.nativeinterface.MsgElement;
import io.github.qauxv.bridge.kernelcompat.ContactCompat;
import java.util.ArrayList;

public class MsgSender {
    public static void send_pic_by_contact(ContactCompat contact,String picPath){
        if (QAppUtils.isQQnt()){
            ArrayList<MsgElement> newMsgArr = new ArrayList<>();
            if (contact.getChatType() == 4){
                newMsgArr.add(MsgBuilder.nt_build_pic_guild(picPath));
            }else {
                newMsgArr.add(MsgBuilder.nt_build_pic(picPath));
            }

            Nt_kernel_bridge.send_msg(contact,newMsgArr);
        }else {
            Chat_facade_bridge.sendPic(contact,picPath);
        }
    }
}
