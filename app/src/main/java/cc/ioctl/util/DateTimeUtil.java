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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateTimeUtil {

    public static String getRelTimeStrSec(long timeSec) {
        return getRelTimeStrMs(timeSec * 1000);
    }

    public static String getRelTimeStrMs(long timeMs) {
        SimpleDateFormat format;
        long curr = System.currentTimeMillis();
        Date now = new Date(curr);
        Date t = new Date(timeMs);
        if (t.getYear() != now.getYear()) {
            format = new SimpleDateFormat("yyyy/MM/dd HH:mm");
            return format.format(t);
        }
        if (t.getMonth() == now.getMonth() && t.getDay() == now.getDay()) {
            format = new SimpleDateFormat("HH:mm:ss");
            return format.format(t);
        }
        if ((curr - timeMs) / 1000f / 3600f / 24f < 6.0f) {
            format = new SimpleDateFormat(" HH:mm");
            return "星期" + new String[]{"日", "一", "二", "三", "四", "五", "六"}[t.getDay()] + format
                .format(t);
        }
        format = new SimpleDateFormat("MM-dd HH:mm");
        return format.format(t);
    }

    public static String getIntervalDspMs(long ms1, long ms2) {
        Date t1 = new Date(Math.min(ms1, ms2));
        Date t2 = new Date(Math.max(ms1, ms2));
        Date tn = new Date();
        SimpleDateFormat format;
        String ret;
        switch (difTimeMs(t1, tn)) {
            case 4:
            case 3:
            case 2:
                format = new SimpleDateFormat("MM-dd HH:mm");
                break;
            case 1:
            case 0:
                format = new SimpleDateFormat("HH:mm");
                break;
            default:
                format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                break;
        }
        ret = format.format(t1);
        switch (difTimeMs(t1, t2)) {
            case 4:
            case 3:
            case 2:
                format = new SimpleDateFormat("MM-dd HH:mm");
                break;
            case 1:
            case 0:
                format = new SimpleDateFormat("HH:mm");
                break;
            default:
                format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                break;
        }
        ret = ret + " 至 " + format.format(t2);
        return ret;
    }

    /**
     * same: t0 d1 w2 m3 y4
     */
    private static int difTimeMs(Date t1, Date t2) {
        Calendar c1 = Calendar.getInstance();
        c1.setTime(t1);
        Calendar c2 = Calendar.getInstance();
        c2.setTime(t2);
        if (c1.get(Calendar.YEAR) != c2.get(Calendar.YEAR)) {
            return 5;
        }
        if (c1.get(Calendar.MONTH) != c2.get(Calendar.MONTH)) {
            return 4;
        }
        if (c1.get(Calendar.DATE) != c2.get(Calendar.DATE)) {
            return 3;
        }
        if (t1.equals(t2)) {
            return 0;
        }
        return 1;
    }

}
