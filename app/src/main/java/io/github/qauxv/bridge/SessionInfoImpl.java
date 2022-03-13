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
package io.github.qauxv.bridge;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import io.github.qauxv.util.Initiator;
import java.lang.reflect.Constructor;

/**
 * Utility class for creating SessionInfo instances.
 */
public class SessionInfoImpl {

    private SessionInfoImpl() {
        throw new UnsupportedOperationException("utility class");
    }

    /**
     * Create a SessionInfo reference for a contact friend.
     *
     * @param uin the uin of the friend
     * @return a SessionInfo instance
     */
    @NonNull
    public static Parcelable forFriend(@NonNull String uin) {
        if (TextUtils.isEmpty(uin)) {
            throw new IllegalArgumentException("uin is empty");
        }
        return createSessionInfo(uin, 0);
    }

    /**
     * Create a SessionInfo reference for a group.
     *
     * @param uin the uin of the group
     * @return a SessionInfo instance
     */
    @NonNull
    public static Parcelable forTroop(@NonNull String uin) {
        if (TextUtils.isEmpty(uin)) {
            throw new IllegalArgumentException("uin is empty");
        }
        return createSessionInfo(uin, 1);
    }

    /**
     * Create a SessionInfo reference.
     *
     * @param uin     the uin of the session
     * @param uinType the type of the uin, 0 for friend, 1 for group
     * @return a SessionInfo instance
     */
    @NonNull
    public static Parcelable createSessionInfo(@NonNull String uin, int uinType) {
        Parcel parcel = Parcel.obtain();
        parcel.writeInt(uinType);
        parcel.writeString(uin);
        parcel.writeString(null);//troopUin_b
        parcel.writeString(null);//uin_name_d
        parcel.writeString(null);//phoneNum_e
        parcel.writeInt(3999);//add_friend_source_id_d
        parcel.writeBundle(null);
        parcel.setDataPosition(0);
        Parcelable ret = null;
        try {
            Class<?> clSessionInfo = Initiator._SessionInfo();
            Constructor<?> c = clSessionInfo.getDeclaredConstructor(Parcel.class);
            c.setAccessible(true);
            ret = (Parcelable) c.newInstance(parcel);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            parcel.recycle();
        }
        return ret;
    }
}
