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
import cc.hicore.message.bridge.Chat_facade_bridge;
import cc.hicore.message.bridge.Nt_kernel_bridge;
import cc.hicore.message.chat.SessionUtils;
import cc.hicore.message.chat.CommonChat;
import com.tencent.qqnt.kernel.nativeinterface.MsgElement;
import java.util.ArrayList;

public class MsgSender {
    public static void send_text(CommonChat chat,String text){
        if (QAppUtils.isQQnt()){

            ArrayList<MsgElement> newMsgArr = new ArrayList<>();
            newMsgArr.add(MsgBuilder.nt_build_text(text));
            Nt_kernel_bridge.send_msg(SessionUtils.buildContact(chat),newMsgArr);
        }else {
            Chat_facade_bridge.sendText(SessionUtils.buildSession(chat),text,new ArrayList<>());
        }
    }
    public static void send_pic(CommonChat chat,String picPath){
        if (QAppUtils.isQQnt()){
            ArrayList<MsgElement> newMsgArr = new ArrayList<>();
            newMsgArr.add(MsgBuilder.nt_build_pic(picPath));
            Nt_kernel_bridge.send_msg(SessionUtils.buildContact(chat),newMsgArr);
        }else {
            Chat_facade_bridge.sendPic(SessionUtils.buildSession(chat),picPath);
        }
    }
    public static void send_voice(CommonChat chat,String voicePath){

    }
    public static void send_reply(CommonChat chat,Object source){

    }
}
