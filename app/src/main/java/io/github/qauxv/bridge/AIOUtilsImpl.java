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

import android.view.View;
import androidx.annotation.Nullable;
import io.github.qauxv.util.Initiator;
import io.github.qauxv.util.Log;
import java.lang.reflect.Field;

public class AIOUtilsImpl {

    private static Class<?> c_tx_ListView = null;
    private static Field f_BaseHolder_ChatMsg = null;

    @Nullable
    public static Object getBaseHolder(View v) {
        if (v == null) {
            return null;
        }
        if (c_tx_ListView == null) {
            c_tx_ListView = Initiator.load("com.tencent.widget.ListView");
        }
        if (v.getParent() == null || c_tx_ListView.isInstance(v.getParent())) {
            return v.getTag();
        }
        return getBaseHolder((View) v.getParent());
    }

    @Nullable
    public static Object getChatMessage(View v) {
        Object holder = getBaseHolder(v);
        if (holder == null) {
            return null;
        }
        if (f_BaseHolder_ChatMsg == null) {
            Class<?> c_BaseHolder = holder.getClass();
            while (c_BaseHolder.getSuperclass() != Object.class) {
                c_BaseHolder = c_BaseHolder.getSuperclass();
            }
            Field[] fs = c_BaseHolder.getDeclaredFields();
            for (Field f : fs) {
                if (f.getType() == Initiator._ChatMessage()) {
                    f_BaseHolder_ChatMsg = f;
                    break;
                }
            }
        }
        try {
            return f_BaseHolder_ChatMsg.get(holder);
        } catch (Exception e) {
            Log.e(e);
            //should not happen, it's public
            return null;
        }
    }
}
