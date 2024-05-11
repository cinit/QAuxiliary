/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
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

package io.github.qauxv.util.data;

import androidx.annotation.Nullable;
import cc.ioctl.util.Reflex;
import io.github.qauxv.util.HostInfo;
import io.github.qauxv.util.QQVersion;

public class ContactDescriptor {

    public String uin;
    public int uinType;
    @Nullable
    public String nick;

    public String getId() {
        StringBuilder msg = new StringBuilder();
        if (uin.length() < 10) {
            for (int i = 0; i < 10 - uin.length(); i++) {
                msg.append("0");
            }
        }
        return msg + uin + uinType;
    }

    public static ContactDescriptor parseResultRec(Object a) {
        ContactDescriptor cd = new ContactDescriptor();
        if (HostInfo.requireMinQQVersion(QQVersion.QQ_8_9_88)) {
            cd.uin = Reflex.getInstanceObjectOrNull(a, "uin", String.class);
            cd.nick = Reflex.getInstanceObjectOrNull(a, "nick", String.class);
            cd.uinType = Reflex.getInstanceObjectOrNull(a, "uinType", int.class);
        } else {
            cd.uin = Reflex.getInstanceObjectOrNull(a, "a", String.class);
            cd.nick = Reflex.getInstanceObjectOrNull(a, "b", String.class);
            cd.uinType = Reflex.getInstanceObjectOrNull(a, "b", int.class);
        }
        return cd;
    }
}
