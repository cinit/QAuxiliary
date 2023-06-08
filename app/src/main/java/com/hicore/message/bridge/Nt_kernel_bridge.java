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

package com.hicore.message.bridge;

import com.hicore.QApp.QAppUtils;
import com.tencent.qqnt.kernel.nativeinterface.Contact;
import com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService;
import com.tencent.qqnt.kernel.nativeinterface.MsgAttributeInfo;
import com.tencent.qqnt.kernel.nativeinterface.MsgElement;
import com.tencent.qqnt.kernel.nativeinterface.VASMsgAvatarPendant;
import com.tencent.qqnt.kernel.nativeinterface.VASMsgBubble;
import com.tencent.qqnt.kernel.nativeinterface.VASMsgElement;
import com.tencent.qqnt.kernel.nativeinterface.VASMsgFont;
import com.tencent.qqnt.kernel.nativeinterface.VASMsgIceBreak;
import com.tencent.qqnt.kernel.nativeinterface.VASMsgNamePlate;
import java.util.ArrayList;
import java.util.HashMap;

public class Nt_kernel_bridge {
    private static IKernelMsgService kernel_service;
    private static void checkKernelMsgService(){
        assert kernel_service != null;
    }
    public static void send_msg(Contact contact, ArrayList<MsgElement> elements){
        checkKernelMsgService();
        HashMap<Integer, MsgAttributeInfo> attrMap = new HashMap<>();
        attrMap.put(0,getDefaultAttributeInfo());

        kernel_service.sendMsg(kernel_service.getMsgUniqueId(QAppUtils.getServiceTime()), contact, elements, attrMap, (i2, str) -> {

        });
    }
    public static MsgAttributeInfo getDefaultAttributeInfo(){

        VASMsgNamePlate plate = new VASMsgNamePlate(258,64,0,0,0,0,258,0,new ArrayList<>(),0,0);
        VASMsgBubble bubble = new VASMsgBubble(0,0,0,0);
        VASMsgFont font = new VASMsgFont(65536,0L,0,0,2000);
        VASMsgAvatarPendant pendant = new VASMsgAvatarPendant();
        VASMsgIceBreak iceBreak = new VASMsgIceBreak(null,null);

        VASMsgElement element = new VASMsgElement(plate,bubble,pendant,font,iceBreak);


        return new MsgAttributeInfo(0,0,element,null,null,null,null,null,null,null);
    }
}
