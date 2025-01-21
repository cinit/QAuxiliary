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

package cc.hicore.message.chat;

import static cc.ioctl.util.HostInfo.requireMinTimVersion;

import cc.hicore.ReflectUtil.XField;
import cc.hicore.Utils.XLog;
import io.github.qauxv.bridge.kernelcompat.ContactCompat;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.TIMVersion;
import java.io.Serializable;

public class SessionUtils {

    public static ContactCompat AIOParam2Contact(Object AIOParam) {
        try {
            Object AIOSession = XField.obj(AIOParam).type(Initiator.loadClass("com.tencent.aio.data.AIOSession")).get();
            Object AIOContact = XField.obj(AIOSession).type(Initiator.loadClass("com.tencent.aio.data.AIOContact")).get();
            ContactCompat contact = new ContactCompat();
            contact.setPeerUid(getCurrentPeerIDByAIOContact(AIOContact));

            int chatType = getCurrentChatTypeByAIOContact(AIOContact);
            contact.setChatType(chatType);

            if (chatType == 4) {
                contact.setGuildId(getCurrentGuildIDByAIOContact(AIOContact));
            }
            return contact;
        } catch (Exception e) {
            XLog.e("SessionUtils.AIOParam2Contact", e);
            return null;
        }
    }

    public static Serializable AIOParam2ContactRaw(Object AIOParam) {
        return AIOParam2Contact(AIOParam).toKernelObject();
    }

    public static String getCurrentPeerIDByAIOContact(Object AIOContact) throws Exception {
        return XField.obj(AIOContact).name(requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA) ? "e" : "f").type(String.class).get();
    }

    public static int getCurrentChatTypeByAIOContact(Object AIOContact) throws Exception {
        return XField.obj(AIOContact).name(requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA) ? "d" : "e").type(int.class).get();
    }

    public static String getCurrentGuildIDByAIOContact(Object AIOContact) throws Exception {
        return XField.obj(AIOContact).name(requireMinTimVersion(TIMVersion.TIM_4_0_95_BETA) ? "f" : "g").type(String.class).get();
    }
}
