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
import com.tencent.qqnt.kernel.nativeinterface.Contact;
import com.tencent.qqnt.kernel.nativeinterface.MsgElement;
import java.util.ArrayList;

public class MsgSender {
    public static void send_pic_by_contact(Contact contact,String picPath){
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
