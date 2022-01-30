/*
 * QAuxiliary - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2022 qwq233@qwq2333.top
 * https://github.com/cinit/QAuxiliary
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
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
package io.github.qauxv.bridge;

import static io.github.qauxv.util.Initiator.load;

import io.github.qauxv.util.Log;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;


public class FriendChunk implements Serializable, Cloneable {

    private static final int maxLength = 14;
    private static Field[] from;
    private static Field[] to;
    private static int validLength = -1;
    private static Field f_uin, f_remark, f_nick, f_cSpecialFlag, f_status, f_stSelfInfo;
    public byte cHasOtherRespFlag;
    public byte cRespType;
    public short errorCode;
    public short friend_count;//len
    public short getfriendCount;
    public byte ifReflush;
    public short online_friend_count;
    public int reqtype;
    public int result;
    public long serverTime;
    public short sqqOnLine_count;
    public short startIndex;
    public short totoal_friend_count;
    public long uin;
    public long[] arrUin;
    public String[] arrRemark;
    public String[] arrNick;
    public byte[] arrcSpecialFlag;
    public byte[] arrStatus;

    public FriendChunk(Object resp) {
        fromGetFriendListResp(resp);
    }

    public FriendChunk() {
    }

    public static synchronized void initOnce() {
        if (validLength > 0) {
            return;
        }
        from = new Field[maxLength];
        to = new Field[maxLength];
        Class clz_gfr = load("friendlist/GetFriendListResp");
        validLength = 0;
        Field[] mine = FriendChunk.class.getDeclaredFields();

        Field f;
        for (Field field : mine) {
            try {
                if (!field.getName().startsWith("arr") && !Modifier
                    .isStatic(field.getModifiers())) {
                    f = clz_gfr.getField(field.getName());
                    f.setAccessible(true);
                    field.setAccessible(true);
                    from[validLength] = f;
                    to[validLength++] = field;
                }
            } catch (Throwable e) {
            }
        }
        try {
            f_stSelfInfo = clz_gfr.getField("vecFriendInfo");
            f_stSelfInfo.setAccessible(true);
        } catch (NoSuchFieldException e) {
        }
        Class clz_fi = load("friendlist/FriendInfo");
        try {
            f_uin = clz_fi.getField("friendUin");
            f_uin.setAccessible(true);
        } catch (NoSuchFieldException e) {
        }
        try {
            f_remark = clz_fi.getField("remark");
            f_remark.setAccessible(true);
        } catch (NoSuchFieldException e) {
        }
        try {
            f_nick = clz_fi.getField("nick");
            f_nick.setAccessible(true);
        } catch (NoSuchFieldException e) {
        }
        try {
            f_cSpecialFlag = clz_fi.getField("cSpecialFlag");
            f_cSpecialFlag.setAccessible(true);
        } catch (NoSuchFieldException e) {
        }
        try {
            f_status = clz_fi.getField("status");
            f_status.setAccessible(true);
        } catch (NoSuchFieldException e) {
        }

    }

    public void fromGetFriendListResp(Object resp) {
        if (validLength < 0) {
            initOnce();
        }
        try {
            for (int i = 0; i < validLength; i++) {
                to[i].set(this, from[i].get(resp));

            }
            int len = friend_count;
            arrStatus = new byte[len];
            arrUin = new long[len];
            arrRemark = new String[len];
            arrNick = new String[len];
            arrcSpecialFlag = new byte[len];
            ArrayList fs = (ArrayList) f_stSelfInfo.get(resp);
            for (int i = 0; i < len; i++) {
                arrStatus[i] = (byte) f_status.get(fs.get(i));
                arrUin[i] = (long) f_uin.get(fs.get(i));
                arrRemark[i] = (String) f_remark.get(fs.get(i));
                arrNick[i] = (String) f_nick.get(fs.get(i));
                arrcSpecialFlag[i] = (byte) f_cSpecialFlag.get(fs.get(i));
            }
        } catch (IllegalAccessException e) {
        } catch (ClassCastException e) {
            Log.e(e);
        }
        if (serverTime == 0) {
            serverTime = System.currentTimeMillis() / 1000L;
        }
    }

    @Override
    public int hashCode() {
        return (int) serverTime;
    }

    public int getUinIndex(long uin) {
        if (arrUin == null) {
            return -1;
        }
        for (int i = 0; i < arrUin.length; i++) {
            if (arrUin[i] == uin) {
                return i;
            }
        }
        return -1;
    }

}
