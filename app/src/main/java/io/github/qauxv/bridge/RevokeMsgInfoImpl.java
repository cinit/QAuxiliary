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

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;
import io.github.qauxv.util.Log;

public class RevokeMsgInfoImpl {

    public static final int OP_TYPE_TROOP_UNKNOWN = -1;
    public static final int OP_TYPE_TROOP_MEMBER = 0;
    public static final int OP_TYPE_TROOP_ADMIN = 1;
    public static final int OP_TYPE_TROOP_OWNER = 2;

    public int istroop;
    public long shmsgseq;
    public String friendUin;
    public long msgUid;
    public String fromUin;
    public long time;
    public String sendUin;
    @Nullable
    public String authorUin = null;
    @Nullable
    public int opType = -1;

    public RevokeMsgInfoImpl(Parcelable o) {
        if (o == null) {
            throw new NullPointerException("RevokeMsgInfo == null");
        }
        Parcel p = Parcel.obtain();
        try {
            o.writeToParcel(p, 0);
            p.setDataPosition(0);
            istroop = p.readInt();
            shmsgseq = p.readLong();
            friendUin = p.readString();
            sendUin = p.readString();
            msgUid = p.readLong();
            time = p.readLong();
            if (p.dataAvail() > 0) {
                authorUin = p.readString();
            }
            if (p.dataAvail() > 0) {
                opType = p.readInt();
            }
        } catch (Exception e) {
            Log.e(e);
        }
        p.recycle();
        String summery = o.toString();
        int keyIndex = summery.indexOf("fromuin");
        if (keyIndex == -1) {
            Log.i("RevokeMsgInfoImpl/E indexOf('fromuin') == -1, leave fromUin null");
            return;
        }
        int valueStart = summery.indexOf('=', keyIndex) + 1;
        if (summery.charAt(valueStart) == ' ') {
            valueStart++;
        }
        int end1 = summery.indexOf(',', valueStart);
        int end2 = summery.indexOf(' ', valueStart);
        if (end1 == -1) {
            end1 = summery.length() - 1;
        }
        if (end2 == -1) {
            end2 = summery.length() - 1;
        }
        int end = Math.min(end1, end2);
        String str = summery.substring(valueStart, end);
        if (str.equals("null")) {
            fromUin = null;
        } else {
            fromUin = str;
        }
    }

    @Override
    public String toString() {
        return "RevokeMsgInfoImpl{" +
            "istroop=" + istroop +
            ", shmsgseq=" + shmsgseq +
            ", friendUin='" + friendUin + '\'' +
            ", msgUid=" + msgUid +
            ", fromUin='" + fromUin + '\'' +
            ", time=" + time +
            ", sendUin='" + sendUin + '\'' +
            ", authorUin='" + authorUin + '\'' +
            ", opType=" + opType +
            '}';
    }
}
