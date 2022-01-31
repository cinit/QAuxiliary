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

package cc.ioctl.util;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utils from com.bug.zqq
 *
 * @author BUG
 */
public class BugUtils {

    /**
     * @param size in bytes
     * @return A human readable string for the size
     */
    @NonNull
    public static String getSizeString(long size) {
        if (size < 0) {
            return "-1";
        }
        if (size < 1024) {
            return size + "B";
        }
        LinkedHashMap<Long, String> map = new LinkedHashMap<>();
        map.put(1152921504606846976L, "EiB");
        map.put(1125899906842624L, "PiB");
        map.put(1099511627776L, "TiB");
        map.put(1073741824L, "GiB");
        map.put(1048576L, "MiB");
        map.put(1024L, "KiB");
        for (Map.Entry<Long, String> entry : map.entrySet()) {
            long longValue = (Long) entry.getKey();
            String str = (String) entry.getValue();
            if (size >= longValue) {
                @SuppressLint("DefaultLocale")
                String format = String.format("%.2f", ((double) size) / ((double) longValue));
                int indexOf = format.indexOf(".00");
                if (indexOf != -1) {
                    return format.substring(0, indexOf) + str;
                }
                return format + str;
            }
        }
        return "0B";
    }
}
